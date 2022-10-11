package dev.alexmaycon.bucketservice.config.model;

import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Validated
public class OciConfig {

    private String profile;

    @NotEmpty(message = "'service.oci.bucket' must be informed.")
    private String bucket;

    public String getProfile() {
        return profile == null ? "DEFAULT" : profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    @Override
    public String toString() {
        return "OciConfig{" +
                "profile='" + profile + '\'' +
                ", bucket='" + bucket + '\'' +
                '}';
    }
}
