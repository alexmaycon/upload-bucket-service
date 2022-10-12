package dev.alexmaycon.bucketservice.oci;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.Bucket;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.model.ListObjects;
import com.oracle.bmc.objectstorage.requests.*;
import com.oracle.bmc.objectstorage.responses.*;
import com.oracle.bmc.retrier.DefaultRetryCondition;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.retrier.RetryOptions;
import dev.alexmaycon.bucketservice.config.ServiceConfiguration;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.HashMap;

@Component
public class ObjectStorageComponent {

    private final ServiceConfiguration serviceConfiguration;

    @Autowired
    public ObjectStorageComponent(ServiceConfiguration serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    private RetryConfiguration retryConfiguration() {
        DefaultRetryCondition defaultRetryCondition = new DefaultRetryCondition();
        return RetryConfiguration
                .builder()
                .retryCondition(defaultRetryCondition)
                .retryOptions(new RetryOptions(serviceConfiguration.getService().getAttemptsFailure()))
                .build();
    }

    public final ObjectStorage getObjectStorage(ConfigFileReader.ConfigFile configFile, AuthenticationDetailsProvider provider) throws Exception {
        ObjectStorage client = new ObjectStorageClient(provider);
        String regionId = configFile.get("region");
        if (regionId == null || Strings.isEmpty(regionId)) {
            // TODO: Criar exceção personalizada
            throw new Exception("Region not informed in the configuration file.");
        }
        client.setRegion(Region.fromRegionId(regionId));
        return client;
    }

    public String getNamespace(ObjectStorage objectStorage) {
        GetNamespaceResponse response = objectStorage.getNamespace(GetNamespaceRequest.builder().retryConfiguration(retryConfiguration()).build());
        return response.getValue();
    }

    public Bucket getBucket(ObjectStorage objectStorage, String namespace, String bucketName) {
        return objectStorage.getBucket(GetBucketRequest.builder()
                .retryConfiguration(retryConfiguration())
                .namespaceName(namespace)
                .bucketName(bucketName).build()).getBucket();
    }

    public HeadBucketResponse getHeadBucket(ObjectStorage objectStorage, String namespace,  String bucketName){
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .retryConfiguration(retryConfiguration())
                .namespaceName(namespace)
                .bucketName(bucketName)
                .build();

        return objectStorage.headBucket(headBucketRequest);
    }

    public boolean createBucket(ObjectStorage objectStorage, String compartmentId, String namespace, String bucketName) throws Exception {
        CreateBucketDetails createBucketDetails = CreateBucketDetails.builder()
                .compartmentId(compartmentId)
                .name(bucketName)
                .publicAccessType(CreateBucketDetails.PublicAccessType.NoPublicAccess)
                .storageTier(CreateBucketDetails.StorageTier.Standard)
                .objectEventsEnabled(false)
                .versioning(CreateBucketDetails.Versioning.Disabled)
                .autoTiering(Bucket.AutoTiering.Disabled).build();

        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .namespaceName(namespace)
                .createBucketDetails(createBucketDetails)
                .build();

        CreateBucketResponse response = objectStorage.createBucket(createBucketRequest);
        return response.get__httpStatusCode__() == 200;
    }

    public ListObjects getObjects(ObjectStorage objectStorage, String namespace, String bucketName) throws BmcException {
        try {
            ListObjectsResponse listObjectsResponse = objectStorage.listObjects(ListObjectsRequest.builder()
                    .retryConfiguration(retryConfiguration())
                    .namespaceName(namespace)
                    .bucketName(bucketName).build());

            return listObjectsResponse.getListObjects();
        } catch (BmcException e) {
            System.out.println(e.getStatusCode());
            System.out.println(e.getServiceCode());
            System.out.println(e.getMessage());
            // TODO: Criar exceção personalizada
            throw e;
        }
    }

    public GetObjectResponse getObject(ObjectStorage objectStorage, String namespace, String bucketName, String objectName) throws BmcException {
        return objectStorage.getObject(GetObjectRequest.builder()
                .retryConfiguration(retryConfiguration())
                .namespaceName(namespace)
                .bucketName(bucketName)
                .objectName(objectName).build());
    }

    public boolean uploadFile(ObjectStorage objectStorage, String namespaceName, String bucketName, String objectName, Long length, String md5, InputStream objectBody) {
        HashMap<String, String> opcMeta = new HashMap<>();
        opcMeta.put("file-md5",md5);

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .retryConfiguration(retryConfiguration())
                        .namespaceName(namespaceName)
                        .bucketName(bucketName)
                        .objectName(objectName)
                        .opcMeta(opcMeta)
                        .contentLength(length)
                        .putObjectBody(
                                objectBody)
                        .build();
        PutObjectResponse response = objectStorage.putObject(putObjectRequest);
        return response.get__httpStatusCode__() == 200;
    }

    public boolean createDirectory(ObjectStorage objectStorage, String namespaceName, String bucketName, String directoryName) {
        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .retryConfiguration(retryConfiguration())
                        .namespaceName(namespaceName)
                        .bucketName(bucketName)
                        .objectName(directoryName.replace("/", "")
                                .replace(FileSystems.getDefault().getSeparator(), "")
                                .concat(FileSystems.getDefault().getSeparator()))
                        .contentLength(0L)
                        .putObjectBody(
                                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)))
                        .build();
        PutObjectResponse response = objectStorage.putObject(putObjectRequest);
        return response.get__httpStatusCode__() == 200;
    }

    public boolean deleteObject(ObjectStorage objectStorage, String namespaceName, String bucketName, String objectName) {
        DeleteObjectResponse deleteObjectResponse = objectStorage.deleteObject(DeleteObjectRequest.builder()
                .retryConfiguration(retryConfiguration())
                .namespaceName(namespaceName)
                .bucketName(bucketName)
                .objectName(objectName)
                .build());
        return deleteObjectResponse.get__httpStatusCode__() == 200;
    }

    public HeadObjectResponse getHeadObject(ObjectStorage objectStorage, String namespaceName, String bucketName, String objectName) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .retryConfiguration(retryConfiguration())
                .namespaceName(namespaceName)
                .bucketName(bucketName)
                .objectName(objectName).build();

        return objectStorage.headObject(headObjectRequest);
    }

    public static String getFullObjectName(String dir, String objectName) {
        final String fullObjectName = (dir == null || Strings.isEmpty(dir)
                ? objectName
                : dir.concat("/".concat(objectName))); //FileSystems.getDefault().getSeparator()
        return fullObjectName;
    }
}
