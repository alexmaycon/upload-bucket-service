package dev.alexmaycon.bucketservice.batch.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;
import com.oracle.bmc.objectstorage.responses.HeadBucketResponse;
import com.oracle.bmc.objectstorage.responses.HeadObjectResponse;

import dev.alexmaycon.bucketservice.config.model.ZipConfig;
import dev.alexmaycon.bucketservice.oci.ObjectStorageComponent;
import dev.alexmaycon.bucketservice.oci.OciAuthComponent;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;

public class FileUploadTask implements Tasklet, InitializingBean {

    private final static Logger logger = LoggerFactory.getLogger(FileUploadTask.class);

    private Resource directory;
    private OciAuthComponent ociAuthComponent;
    private ObjectStorageComponent objectStorageComponent;
    private String bucketName;
    private String profile;
    private String bucketDir;
    private String compartmentOcid;
    private boolean overrideFile = false;
    private boolean createBucketIfNotExists = false;
    private boolean generatePreauthenticatedUrl = false;
    private ZipConfig zipConfig;
    private Integer deleteFileAfter;
    private String filenameExtensionFilter;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        logger.info("Starting task for file upload...");
        ConfigFileReader.ConfigFile configFile = ociAuthComponent.getConfigFile(profile);
        final String tenancyOcid = configFile.get("tenancy");

        ObjectStorage objectStorage = objectStorageComponent.getObjectStorage(
                configFile,
                ociAuthComponent.getAuthenticationDetailsProvider(profile));

        final String namespace = objectStorageComponent.getNamespace(objectStorage);

        if (directory == null || !directory.exists()) {
            logger.warn("No valid directory has been reported.");
        }
        
        if (filenameExtensionFilter != null) {
        	logger.info("Using filter  '{}' on files search.", filenameExtensionFilter);
    	}

        FilenameFilter filenameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
            	
            	if (filenameExtensionFilter == null || filenameExtensionFilter.isEmpty()) {
            		return true;
            	}
            	
            	List<String> extensions = Arrays.stream(filenameExtensionFilter.split(";"))
                        .map(str -> str.trim())
                        .collect(Collectors.toList());
            	
            	return extensions.stream().anyMatch(extension -> name.toLowerCase().endsWith(extension));
            }
        };
        
        File dir = null;
        try {
            dir = directory.getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("Starting file scan in directory '{}'.", dir.getPath());
        
        HashMap<String, String> pars = new HashMap<>();
        
        if (dir.isDirectory()) {

            if (bucketDir != null) {
                logger.info("'{}' will be mapped to '{}' bucket directory.", dir.getPath(), bucketDir);
            }

            // If 'compartmentOcid' is null then use 'tenancyOcid' (root compartment), otherwise use 'compartmentOcid'
            validateAndCreateBucket(objectStorageComponent, objectStorage, (compartmentOcid == null ? tenancyOcid : compartmentOcid), namespace, bucketName);

            Arrays.asList(dir.listFiles(filenameFilter)).forEach(file -> {
                logger.info("Started processing file {}.", file.getName());
                HeadObjectResponse getObjectResponse = null;
                boolean fileExists = false;
                final String fullFileName = ObjectStorageComponent.getFullObjectName(bucketDir, zipConfig.isEnabled() ? file.getName().concat(".zip") : file.getName());
                try {
                    getObjectResponse = objectStorageComponent.getHeadObject(objectStorage, namespace, bucketName, fullFileName);
                    fileExists = getObjectResponse.get__httpStatusCode__() == 200;
                } catch (BmcException e) {
                    if (e.getStatusCode() == 404) {
                        fileExists = false;
                    }
                }

                if (!overrideFile && fileExists) {
                    logger.info("File {} will be ignored as the configuration does not allow file overwriting.", fullFileName);
                }

                if (overrideFile || !fileExists) {
                    if (overrideFile) {
                        logger.info("File {} will be overwritten as per the configuration.", fullFileName);
                    }
                    
                    File finalFile = processFile(file, fullFileName);
                    if (finalFile == null) {
                    	throw new RuntimeException("Error on reading file.");
                    }
                    
                    InputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(finalFile);
                    } catch (FileNotFoundException e) {
                        logger.error("File not found '" + finalFile.getPath() + "'.", e.getCause());
                    }
                                        
                    if (inputStream != null) {
                        String md5 = null;
                        try {
                        	 InputStream temp = Files.newInputStream(Paths.get(finalFile.getPath()));
                             md5 = DigestUtils.md5Hex(temp);
                             temp.close();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        
                        final String ociFileHash = getObjectResponse != null && getObjectResponse.getOpcMeta() != null 
                        		? getObjectResponse.getOpcMeta().get("file-md5") : null;
                        final boolean isOciFileModified = ociFileHash != null && !md5.equals(ociFileHash);
                        
                        if (!fileExists || (overrideFile && isOciFileModified)) {
                            final boolean res = objectStorageComponent.uploadFile(objectStorage, namespace, bucketName, fullFileName, finalFile.length(), md5, inputStream);
                            if (!res) {
                                logger.warn("File {} was not uploaded successfully.", fullFileName);
                            } else {
                                logger.info("File {} uploaded successfully.", fullFileName);
                            }
                            
                            if (generatePreauthenticatedUrl) {
	                            CreatePreauthenticatedRequestResponse rr = objectStorageComponent.createPreauthenticatedRequest(objectStorage,namespace, bucketName, fullFileName);
	                            pars.put(fullFileName, rr.getPreauthenticatedRequest().getFullPath());
	                            logger.info("Generated pre authenticated URL to file {}.", fullFileName);
                            } else
                            	pars.put(fullFileName, null);
                        }
                        
                        try {
                        	inputStream.close();
                        	
                            if (zipConfig.isEnabled() && finalFile.delete()) {
                            	logger.info("Temp file {} deleted successfully.", finalFile.getName());
                            }
                            
	                        if (fileExists && overrideFile) {
	                            if (!isOciFileModified) {
	                            	logger.info("File {} was ignored because was not modified.", file.getName());
	                            }
	                        }
                        } catch (IOException e) {
                        	throw new RuntimeException(e);
						}
                    }
                }
                
                try {                    
                    if (fileExists) {
						if (deleteFileAfter != null 
								&& getDaysDifBetween(file.toPath()) >= deleteFileAfter 
								&& file.delete()) {
							logger.info("File {} was deleted because parameter \"deleteFileAfter\" was result true." , file.getName());
						}
                    }
                } catch (IOException e) {
                	logger.error("Error on deleting file '" + file.getPath() + "'.", e.getCause());
				}
                logger.info("Finished processing file {} as {}.", fullFileName, new Date());
            });
            logger.info("Finishing file scan in directory '{}'.", dir.getPath());
        }
        objectStorage.close();
        
        ExecutionContext ec = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
    	ec.put("Dir", dir.getName());
    	
        if (!pars.isEmpty()) {
        	ec.put("MapFiles", pars);
        }
        
        logger.info("Finished task for file upload...");
        return RepeatStatus.FINISHED;
    }
    
    private File processFile(File file, String fullFileName) {
    	try {
	    	if (zipConfig.isEnabled()) {
	    		File tempZip = new File(System.getProperty("java.io.tmpdir"), fullFileName);
	    		if (tempZip.exists()) {
	    			tempZip.delete();
	    		}
	    		ZipFile zipFile = null;
	    		ZipParameters parameters = new ZipParameters();
	    		final String password = zipConfig.getPassword(); 
	    		
	    		if (password == null) {
	    			zipFile = new ZipFile(tempZip);
	    		} else {
	    			zipFile = new ZipFile(tempZip, password.toCharArray());
	    			parameters.setEncryptFiles(true);
	    			parameters.setEncryptionMethod(EncryptionMethod.AES);
	    			parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
	    		}	    		
	    		
	            zipFile.addFile(file, parameters);
	            zipFile.close();

	            logger.info("Generated ZIP file '" + tempZip.getPath() + "'.");
	            return tempZip;
	    	} 
        } catch (FileNotFoundException e) {
            logger.error("File not found '" + file.getPath() + "'.", e.getCause());
        } catch (ZipException e) {
        	logger.error("Error on generating ZIP file '" + file.getPath() + "'.", e.getCause());
        } catch (IOException e) {
        	logger.error("Error on processing file '" + file.getPath() + "'.", e.getCause());
		}
    	return file;
    }

    private void validateAndCreateBucket(ObjectStorageComponent objectStorageComponent, ObjectStorage objectStorage, String compartmentId, String namespace, String bucketName) throws Exception {
        Assert.notNull(compartmentId, "'tenancy' must be informed on .oci file or 'compartmentId' must be informed on application.properties for bucket "+bucketName+".");

        HeadBucketResponse headBucketResponse = null;

        boolean bucketExists = false;
        try {
            headBucketResponse = objectStorageComponent.getHeadBucket(objectStorage, namespace, bucketName);
            bucketExists = headBucketResponse.get__httpStatusCode__() == 200;
        } catch (BmcException e) {
            if (e.getStatusCode() == 404) {
                bucketExists = false;
            }
        }

        Assert.isTrue(!(!bucketExists && !createBucketIfNotExists), "Bucket " + bucketName + " not found on namespace " + namespace + " on OCI and 'createBucketIfNotExists' is disabled.");

        if (!bucketExists && createBucketIfNotExists) {
            logger.info("Bucket {} will be created on namespace {} compartmentId {}.", bucketName, namespace, compartmentId);
            boolean bucketCreated = false;
            try {
                bucketCreated = objectStorageComponent.createBucket(objectStorage, compartmentId, namespace, bucketName);
            } catch (BmcException e) {
                if (e.getStatusCode() == 409) {
                    bucketCreated = true;
                }
            }
            if (bucketCreated) {
                logger.info("Bucket {} created successfully on namespace {}.", bucketName, namespace);
            }

            Assert.isTrue(bucketCreated, "Error on " + bucketName + " bucket creation.");
        }
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

    public String getBucketDir() {
        return bucketDir;
    }

    public void setBucketDir(String bucketDir) {
        this.bucketDir = bucketDir;
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

	public boolean isGeneratePreauthenticatedUrl() {
		return generatePreauthenticatedUrl;
	}

	public void setGeneratePreauthenticatedUrl(boolean generatePreauthenticatedUrl) {
		this.generatePreauthenticatedUrl = generatePreauthenticatedUrl;
	}

	public ZipConfig getZipConfig() {
		return zipConfig;
	}

	public void setZipConfig(ZipConfig zipConfig) {
		this.zipConfig = zipConfig;
	}

	public Integer getDeleteFileAfter() {
		return deleteFileAfter;
	}

	public void setDeleteFileAfter(Integer deleteFileAfter) {
		this.deleteFileAfter = deleteFileAfter;
	}
	
	public String getFilenameExtensionFilter() {
		return filenameExtensionFilter;
	}

	public void setFilenameExtensionFilter(String filenameExtensionFilter) {
		this.filenameExtensionFilter = filenameExtensionFilter;
	}

	private Integer getDaysDifBetween(Path path) throws IOException {
        FileTime fileTime = Files.getLastModifiedTime(path);
        LocalDateTime modifiedLastDate = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        LocalDateTime now = LocalDateTime.now();
        return Long.valueOf(ChronoUnit.DAYS.between(modifiedLastDate, now)).intValue();
    }
}
