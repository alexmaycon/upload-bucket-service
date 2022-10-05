package dev.alexmaycon.bucketservice.config.model;

import jakarta.validation.constraints.NotNull;

import java.io.File;
import java.util.Objects;

public class FolderConfig {

    private boolean enabled=true;
    @NotNull
    private String directory;

    private String cron;

    private boolean overwriteExistingFile = false;

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

    @Override
    public String toString() {
        return "FolderConfig{" +
                "enabled=" + enabled +
                ", directory='" + directory + '\'' +
                ", cron='" + cron + '\'' +
                ", overwriteExistingFile=" + overwriteExistingFile +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FolderConfig that = (FolderConfig) o;
        return directory.equals(that.directory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directory);
    }

    public boolean isValidDirectory() {
        final File dir = new File(this.directory);
        return dir.isDirectory() && dir.exists();
    }
}
