package dev.alexmaycon.bucketservice.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class JobConfig {

    private HashMap<String, String> directoriesPerJobConfig;

    public JobConfig(){
        directoriesPerJobConfig = new HashMap<>();
    }

    @Bean
    public HashMap<String, String> getDirectoriesPerJobConfig(){
        return this.directoriesPerJobConfig;
    }

    public void put(String key, String value){
        if (directoriesPerJobConfig.containsKey(key)) {
            directoriesPerJobConfig.merge(key, value, (oldValue, newValue) -> oldValue+"¢"+newValue);
        } else {
            directoriesPerJobConfig.put(key, value);
        }
    }

}
