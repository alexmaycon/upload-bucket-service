package dev.alexmaycon.bucketservice.batch;

import dev.alexmaycon.bucketservice.batch.listener.JobUploadFileListener;
import dev.alexmaycon.bucketservice.batch.runnable.RunnableJob;
import dev.alexmaycon.bucketservice.batch.task.FileUploadTask;
import dev.alexmaycon.bucketservice.config.ServiceConfiguration;
import dev.alexmaycon.bucketservice.config.model.FolderConfig;
import dev.alexmaycon.bucketservice.oci.ObjectStorageComponent;
import dev.alexmaycon.bucketservice.oci.OciAuthComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static Logger logger = LoggerFactory.getLogger(BatchUploadFiles.class);
    private static final String JOB_NAME = "DEFAULT_CRON_JOB";

    Map<String, ScheduledFuture<?>> jobsMap = new HashMap<>();

    private final JobBuilderFactory jobBuilderFactory;

    private final StepBuilderFactory stepBuilderFactory;

    private final JobUploadFileListener listener;

    private final ServiceConfiguration configuration;

    private final OciAuthComponent ociAuthComponent;

    private final TaskScheduler taskScheduler;

    final
    JobLauncher jobLauncher;

    @Autowired
    public BatchUploadFiles(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, JobUploadFileListener listener, ServiceConfiguration configuration, OciAuthComponent ociAuthComponent, TaskScheduler taskScheduler, JobLauncher jobLauncher) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.listener = listener;
        this.configuration = configuration;
        this.ociAuthComponent = ociAuthComponent;
        this.taskScheduler = taskScheduler;
        this.jobLauncher = jobLauncher;

        Assert.notNull(this.configuration.getService(), "application.properties file was not successfully loaded.");
        Assert.notNull(this.ociAuthComponent, ".oci file was not successfully loaded.");
    }

    public Step createStep(String name, FolderConfig folderConfig) throws IOException {
        StepBuilder stepBuilder = this.stepBuilderFactory.get(name);

        FileUploadTask fileUploadTask = new FileUploadTask();
        fileUploadTask.setDirectory(new FileUrlResource(folderConfig.getDirectory()));
        fileUploadTask.setBucketName(configuration.getService().getOci().getBucket());
        fileUploadTask.setProfile(configuration.getService().getOci().getProfile());
        fileUploadTask.setOciAuthComponent(ociAuthComponent);
        fileUploadTask.setObjectStorageComponent(new ObjectStorageComponent(configuration));
        fileUploadTask.setOverrideFile(folderConfig.isOverwriteExistingFile());
        fileUploadTask.setBucketDir(folderConfig.getMapToBucketDir());

        return stepBuilder.tasklet(fileUploadTask).build();
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

    public List<Flow> flows() throws Exception {
        final List<FolderConfig> listCleared = new ArrayList<>(
                new LinkedHashSet<>(new ArrayList<>(configuration.getService().getFolders())));

        List<Step> steps = listCleared.stream()
                .filter(folderConfig -> validateDefaultFolderConfig(folderConfig))
                .map(dir -> {
                    File file = new File(dir.getDirectory());
                    try {
                        logger.info("Creating step to folder config \"{}\".", file.getPath());
                        return createStep("step_".concat(file.getName()).concat("_" + UUID.randomUUID().toString()), dir);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        Assert.notEmpty(steps, "No steps were created for the job " + JOB_NAME);

        return steps.stream().map(step -> {
            return new FlowBuilder<SimpleFlow>("flow_".concat(step.getName()))
                    .start(step).build();
        }).collect(Collectors.toList());
    }


    @Bean
    public Job job() throws Exception {
        logger.info("Creating job {} with default cron {}.", JOB_NAME, configuration.getService().getCron());
        return this.jobBuilderFactory.get(JOB_NAME).listener(listener).start(splitFlow(flows())).end().build();
    }

    @Bean
    public void createSeperatedCronJobs() throws Exception {
        final List<FolderConfig> listCleared = new ArrayList<>(
                new LinkedHashSet<>(new ArrayList<>(configuration.getService().getFolders())));

        listCleared.stream()
                .filter(folderConfig -> validateCustomCronFolderConfig(folderConfig))
                .forEach(dir -> {
                    File file = new File(dir.getDirectory());
                    try {
                        Step step = createStep("STEP_CUSTOM_CRON_".concat(file.getName()), dir);
                        FileUrlResource fileUrlResource = new FileUrlResource(dir.getDirectory());
                        String dirName = fileUrlResource.getFile().getName().toUpperCase().concat("_" + UUID.randomUUID().toString().toUpperCase());

                        RunnableJob runnableJob = new RunnableJob(this.jobBuilderFactory.get("JOB_CUSTOM_CRON_DIR_".concat(dirName)).listener(listener).start(step).build(),
                                this.jobLauncher);

                        logger.info("Created custom cron job {} to folder config \"{}\" with cron {}.", runnableJob.getName(), dir.getDirectory(), dir.getCron());

                        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(runnableJob, new CronTrigger(dir.getCron(), TimeZone.getTimeZone(TimeZone.getDefault().getID())));

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
        return new SimpleAsyncTaskExecutor("batch_upload_files");
    }


}
