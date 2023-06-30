package dev.alexmaycon.bucketservice.config.model;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import jakarta.ws.rs.core.MediaType;
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

    private String nameDefaultJob;

    @Pattern(regexp = "^(?:^|[ \\t])((https?:\\/\\/)?(?:localhost|[\\w-]+(?:\\.[\\w-]+)+)(:\\d+)?(\\/\\S*)?)$", message = "'service.hook' must me a valid URL.")
    private String hook;

    private String hookContentType = MediaType.APPLICATION_JSON;

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

    public String getHook() {
        return hook;
    }

    public void setHook(String hook) {
        this.hook = hook;
    }

    public String getHookContentType() {
        return hookContentType;
    }

    public void setHookContentType(String hookContentType) {
        this.hookContentType = hookContentType;
    }

    public String getNameDefaultJob() {
        return this.nameDefaultJob;
    }

    public void setNameDefaultJob(String nameDefaultJob) {
        this.nameDefaultJob = nameDefaultJob;
    }

    @Override
    public String toString() {
        return "Service{" +
                "nameDefaultJob="+nameDefaultJob+
                ", folders=" + folders +
                ", attemptsFailure=" + attemptsFailure +
                ", oci=" + oci +
                ", cron='" + cron + '\'' +
                '}';
    }

}
