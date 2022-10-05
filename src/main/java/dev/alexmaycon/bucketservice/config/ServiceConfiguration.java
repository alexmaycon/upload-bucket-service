package dev.alexmaycon.bucketservice.config;

import dev.alexmaycon.bucketservice.config.model.EmailNotification;
import dev.alexmaycon.bucketservice.config.model.Service;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Configuration
@PropertySource(value = "application.properties", ignoreResourceNotFound = true)
@ConfigurationProperties()
@Validated
public class ServiceConfiguration {

    private Service service;

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    @Override
    public String toString() {
        return "ServiceConfiguration{" +
                "service=" + service +
                '}';
    }

}
