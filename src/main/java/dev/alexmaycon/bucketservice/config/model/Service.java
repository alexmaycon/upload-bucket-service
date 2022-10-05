package dev.alexmaycon.bucketservice.config.model;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@Validated
public class Service {

    @NotEmpty
    private List<FolderConfig> folders;

    @Min(1)
    @Max(10)
    private Integer attemptsFailure = 1;

    @NotNull
    private OciConfig oci;

    private String cron = "0/10 * * * * ?";

    public List<FolderConfig> getFolders() {
        return folders;
    }

    public void setFolders(List<FolderConfig> folders) {
        this.folders = folders;
    }

    public Integer getAttemptsFailure() {
        return attemptsFailure;
    }

    public void setAttemptsFailure(Integer attemptsFailure) {
        this.attemptsFailure = attemptsFailure;
    }

    public OciConfig getOci() {
        return oci;
    }

    public void setOci(OciConfig oci) {
        this.oci = oci;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    @Override
    public String toString() {
        return "Service{" +
                "folders=" + folders +
                ", attemptsFailure=" + attemptsFailure +
                ", oci=" + oci +
                ", cron='" + cron + '\'' +
                '}';
    }

}
