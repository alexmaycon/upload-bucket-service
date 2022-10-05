package dev.alexmaycon.bucketservice.batch.runnable;

import dev.alexmaycon.bucketservice.batch.task.FileUploadTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RunnableJob extends Thread {

    private static Logger logger = LoggerFactory
            .getLogger(RunnableJob.class);

    private Job job;

    private JobParameters jobParameters;

    private JobLauncher jobLauncher;

    public RunnableJob(Job job, JobLauncher jobLauncher){
        this.job = job;
        this.jobLauncher = jobLauncher;

        this.setName(job.getName());
    }

    @Override
    public void run() {
        try {
            this.jobParameters = new JobParametersBuilder().addLong("JobId", System.currentTimeMillis()).toJobParameters();
            jobLauncher.run(job, jobParameters);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex.getCause());
        }
    }
}
