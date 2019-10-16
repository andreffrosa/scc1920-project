package scc.storage;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Map;
import java.util.Properties;

public class BlobStorageSingleton {

    private static Map<String,BlobStorageSingleton> blobStorageSingletons;
    private CloudBlobContainer container;

    private BlobStorageSingleton(Map<String, String> props, String containerName) throws URISyntaxException, StorageException, InvalidKeyException, IOException {
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(props.get(Config.BLOB_STORAGE_CONNECTION_STRING));
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        container = blobClient.getContainerReference(containerName);
    }

    private BlobStorageSingleton(String connectionString, String containerName) throws URISyntaxException, StorageException, InvalidKeyException {
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        container = blobClient.getContainerReference(containerName);
    }

    public static BlobStorageSingleton getInstance(String connectionString, String containerName) throws StorageException, InvalidKeyException, URISyntaxException {

        BlobStorageSingleton blobStorageSingleton = blobStorageSingletons.get(containerName);

        if (blobStorageSingleton == null){
            blobStorageSingleton = new BlobStorageSingleton(connectionString, containerName);
            blobStorageSingletons.put(containerName, blobStorageSingleton);
        }

        return blobStorageSingleton;
    }

    public static BlobStorageSingleton getInstance(Map<String, String> props, String containerName) throws StorageException, InvalidKeyException, URISyntaxException, IOException {

        BlobStorageSingleton blobStorageSingleton = blobStorageSingletons.get(containerName);

    	if(blobStorageSingleton == null){
            blobStorageSingleton = new BlobStorageSingleton(props, containerName);
            blobStorageSingletons.put(containerName, blobStorageSingleton);
        }

        return blobStorageSingleton;
    }

    public CloudBlobContainer getContainer() {
        return container;
    }

}
