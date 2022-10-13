package dev.alexmaycon.bucketservice.batch.runnable;

import dev.alexmaycon.bucketservice.batch.BatchUploadFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;

import java.util.Set;


public class RunnableJob extends Thread {

    private final static Logger logger = LoggerFactory.getLogger(RunnableJob.class);

    private Job job;

    private JobParameters jobParameters;

    private JobLauncher jobLauncher;

    private JobExplorer jobExplorer;

    public RunnableJob(Job job, JobLauncher jobLauncher, JobExplorer jobExplorer){
        this.job = job;
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;

        this.setName(job.getName());
    }

    @Override
    public void run() {
        try {
            Set<JobExecution> jobs = jobExplorer.findRunningJobExecutions(job.getName());

            boolean jobRunning = jobs.stream().filter(jobExecution -> jobExecution.isRunning() && jobExecution.getStatus() == BatchStatus.STARTED).count() > 0;

            if(jobRunning) {
                logger.warn("[RunnableJob] Job {} already running... ignoring...", job.getName());
                return;
            }

            this.jobParameters = new JobParametersBuilder().addLong("JobId", System.currentTimeMillis()).toJobParameters();
            jobLauncher.run(job, jobParameters);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex.getCause());
        }
    }
}
