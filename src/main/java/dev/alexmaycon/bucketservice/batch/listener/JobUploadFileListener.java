package dev.alexmaycon.bucketservice.batch.listener;

import dev.alexmaycon.bucketservice.config.ServiceConfiguration;
import dev.alexmaycon.bucketservice.hook.HookClientRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class JobUploadFileListener extends JobExecutionListenerSupport {

    private static final Logger logger = LoggerFactory.getLogger(JobUploadFileListener.class);

    private final HookClientRequest hookClientRequest;

    @Autowired
    public JobUploadFileListener(HookClientRequest hookClientRequest) {
        this.hookClientRequest = hookClientRequest;
    }

    public void sendHook(JobExecution jobExecution){
        hookClientRequest.send(jobExecution);
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Starting job execution {}.'", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        sendHook(jobExecution);

        if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
            logger.info("Finishing job execution {} with SUCCESS status.'", jobExecution.getJobInstance().getJobName());
        } else if(jobExecution.getStatus() == BatchStatus.FAILED) {
            logger.info("Finishing job execution {} with FAIL status'", jobExecution.getJobInstance().getJobName());
        }


    }

}
