package dev.alexmaycon.bucketservice.batch;

import dev.alexmaycon.bucketservice.batch.config.JobConfig;
import dev.alexmaycon.bucketservice.batch.listener.JobUploadFileListener;
import dev.alexmaycon.bucketservice.batch.runnable.RunnableJob;
import dev.alexmaycon.bucketservice.batch.task.FileUploadTask;
import dev.alexmaycon.bucketservice.config.ServiceConfiguration;
import dev.alexmaycon.bucketservice.config.model.FolderConfig;
import dev.alexmaycon.bucketservice.config.model.OciConfig;
import dev.alexmaycon.bucketservice.oci.ObjectStorageComponent;
import dev.alexmaycon.bucketservice.oci.OciAuthComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Configuration
@EnableBatchProcessing
public class BatchUploadFiles {

    private final static Logger logger = LoggerFactory.getLogger(BatchUploadFiles.class);
    public static final String JOB_NAME = "DEFAULT_CRON_JOB";

    Map<String, ScheduledFuture<?>> jobsMap = new HashMap<>();

    private final JobBuilderFactory jobBuilderFactory;

    private final StepBuilderFactory stepBuilderFactory;

    private final JobUploadFileListener listener;

    private final ServiceConfiguration configuration;

    private final TaskScheduler taskScheduler;

    private JobConfig jobConfig;

    private final JobLauncher jobLauncher;

    private final OciAuthComponent ociAuthComponent;

    private final JobExplorer jobExplorer;

    public BatchUploadFiles(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, JobUploadFileListener listener, ServiceConfiguration configuration, TaskScheduler taskScheduler, JobLauncher jobLauncher, JobConfig jobConfig,  OciAuthComponent ociAuthComponent, JobExplorer jobExplorer) {

        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.listener = listener;
        this.configuration = configuration;
        this.taskScheduler = taskScheduler;
        this.jobLauncher = jobLauncher;
        this.jobConfig = jobConfig;
        this.ociAuthComponent = ociAuthComponent;
        this.jobExplorer = jobExplorer;

        Assert.notNull(this.configuration.getService(), "application.properties file was not successfully loaded.");
        Assert.notNull(this.ociAuthComponent, ".oci file was not successfully loaded.");
    }

    public Step createStep(String jobName, String name, FolderConfig folderConfig) throws IOException {
        StepBuilder stepBuilder = this.stepBuilderFactory.get(name);

        final OciConfig oci = folderConfig.getOci() == null ? configuration.getService().getOci() : folderConfig.getOci();

        FileUploadTask fileUploadTask = new FileUploadTask();
        fileUploadTask.setDirectory(new FileUrlResource(folderConfig.getDirectory()));

        fileUploadTask.setBucketName(oci.getBucket());
        fileUploadTask.setProfile(oci.getProfile());
        fileUploadTask.setCompartmentOcid(oci.getCompartmentOcid());
        fileUploadTask.setCreateBucketIfNotExists(oci.isCreateBucketIfNotExists());
        fileUploadTask.setOciAuthComponent(ociAuthComponent);
        fileUploadTask.setObjectStorageComponent(new ObjectStorageComponent(configuration));
        fileUploadTask.setOverrideFile(folderConfig.isOverwriteExistingFile());
        fileUploadTask.setBucketDir(folderConfig.getMapToBucketDir());

        final String cron = (folderConfig.getCron() == null ? configuration.getService().getCron() :folderConfig.getCron());

        jobConfig.put(jobName, "DIRECTORY="+folderConfig.getDirectory()+";CRON="+cron +";BUCKET="+oci.getBucket());

        return stepBuilder.tasklet(fileUploadTask).throttleLimit(1).startLimit(1).build();
    }


    public Flow splitFlow(List<Flow> flows) {
        return new FlowBuilder<SimpleFlow>("split_flow").split(taskExecutor()).add(flows.toArray(new Flow[0])).build();
    }

    private boolean validateFolderConfig(FolderConfig folderConfig) {
        Assert.notNull(folderConfig, "FolderConfig must not be null.");

        boolean validDir = folderConfig.isValidDirectory();

        if (!validDir) {
            logger.warn("Parameter '" + folderConfig + "' configured in application properties is not a valid directory.");
        }

        if (!folderConfig.isEnabled()) {
            logger.warn("Directory '" + folderConfig + "' was ignored because it is disabled.");
        }

        return folderConfig.isEnabled() && validDir;
    }

    private boolean validateDefaultFolderConfig(FolderConfig folderConfig) {
        return validateFolderConfig(folderConfig) && folderConfig.getCron() == null;
    }

    private boolean validateCustomCronFolderConfig(FolderConfig folderConfig) {
        return validateFolderConfig(folderConfig) && folderConfig.getCron() != null;
    }

    public List<Flow> flows(String jobName) {
        final List<FolderConfig> listCleared = new ArrayList<>(
                new LinkedHashSet<>(new ArrayList<>(configuration.getService().getFolders())));

        Assert.isTrue(!(listCleared.stream()
                        .filter(this::validateDefaultFolderConfig).count() == 0 && listCleared.stream()
                        .filter(this::validateCustomCronFolderConfig).count() > 0),
                "At least one directory must use the default cron configuration 'service.cron' and its property 'service.folders[*].cron' must not be informed.");

        List<Step> steps = listCleared.stream()
                .filter(this::validateDefaultFolderConfig)
                .map(dir -> {
                    File file = new File(dir.getDirectory());
                    try {
                        logger.info("Creating step to folder config \"{}\".", file.getPath());
                        return createStep(jobName, "step_".concat(file.getName()).concat("_" + UUID.randomUUID()), dir);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        Assert.notEmpty(steps, "No steps were created for the job " + getDefaultJobName());

        return steps.stream()
                .map(step -> new FlowBuilder<SimpleFlow>("flow_".concat(step.getName()))
                .start(step)
                .build())
                .collect(Collectors.toList());
    }


    @Bean
    protected Job job() {
        logger.info("Creating job {} with default cron {}.", getDefaultJobName(), configuration.getService().getCron());
        List<Flow> flows = flows(getDefaultJobName());
        Flow flow = splitFlow(flows);
        return this.jobBuilderFactory.get(getDefaultJobName()).listener(listener).start(flow).build().build();
    }

    @Bean
    protected void createSeperatedCronJobs() {
        final List<FolderConfig> listCleared = new ArrayList<>(
                new LinkedHashSet<>(new ArrayList<>(configuration.getService().getFolders())));

        listCleared.stream()
                .filter(this::validateCustomCronFolderConfig)
                .forEach(dir -> {
                    File file = new File(dir.getDirectory());
                    try {
                        FileUrlResource fileUrlResource = new FileUrlResource(dir.getDirectory());
                        String dirName = fileUrlResource.getFile().getName().toUpperCase().concat("_" + UUID.randomUUID().toString().toUpperCase());
                        String jobName = "JOB_CUSTOM_CRON_DIR_".concat(dirName);
                        Step step = createStep(jobName, "STEP_CUSTOM_CRON_".concat(file.getName()), dir);

                        Job job = this.jobBuilderFactory.get(jobName).listener(listener).start(step).build();
                        RunnableJob runnableJob = new RunnableJob(job, this.jobLauncher, jobExplorer);

                        logger.info("Created custom cron job {} to folder config \"{}\" with cron {}.", runnableJob.getName(), dir.getDirectory(), dir.getCron());

                        TimeZone timezone = TimeZone.getTimeZone(TimeZone.getDefault().getID());

                        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(runnableJob, new CronTrigger(dir.getCron(), timezone));

                        if (jobsMap.containsKey(runnableJob.getName())) {
                            removeScheduledTask(runnableJob.getName());
                        }

                        jobsMap.put(runnableJob.getName(), scheduledTask);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public void removeScheduledTask(String jobName) {
        ScheduledFuture<?> scheduledTask = jobsMap.get(jobName);
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
            jobsMap.put(jobName, null);
        }
    }

    @Bean
    public TaskExecutor taskExecutor() {
        //taskExecutor.setConcurrencyLimit(2);
        return new SimpleAsyncTaskExecutor("batch_upload_files");
    }

    private String getDefaultJobName() {
        return configuration.getService().getNameDefaultJob() == null ? JOB_NAME : configuration.getService().getNameDefaultJob().toUpperCase();
    }

}
