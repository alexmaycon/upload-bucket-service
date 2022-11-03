package dev.alexmaycon.bucketservice.config.model;

import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.File;
import java.util.Objects;

@Validated
public class FolderConfig {

    private boolean enabled=true;
    @NotNull(message = "'service.folders[*].directory' must be informed.")
    @NotEmpty(message = "'service.folders[*].directory' must not be empty.")
    private String directory;

    @Pattern(
            regexp ="(@(annually|yearly|monthly|weekly|daily|hourly|reboot))|(@every (\\d+(ns|us|µs|ms|s|m|h))+)|((((\\d+,)+\\d+|(\\d+(\\/|-)\\d+)|\\d+|\\*|\\?) ?){5,7})",
            message = "'service.folders[*].cron' must ve a valid cron expression.")
    private String cron;

    private boolean overwriteExistingFile = false;

    @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message="'service.folders[*].mapToBucketDir' must contain only letters, numbers, hyphen and underscore") //
    private String mapToBucketDir;

    private OciConfig oci;

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public boolean isOverwriteExistingFile() {
        return overwriteExistingFile;
    }

    public void setOverwriteExistingFile(boolean overwriteExistingFile) {
        this.overwriteExistingFile = overwriteExistingFile;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getMapToBucketDir() {
        return mapToBucketDir;
    }

    public void setMapToBucketDir(String mapToBucketDir) {
        this.mapToBucketDir = mapToBucketDir;
    }

    public OciConfig getOci() {
        return oci;
    }

    public void setOci(OciConfig oci) {
        this.oci = oci;
    }

    @Override
    public String toString() {
        return "FolderConfig{" +
                "enabled=" + enabled +
                ", directory='" + directory + '\'' +
                ", cron='" + cron + '\'' +
                ", overwriteExistingFile=" + overwriteExistingFile +
                ", mapToBucketDir='" + mapToBucketDir + '\'' +
                ", oci=" + oci +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FolderConfig that = (FolderConfig) o;
        return directory.equals(that.directory) && Objects.equals(cron, that.cron);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directory, cron);
    }

    public boolean isValidDirectory() {
        final File dir = new File(this.directory);
        return dir.isDirectory() && dir.exists();
    }
}
