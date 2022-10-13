package dev.alexmaycon.bucketservice.hook;

import dev.alexmaycon.bucketservice.batch.listener.JobUploadFileListener;
import dev.alexmaycon.bucketservice.config.ServiceConfiguration;
import dev.alexmaycon.bucketservice.hook.model.Hook;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

@Component
public class HookClientRequest {

    private static final Logger logger = LoggerFactory.getLogger(HookClientRequest.class);
    private final ServiceConfiguration serviceConfiguration;

    public HookClientRequest(ServiceConfiguration serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    public void send(JobExecution jobExecution) {
        try {
            final String hook = serviceConfiguration.getService().getHook();

            if (hook == null)
                return;

            final String contentType = serviceConfiguration.getService().getHookContentType();

            ClientConfig config = new ClientConfig();

            Client client = ClientBuilder.newClient(config);

            WebTarget target = client.target(hook);

            Response response = target
                    .request()
                    .accept(contentType)
                    .post(Entity.entity(Hook.parseJobExecution(jobExecution), contentType), Response.class);

            if (response.getStatus() == 200) {
                logger.info("Job {} hook notification as sent successfully.", jobExecution.getJobInstance().getJobName());
            } else {
                logger.warn("Error {} on send job {} hook notification: Response: {}", response.getStatus(), jobExecution.getJobInstance().getJobName(), response.readEntity(String.class));
            }
        }   catch (Exception e) {
            logger.error("Error on send job {} hook notification: Error: {}", jobExecution.getJobInstance().getJobName(), e.getMessage());
        }

    }
}
