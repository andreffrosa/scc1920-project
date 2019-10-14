package scc.controllers.blobStorage;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import scc.config.Config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Map;
import java.util.Properties;

public class BlobStorageSingleton {

    private static final String BLOB_STORAGE_CONFIG_FILE_PATH = "./config/Blob.conf";
    private static final String CONNECTION_STRING = "storage_connection_string";

    private static Map<String,BlobStorageSingleton> blobStorageSingletons;
    private CloudBlobContainer container;

    private BlobStorageSingleton(String containerName) throws URISyntaxException, StorageException, InvalidKeyException, IOException {
        // Get connection string in the storage access keys page
        Properties properties = Config.getInstance(BlobStorageSingleton.BLOB_STORAGE_CONFIG_FILE_PATH).getProperties();
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(properties.getProperty(CONNECTION_STRING));
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        container = blobClient.getContainerReference(containerName);
    }

    public static BlobStorageSingleton getInstance(String containerName) throws StorageException, InvalidKeyException, URISyntaxException, IOException {
    	BlobStorageSingleton blobStorageSingleton = blobStorageSingletons.get(containerName);
    	if(blobStorageSingleton != null)
            return blobStorageSingleton;
        else {
            blobStorageSingleton = new BlobStorageSingleton(containerName);
            blobStorageSingletons.put(containerName, blobStorageSingleton);
            return blobStorageSingleton;
        }
    }

    public CloudBlobContainer getContainer() {
        return container;
    }

}
