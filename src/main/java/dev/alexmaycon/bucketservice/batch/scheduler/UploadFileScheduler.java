package dev.alexmaycon.bucketservice.batch.scheduler;

import dev.alexmaycon.bucketservice.config.ServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

@Component
public class UploadFileScheduler {

    private static Logger logger = LoggerFactory.getLogger(UploadFileScheduler.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job job;

    @Autowired
    private ServiceConfiguration serviceConfiguration;

    @Scheduled(cron = "#{serviceConfiguration.service.cron}")
    public void run() throws Exception {
        logger.info("Job {} started as {}.", job.getName(), new Date());
        JobParameters jobParameters = new JobParametersBuilder().addLong("JobId", System.currentTimeMillis()).toJobParameters();
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
        logger.info("Job {} finished as {} with status {}.", job.getName(), new Date(), jobExecution.getStatus());
        CronExpression cronTrigger = CronExpression.parse(serviceConfiguration.getService().getCron());
        logger.info("Next job {} execution {}.", job.getName(), cronTrigger.next(LocalDateTime.now()));
    }

}
