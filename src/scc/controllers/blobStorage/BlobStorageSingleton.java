package scc.controllers.blobStorage;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Map;

public class BlobStorageSingleton {

    public static final String BLOB_STORAGE_CONFIG_FILE_PATH = "./config/Blob.conf";
    public static final String CONNECTION_STRING = "storage_connection_string";

    private static Map<String,BlobStorageSingleton> blobStorageSingletons;
    private CloudBlobContainer container;

    private BlobStorageSingleton(String connectionString, String containerName) throws URISyntaxException, StorageException, InvalidKeyException {
        // Get connection string in the storage access keys page
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        container = blobClient.getContainerReference(containerName);
    }

    public static BlobStorageSingleton getInstance(String connectionString, String containerName) throws StorageException, InvalidKeyException, URISyntaxException {
        if(blobStorageSingletons.containsKey(containerName))
            return blobStorageSingletons.get(containerName);
        else {
            BlobStorageSingleton blobStorageSingleton = new BlobStorageSingleton(connectionString, containerName);
            blobStorageSingletons.put(containerName, blobStorageSingleton);
            return blobStorageSingleton;
        }
    }

    public CloudBlobContainer getContainer(){
        return container;
    }

}
