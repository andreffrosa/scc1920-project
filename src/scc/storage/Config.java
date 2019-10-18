package scc.storage;

public class Config {

    //Redis Config Setting
    private static final String REDIS_HOST_NAME = "cloud-47134-48043-redis.redis.cache.windows.net";
    private static final String CACHE_KEY = "PFc1HwoQzGSL9YxfaH7YllCSPpJIJPd60PwGK5Xs2sg=";


    //Blob Storage Config setting
    private static final String BLOB_STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=cloud4713448043blob;AccountKey=1k4A4Tgj/3BNpZbCSzKtWwXKE1qfXy3bTQrkLsrTWwjcauExiHwf7JfknyLZZbJifPg8h+ltAO3QCH00sYZEyA==;EndpointSuffix=core.windows.net";

    //CosmosDB Config settings
    private static final String COSMOS_DB_ENDPOINT = "https://cloud-47134-48043-cosmos.documents.azure.com:443/";
    private static final String COSMOS_DB_MASTER_KEY = "COttb942kFWONxGzfIpzoubAK43NqOEm04Cia2uNKGWzycAcEgFyfxwxNJamXjUwJOjgKwE0ej3QKrpmm3GVZA==";
    private static final String COSMOS_DB_DATABASE = "cloud-47134-48043-database";

    public static void load() {
    	CosmosClient.init(COSMOS_DB_DATABASE, COSMOS_DB_MASTER_KEY, COSMOS_DB_ENDPOINT);
    	BlobStorageClient.init(BLOB_STORAGE_CONNECTION_STRING);
        Redis.init(REDIS_HOST_NAME, CACHE_KEY);
    }
    
}
