package dev.alexmaycon.bucketservice.config.model;

public class OciConfig {

    private String profile;
    private String bucket;

    public String getProfile() {
        return profile;
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
