package dev.alexmaycon.bucketservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableScheduling
public class BucketServiceApplication  {

    private static Logger LOG = LoggerFactory
            .getLogger(BucketServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BucketServiceApplication.class, args);
    }

}
