package dev.alexmaycon.bucketservice.batch.scheduler;

import dev.alexmaycon.bucketservice.batch.BatchUploadFiles;
import dev.alexmaycon.bucketservice.config.ServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

@Component
public class UploadFileScheduler {

    private final static Logger logger = LoggerFactory.getLogger(UploadFileScheduler.class);

    private final JobLauncher jobLauncher;

    private final Job job;

    private final ServiceConfiguration serviceConfiguration;

    final
    JobExplorer jobExplorer;

    public UploadFileScheduler(JobLauncher jobLauncher, Job job, ServiceConfiguration serviceConfiguration, JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.job = job;
        this.serviceConfiguration = serviceConfiguration;
        this.jobExplorer = jobExplorer;
    }

    @Scheduled(cron = "#{serviceConfiguration.service.cron}")
    public void run() throws Exception {
        Set<JobExecution> jobs = jobExplorer.findRunningJobExecutions(BatchUploadFiles.JOB_NAME);

        boolean jobRunning = jobs.stream().filter(jobExecution -> jobExecution.isRunning() && jobExecution.getStatus() == BatchStatus.STARTED).count() > 0;

        if(jobRunning) {
            logger.warn("Job {} already running... ignoring...", job.getName());
            return;
        }

        logger.info("Job {} started as {}.", job.getName(), new Date());
        JobParameters jobParameters = new JobParametersBuilder().addLong("JobId", System.currentTimeMillis()).toJobParameters();
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
        logger.info("Job {} finished as {} with status {}.", job.getName(), new Date(), jobExecution.getStatus());
        CronExpression cronTrigger = CronExpression.parse(serviceConfiguration.getService().getCron());
        logger.info("Next job {} execution {}.", job.getName(), cronTrigger.next(LocalDateTime.now()));
    }

}
