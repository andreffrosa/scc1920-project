package scc.storage.cosmos;

import com.microsoft.azure.cosmosdb.ConnectionMode;
import com.microsoft.azure.cosmosdb.ConnectionPolicy;
import com.microsoft.azure.cosmosdb.ConsistencyLevel;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import scc.storage.config.Config;
import scc.storage.config.Exceptions.CosmosDatabaseIdNotFound;
import scc.storage.config.Exceptions.EndpointURLNotFound;
import scc.storage.config.Exceptions.MasterKeyNotFound;

import javax.crypto.Cipher;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class CosmosClientSingleton {

    private static CosmosClientSingleton cosmosClientSingleton = null;
    private AsyncDocumentClient cosmosClient;
    private String cosmosDatabase, container;

    /*
    private CosmosClientSingleton(Map<String, String> props) throws IOException, CosmosDatabaseIdNotFound, MasterKeyNotFound, EndpointURLNotFound {

        if(!props.containsKey(Config.COSMOS_DB_DATABASE)) throw new CosmosDatabaseIdNotFound();

        if (!props.containsKey(Config.COSMOS_DB_MASTER_KEY)) throw new MasterKeyNotFound();

        if (!props.containsKey(Config.COSMOS_DB_ENDPOINT)) throw new EndpointURLNotFound();

        new CosmosClientSingleton(props.get(Config.COSMOS_DB_DATABASE), props.get(Config.COSMOS_DB_MASTER_KEY), props.get(Config.COSMOS_DB_ENDPOINT));
    }
    */
    
    private CosmosClientSingleton(String container, String cosmosDB, String cosmosMasterKey, String cosmosEndpoint) {

        this.container = container;
        this.cosmosDatabase = cosmosDB;

        ConnectionPolicy connectionPolicy = new ConnectionPolicy();
        connectionPolicy.setConnectionMode(ConnectionMode.Direct);
        cosmosClient = new AsyncDocumentClient.Builder().withServiceEndpoint(cosmosEndpoint)
                .withMasterKeyOrResourceToken(cosmosMasterKey).withConnectionPolicy(connectionPolicy)
                .withConsistencyLevel(ConsistencyLevel.Eventual).build();
    }

    public static CosmosClientSingleton getInstance(String container, String cosmosDB, String cosmosMasterKey, String cosmosEndpoint) {
        if(cosmosClientSingleton == null)
            cosmosClientSingleton = new CosmosClientSingleton(container, cosmosDB, cosmosMasterKey, cosmosEndpoint);

        return cosmosClientSingleton;
    }

    public AsyncDocumentClient getCosmosClient(){
        return cosmosClient;
    }

    public String getCosmosDatabase(){
        return cosmosDatabase;
    }
}
