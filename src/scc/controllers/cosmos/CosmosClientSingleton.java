package scc.controllers.cosmos;

import com.microsoft.azure.cosmosdb.ConnectionMode;
import com.microsoft.azure.cosmosdb.ConnectionPolicy;
import com.microsoft.azure.cosmosdb.ConsistencyLevel;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import scc.config.Exceptions.CosmosDatabaseIdNotFound;
import scc.config.Exceptions.EndpointURLNotFound;
import scc.config.Exceptions.MasterKeyNotFound;

import java.io.IOException;
import java.util.Properties;

public class CosmosClientSingleton {

    public static final String COSMOS_CONFIG_FILE_PATH = "./config/Cosmos.conf";
    private static final String COSMOS_DB_ENDPOINT = "cosmos_db_endpoint";
    private static final String COSMOS_DB_MASTER_KEY = "cosmos_db_master_key";
    public static final String COSMOS_DB_DATABASE = "cosmos_db_database";

    private static CosmosClientSingleton cosmosClientSingleton = null;
    private AsyncDocumentClient cosmosClient;

    private CosmosClientSingleton(Properties props) throws IOException, CosmosDatabaseIdNotFound, MasterKeyNotFound, EndpointURLNotFound {

        if(!props.contains(COSMOS_DB_DATABASE)) throw new CosmosDatabaseIdNotFound();

        if (!props.contains(COSMOS_DB_MASTER_KEY)) throw new MasterKeyNotFound();

        if (!props.contains(COSMOS_DB_ENDPOINT)) throw new EndpointURLNotFound();

        ConnectionPolicy connectionPolicy = new ConnectionPolicy();
        connectionPolicy.setConnectionMode(ConnectionMode.Direct);
        cosmosClient = new AsyncDocumentClient.Builder().withServiceEndpoint(props.getProperty(COSMOS_DB_ENDPOINT))
                .withMasterKeyOrResourceToken(props.getProperty(COSMOS_DB_MASTER_KEY)).withConnectionPolicy(connectionPolicy)
                .withConsistencyLevel(ConsistencyLevel.Eventual).build();
    }

    public static CosmosClientSingleton getInstance(Properties properties) throws IOException, CosmosDatabaseIdNotFound, MasterKeyNotFound, EndpointURLNotFound {
        if(cosmosClientSingleton == null)
            cosmosClientSingleton = new CosmosClientSingleton(properties);

        return cosmosClientSingleton;
    }

    public AsyncDocumentClient getCosmosClient(){
        return cosmosClient;
    }
}
