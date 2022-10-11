package dev.alexmaycon.bucketservice.config.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.*;
import java.util.List;

@Validated
public class Service {

    @NotNull
    @NotEmpty
    private List<FolderConfig> folders;

    @Min(1)
    @Max(10)
    private Integer attemptsFailure;

    @NotNull
    private OciConfig oci;

    @NotNull
    @NotEmpty
    @NotBlank
    private String cron;

    public List<FolderConfig> getFolders() {
        return folders;
    }

    public void setFolders(List<FolderConfig> folders) {
        this.folders = folders;
    }

    public Integer getAttemptsFailure() {
        return attemptsFailure == null ? 1 : attemptsFailure;
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
        return cron == null ? "0/10 * * * * ?" : cron;
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
