package dev.alexmaycon.bucketservice.config;

import dev.alexmaycon.bucketservice.config.model.Service;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Configuration
@PropertySource(value = {"classpath:application.properties","file:./application.properties"}, ignoreResourceNotFound = true)
@ConfigurationProperties
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
