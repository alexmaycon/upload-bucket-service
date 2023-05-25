package dev.alexmaycon.bucketservice.config.model;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Validated
public class OciConfig {

    private String profile;

    @NotEmpty(message = "'service.oci.bucket' must be informed.")
    private String bucket;

    private String compartmentOcid;

    private boolean createBucketIfNotExists=false;

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

    public boolean isCreateBucketIfNotExists() {
        return createBucketIfNotExists;
    }

    public void setCreateBucketIfNotExists(boolean createBucketIfNotExists) {
        this.createBucketIfNotExists = createBucketIfNotExists;
    }

    public String getCompartmentOcid() {
        return compartmentOcid;
    }

    public void setCompartmentOcid(String compartmentOcid) {
        this.compartmentOcid = compartmentOcid;
    }

    @Override
    public String toString() {
        return "OciConfig{" +
                "profile='" + profile + '\'' +
                ", bucket='" + bucket + '\'' +
                ", compartmentId='" + compartmentOcid + '\'' +
                ", createBucketIfNotExists=" + createBucketIfNotExists +
                '}';
    }
}
