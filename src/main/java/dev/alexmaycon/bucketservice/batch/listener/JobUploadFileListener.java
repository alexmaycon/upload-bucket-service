package dev.alexmaycon.bucketservice.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

import dev.alexmaycon.bucketservice.batch.config.JobConfig;
import dev.alexmaycon.bucketservice.email.EmailNotification;
import dev.alexmaycon.bucketservice.hook.HookClientRequest;

@Component
public class JobUploadFileListener extends JobExecutionListenerSupport {

    private static final Logger logger = LoggerFactory.getLogger(JobUploadFileListener.class);

    private final JobConfig jobConfig;

    private final HookClientRequest hookClientRequest;
    private final EmailNotification emailNotification;

    public JobUploadFileListener(JobConfig jobConfig, HookClientRequest hookClientRequest, EmailNotification emailNotification) {
        this.jobConfig = jobConfig;
        this.hookClientRequest = hookClientRequest;
        this.emailNotification = emailNotification;
    }

    public void sendHook(JobExecution jobExecution){
        hookClientRequest.send(jobConfig.getDirectoriesPerJobConfig(), jobExecution);
    }
    
    public void sendEmail(JobExecution jobExecution) {
    	emailNotification.send(jobConfig.getDirectoriesPerJobConfig(), jobExecution);
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Starting job execution {}.'", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        sendHook(jobExecution);
        sendEmail(jobExecution);
        
        if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
            logger.info("Finishing job execution {} with SUCCESS status.'", jobExecution.getJobInstance().getJobName());
        } else if(jobExecution.getStatus() == BatchStatus.FAILED) {
            logger.info("Finishing job execution {} with FAIL status'", jobExecution.getJobInstance().getJobName());
        }


    }

}
