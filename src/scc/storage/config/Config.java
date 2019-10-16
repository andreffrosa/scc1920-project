package scc.storage.config;

import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import scc.storage.blobStorage.BlobStorageSingleton;
import scc.storage.config.Exceptions.CosmosDatabaseIdNotFound;
import scc.storage.config.Exceptions.EndpointURLNotFound;
import scc.storage.config.Exceptions.MasterKeyNotFound;
import scc.storage.cosmos.CosmosClientSingleton;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {

    //Config Types
    private static final String COSMOS = "cosmos";
    private static final String BLOB = "blobStorage";

    //Blob Storage Config setting
    public static final String BLOB_STORAGE_CONNECTION_STRING = "BLOB_STORAGE_CONNECTION_STRING";
    private static final String BLOB_STORAGE_CONNECTION_STRING_VALUE = "DefaultEndpointsProtocol=https;AccountName=cloud4713448043blob;AccountKey=1k4A4Tgj/3BNpZbCSzKtWwXKE1qfXy3bTQrkLsrTWwjcauExiHwf7JfknyLZZbJifPg8h+ltAO3QCH00sYZEyA==;EndpointSuffix=core.windows.net";

    //CosmosDB Config settings
    public static final String COSMOS_DB_MASTER_KEY = "COSMOS_DB_MASTER_KEY";
    public static final String COSMOS_DB_ENDPOINT = "COSMOS_DB_ENDPOINT";
    public static final String COSMOS_DB_DATABASE = "COSMOS_DB_DATABASE";
    private static final String COSMOS_DB_ENDPOINT_VALUE = "https://cloud-47134-48043-cosmos.documents.azure.com:443/";
    private static final String COSMOS_DB_MASTER_KEY_VALUE = "COttb942kFWONxGzfIpzoubAK43NqOEm04Cia2uNKGWzycAcEgFyfxwxNJamXjUwJOjgKwE0ej3QKrpmm3GVZA==";
    private static final String COSMOS_DB_DATABASE_VALUE = "cloud-47134-48043-database";

    private static final int PROJECT_ENOUGH_SIZE = 2;

    private static Map<String,Config> configs = new HashMap<>(PROJECT_ENOUGH_SIZE);

    private Map<String, String> props;

    private Config(String type) {

        switch (type){
            case COSMOS:
                props = new HashMap<>(3);
                props.put(COSMOS_DB_MASTER_KEY, COSMOS_DB_MASTER_KEY_VALUE);
                props.put(COSMOS_DB_ENDPOINT, COSMOS_DB_ENDPOINT_VALUE);
                props.put(COSMOS_DB_DATABASE, COSMOS_DB_DATABASE_VALUE);
                break;
            case BLOB:
                props = new HashMap<>(1);
                props.put(BLOB_STORAGE_CONNECTION_STRING, BLOB_STORAGE_CONNECTION_STRING_VALUE);
                break;
        }
    }

    private static Config getConfig(String type) {

        Config config = configs.get(type);

        if (config == null){
            config = new Config(type);
            configs.put(type, config);
        }

        return config;
    }

    public static CosmosClientSingleton getCosmosDBClientInstance() throws EndpointURLNotFound, MasterKeyNotFound, CosmosDatabaseIdNotFound, IOException {

        return CosmosClientSingleton.getInstance(COSMOS_DB_DATABASE_VALUE, COSMOS_DB_MASTER_KEY_VALUE, COSMOS_DB_ENDPOINT_VALUE);
    }

    public static BlobStorageSingleton getBlobStorageClientInstance(String containerName) throws IOException, StorageException, InvalidKeyException, URISyntaxException {

        return BlobStorageSingleton.getInstance(BLOB_STORAGE_CONNECTION_STRING_VALUE, containerName);
    }

    public Map<String, String> getProperties(){
        return props;
    }
}
