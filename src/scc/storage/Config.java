package scc.storage;

import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

import scc.controllers.ImageResource;
import scc.storage.Exceptions.CosmosDatabaseIdNotFound;
import scc.storage.Exceptions.EndpointURLNotFound;
import scc.storage.Exceptions.MasterKeyNotFound;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {

    //Blob Storage Config setting
    private static final String BLOB_STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=cloud4713448043blob;AccountKey=1k4A4Tgj/3BNpZbCSzKtWwXKE1qfXy3bTQrkLsrTWwjcauExiHwf7JfknyLZZbJifPg8h+ltAO3QCH00sYZEyA==;EndpointSuffix=core.windows.net";

    //CosmosDB Config settings
    private static final String COSMOS_DB_ENDPOINT = "https://cloud-47134-48043-cosmos.documents.azure.com:443/";
    private static final String COSMOS_DB_MASTER_KEY = "COttb942kFWONxGzfIpzoubAK43NqOEm04Cia2uNKGWzycAcEgFyfxwxNJamXjUwJOjgKwE0ej3QKrpmm3GVZA==";
    private static final String COSMOS_DB_DATABASE = "cloud-47134-48043-database";

    public static void load() {
    	CosmosClient.init(COSMOS_DB_DATABASE, COSMOS_DB_MASTER_KEY, COSMOS_DB_ENDPOINT);
    	BlobStorageClient.init(BLOB_STORAGE_CONNECTION_STRING);
    }
    
}
