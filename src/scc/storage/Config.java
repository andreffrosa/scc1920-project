package scc.storage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private static final String BLOB_STORAGE_CONNECTION_STRING = "BLOB_KEY";
    private static final String COSMOS_DB_MASTER_KEY = "COSMOSDB_KEY";
    private static final String COSMOS_DB_ENDPOINT = "COSMOSDB_URL";
    private static final String COSMOSDB_DATABASE = "COSMOSDB_DATABASE";
    private static final String REDIS_HOST_NAME = "REDIS_URL";
    private static final String CACHE_KEY = "REDIS_KEY";

    private static final String PROPS_FILE = "azurekeys.props";
    private static Properties azureProperties;

    public static synchronized void load() {
        getProperties();
        CosmosClient.init(azureProperties.getProperty(COSMOSDB_DATABASE), azureProperties.getProperty(COSMOS_DB_MASTER_KEY), azureProperties.getProperty(COSMOS_DB_ENDPOINT));
    	BlobStorageClient.init(azureProperties.getProperty(BLOB_STORAGE_CONNECTION_STRING));
        Redis.init(azureProperties.getProperty(REDIS_HOST_NAME), azureProperties.getProperty(CACHE_KEY));
    }

    private static synchronized void getProperties() {

        if( azureProperties == null || azureProperties.size() == 0) {
            azureProperties = new Properties();
            try {
                azureProperties.load( new FileInputStream(PROPS_FILE));
            } catch (IOException e) {
                // do nothing
            }
        }

    }
    
}
