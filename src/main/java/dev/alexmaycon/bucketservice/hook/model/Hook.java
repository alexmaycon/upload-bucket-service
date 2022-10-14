package dev.alexmaycon.bucketservice.hook.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.batch.core.JobExecution;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

@XmlRootElement
public class Hook implements Serializable {

    private String jobName;
    private String jobStatus;
    private String details;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ", timezone = "GMT")
    private Date createdTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ", timezone = "GMT")
    private Date endTime;
    private List<Exception> exceptions;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<Exception> exceptions) {
        this.exceptions = exceptions;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @JsonIgnore
    public static Hook parseJobExecution(JobExecution jobExecution, String details) {
        Hook hook = new Hook();
        hook.setJobName(jobExecution.getJobInstance().getJobName());
        hook.setJobStatus(jobExecution.getStatus().toString());
        hook.setDetails(details);
        hook.setCreatedTime(jobExecution.getCreateTime());
        hook.setEndTime(jobExecution.getEndTime());
        hook.setExceptions(jobExecution.getAllFailureExceptions().stream().map(throwable -> new Exception(throwable.getMessage())).collect(Collectors.toList()));
        return hook;
    }
}
