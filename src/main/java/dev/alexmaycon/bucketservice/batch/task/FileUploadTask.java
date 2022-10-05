package dev.alexmaycon.bucketservice.batch.task;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.responses.HeadObjectResponse;
import dev.alexmaycon.bucketservice.oci.ObjectStorageComponent;
import dev.alexmaycon.bucketservice.oci.OciAuthComponent;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;

public class FileUploadTask implements Tasklet, InitializingBean {

    private static Logger logger = LoggerFactory
            .getLogger(FileUploadTask.class);

    private Resource directory;
    private OciAuthComponent ociAuthComponent;
    private ObjectStorageComponent objectStorageComponent;

    private String bucketName;
    private String profile;

    private boolean overrideFile = false;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        logger.info("Starting task for file upload...");

        ObjectStorage objectStorage = objectStorageComponent.getObjectStorage(
                ociAuthComponent.getConfigFile(profile),
                ociAuthComponent.getAuthenticationDetailsProvider(profile));

        final String namespace = objectStorageComponent.getNamespace(objectStorage);

        if (directory == null || !directory.exists()) {
            logger.warn("No valid directory has been reported.");
        }

        File dir = null;
        try {
            dir = directory.getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("Checking files in dir '" + dir.getPath() + "'...");

        if (dir.isDirectory()) {
            Arrays.asList(dir.listFiles()).forEach(file -> {
                logger.info("Started processing file {}.", file.getName());
                HeadObjectResponse getObjectResponse = null;
                boolean fileExists = false;
                try {
                    getObjectResponse = objectStorageComponent.getHeadObject(objectStorage, namespace, bucketName, file.getName());
                    fileExists = getObjectResponse.get__httpStatusCode__() == 200;
                } catch (BmcException e) {
                    if (e.getStatusCode() == 404) {
                        fileExists = false;
                    }
                }

                if (!overrideFile && fileExists) {
                    logger.info("File {} will be ignored as the configuration does not allow file overwriting.", file.getName());
                }

                if (overrideFile || !fileExists) {
                    if (overrideFile) {
                        logger.info("File {} will be overwritten as per the configuration.", file.getName());
                    }

                    InputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        logger.error("File not found '" + file.getPath() + "'.", e.getCause());
                    }

                    if (inputStream != null) {

                        String md5 = null;
                        try {
                            InputStream temp = Files.newInputStream(Paths.get(file.getPath()));
                            md5 = DigestUtils.md5Hex(temp);
                            temp.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        if (fileExists && overrideFile && md5.equals(getObjectResponse.getOpcMeta().get("file-md5"))) {
                            logger.info("File {} was ignored because was not modified.", file.getName());
                        }

                        if (!fileExists || (overrideFile && !md5.equals(getObjectResponse.getOpcMeta().get("file-md5")))) {
                            final boolean res = objectStorageComponent.uploadFile(objectStorage, namespace, bucketName, file.getName(), file.length(), md5, inputStream);
                            if (!res) {
                                logger.warn("File {} was not uploaded successfully.", file.getName());
                            } else {
                                logger.info("File {} uploaded successfully.", file.getName());
                            }
                        }
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                logger.info("Finished processing file {} as {}.", file.getName(), new Date());

            });
        }
        objectStorage.close();
        logger.info("Finishing task for file upload...");
        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(directory, "Directories must be set");
        Assert.notNull(ociAuthComponent, "OciAuthComponent must be set");
        Assert.notNull(objectStorageComponent, "ObjectStorageComponent must be set");
        Assert.notNull(bucketName, "BucketName must be set");
        Assert.notNull(profile, "Profile must be set");
    }

    public Resource getDirectory() {
        return directory;
    }

    public void setDirectory(Resource directory) {
        this.directory = directory;
    }

    public OciAuthComponent getOciAuthComponent() {
        return ociAuthComponent;
    }

    public void setOciAuthComponent(OciAuthComponent ociAuthComponent) {
        this.ociAuthComponent = ociAuthComponent;
    }

    public ObjectStorageComponent getObjectStorageComponent() {
        return objectStorageComponent;
    }

    public void setObjectStorageComponent(ObjectStorageComponent objectStorageComponent) {
        this.objectStorageComponent = objectStorageComponent;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public boolean isOverrideFile() {
        return overrideFile;
    }

    public void setOverrideFile(boolean overrideFile) {
        this.overrideFile = overrideFile;
    }
}
