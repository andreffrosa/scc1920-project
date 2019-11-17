package scc.management;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.cosmosdb.*;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.cosmosdb.CosmosDBAccount;
import com.microsoft.azure.management.cosmosdb.KeyKind;
import com.microsoft.azure.management.redis.RedisAccessKeys;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.redis.RedisKeyType;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.storage.*;
import com.microsoft.rest.LogLevel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class AzureManagement {


    static final String AZURE_AUTH_LOCATION = "azure.auth";

    static final String MY_SUFFIX = "47134"; // Add your suffix here

    static final Region REGION1 = Region.EUROPE_WEST; // Define the regions to deploy resources here
    static final String REGION1_SUFFIX = "westeu";
    static final Region REGION2 = null; // Region.US_WEST2;
    static final String REGION2_SUFFIX = "westus";

    static final String AZURE_PROPS_REGION1_LOCATION = "azurekeys-" + REGION1_SUFFIX + ".props";
    static final String AZURE_FUNCTIONS_SETTINGS_REGION1 = "set-app-config-" + REGION1_SUFFIX + ".sh";
    static final String AZURE_PROPS_REGION2_LOCATION = "azurekeys-" + REGION2_SUFFIX + ".props";
    static final String AZURE_FUNCTIONS_SETTINGS_REGION2 = "set-app-config-" + REGION2_SUFFIX + ".sh";

    static final String AZURE_STORAGE_NAME_PREFIX = "sccstore";
    static final String AZURE_STORAGE_REGION1_NAME = AZURE_STORAGE_NAME_PREFIX + REGION1_SUFFIX;
    static final String AZURE_STORAGE_REGION2_NAME = AZURE_STORAGE_NAME_PREFIX + REGION2_SUFFIX;
    static final String AZURE_BLOB_MEDIA = "images";

    static final String AZURE_COSMOSDB_NAME = "scccosmos" + MY_SUFFIX;
    static final String AZURE_COSMOSDB_DATABASE = "scccosmosdb" + MY_SUFFIX;

    static final String AZURE_REDIS_NAME_PREFIX = "sccredis";
    static final String AZURE_REDIS_REGION1_NAME = AZURE_REDIS_NAME_PREFIX + REGION1_SUFFIX;
    static final String AZURE_REDIS_REGION2_NAME = AZURE_REDIS_NAME_PREFIX + REGION2_SUFFIX;

    static final String AZURE_SERVERLESS_PREFIX = "scc-serverless-";
    static final String AZURE_SERVERLESS_REGION1_NAME = AZURE_SERVERLESS_PREFIX + REGION1_SUFFIX;
    static final String AZURE_SERVERLESS_REGION2_NAME = AZURE_SERVERLESS_PREFIX + REGION2_SUFFIX;

    static final String AZURE_RG_REGION1 = "scc-backend-" + REGION1_SUFFIX + "-" + MY_SUFFIX;
    static final String AZURE_RG_SERVERLESS_REGION1 = "scc-serverless-" + REGION1_SUFFIX + "-" + MY_SUFFIX;
    static final String AZURE_RG_REGION2 = "scc-backend-" + REGION2_SUFFIX + "-" + MY_SUFFIX;
    static final String AZURE_RG_SERVERLESS_REGION2 = "scc-serverless-" + REGION2_SUFFIX + "-" + MY_SUFFIX;

    static final String AZURE_RG_EUWEST = "scc-backend-euwest-" + MY_SUFFIX;
    static final String AZURE_RG_SERVERLESS_EUWEST = "scc-serverless-euwest-" + MY_SUFFIX;
    static final String USERS_COLLECTION = "Users";
    static final String POSTS_COLLECTION = "Posts";
    static final String COMMUNITIES_COLLECTION = "Communities";
    static final String LIKES_COLLECTION = "Likes";

    static Azure createManagementClient(String authFile) throws CloudException, IOException {
        File credFile = new File(authFile);
        Azure azure = Azure.configure().withLogLevel(LogLevel.BASIC).authenticate(credFile).withDefaultSubscription();
        System.out.println("Azure client created with success");
        return azure;
    }

    static void createResourceGroup(Azure azure, String rgName, Region region) {
        azure.resourceGroups()
                .define(rgName)
                .withRegion(region)
                .create();
    }

    static StorageAccount createStorageAccount(Azure azure, String rgName, String name, Region region) {
        StorageAccount storageAccount = azure.storageAccounts().define(name).withRegion(region)
                .withNewResourceGroup(rgName).withGeneralPurposeAccountKindV2().withAccessFromAllNetworks()
                .withBlobStorageAccountKind().withAccessTier(AccessTier.HOT)
                .withSku(StorageAccountSkuType.STANDARD_RAGRS).create();
        System.out.println("Storage account created with success: name = " + name + " ; group = " + rgName
                + " ; region = " + region.name());
        return storageAccount;
    }

    static BlobContainer createBlobContainer(Azure azure, String rgName, String accountName,
                                                     String containerName) {
        BlobContainer container = azure.storageBlobContainers().defineContainer(containerName)
                .withExistingBlobService(rgName, accountName).withPublicAccess(PublicAccess.BLOB).create();
        System.out.println("Blob container created with success: name = " + containerName + " ; group = " + rgName
                + " ; account = " + accountName);
        return container;
    }

    synchronized static void dumpStorageKey(String propFilename, String settingsFilename,
                                            String functionsName, String functionsRGName, StorageAccount account) throws IOException {
        List<StorageAccountKey> storageAccountKeys = account.getKeys();
        storageAccountKeys = account.regenerateKey(storageAccountKeys.get(0).keyName());
        synchronized(AzureManagement.class) {
            Files.write(Paths.get(propFilename),
                    ("BLOB_KEY=DefaultEndpointsProtocol=https;AccountName=" + account.name() + ";AccountKey="
                            + storageAccountKeys.get(0).value() + ";EndpointSuffix=core.windows.net\n").getBytes(),
                    StandardOpenOption.APPEND);
        }
        StringBuffer cmd = new StringBuffer();
        cmd.append("az functionapp config appsettings set --name ");
        cmd.append(functionsName);
        cmd.append(" --resource-group ");
        cmd.append(functionsRGName);
        cmd.append(" --settings \"BlobStoreConnection=DefaultEndpointsProtocol=https;AccountName=");
        cmd.append(account.name());
        cmd.append(";AccountKey=");
        cmd.append(storageAccountKeys.get(0).value());
        cmd.append(";EndpointSuffix=core.windows.net\"\n");
        synchronized(AzureManagement.class) {
            Files.write(Paths.get(settingsFilename), cmd.toString().getBytes(),
                    StandardOpenOption.APPEND);
        }
    }


    static CosmosDBAccount createReplicatedCosmosDBAccount(Azure azure, String rgName, String name, Region region,
                                                 Region region2) {
        CosmosDBAccount.DefinitionStages.WithConsistencyPolicy step = azure.cosmosDBAccounts().define(name).withRegion(region)
                .withExistingResourceGroup(rgName).withDataModelSql();
        CosmosDBAccount account = null;
        if (region2 != null) {
            account = step.withSessionConsistency().withWriteReplication(region2)
                    .withMultipleWriteLocationsEnabled(true).create();
        } else
            account = step.withSessionConsistency().withWriteReplication(region).create();
        account.regenerateKey(KeyKind.PRIMARY);
        System.out.println("CosmosDB account created with success: name = " + name + " ; group = " + rgName
                + " ; region = " + region.name() + " ; region 2 = " + (region2 == null ? "null" : region2.name()));
        return account;
    }

    synchronized static void dumpCosmosDBKey(String propFilename, String settingsFilename,
                                                    String functionsName, String functionsRGName, CosmosDBAccount account) throws IOException {
        synchronized(AzureManagement.class) {
            Files.write(Paths.get(propFilename),
                    ("COSMOSDB_KEY=" + account.listKeys().primaryMasterKey() + "\n").getBytes(), StandardOpenOption.APPEND);
            Files.write(Paths.get(propFilename), ("COSMOSDB_URL=" + account.documentEndpoint() + "\n").getBytes(),
                    StandardOpenOption.APPEND);
            Files.write(Paths.get(propFilename), ("COSMOSDB_DATABASE=" + AZURE_COSMOSDB_DATABASE + "\n").getBytes(),
                    StandardOpenOption.APPEND);
        }

        StringBuffer cmd = new StringBuffer();
        cmd.append("az functionapp config appsettings set --name ");
        cmd.append(functionsName);
        cmd.append(" --resource-group ");
        cmd.append(functionsRGName);
        cmd.append(" --settings \"AzureCosmosDBConnection=AccountEndpoint=");
        cmd.append(account.documentEndpoint());
        cmd.append(";AccountKey=");
        cmd.append(account.listKeys().primaryMasterKey());
        cmd.append(";\"");
        synchronized(AzureManagement.class) {
            Files.write(Paths.get(settingsFilename), cmd.toString().getBytes(),
                    StandardOpenOption.APPEND);
        }
    }

    static AsyncDocumentClient getDocumentClient(CosmosDBAccount account) {
        ConnectionPolicy connectionPolicy = ConnectionPolicy.GetDefault();
        // connectionPolicy.setConnectionMode(ConnectionMode.Direct);
        AsyncDocumentClient client = new AsyncDocumentClient.Builder().withServiceEndpoint(account.documentEndpoint())
                .withMasterKeyOrResourceToken(account.listKeys().primaryMasterKey())
                .withConnectionPolicy(connectionPolicy).withConsistencyLevel(ConsistencyLevel.Session).build();
        System.out.println("CosmosDB client created with success: name = " + account.name());
        return client;
    }

    static String getDatabaseString(String dbname) {
        return String.format("/dbs/%s", dbname);
    }

    static String getCollectionString(String col) {
        return String.format("/dbs/%s/colls/%s", AZURE_COSMOSDB_DATABASE, col);
    }

    static void createDatabase(AsyncDocumentClient client, String dbname) {
        // create database if not exists
        List<Database> databaseList = client.queryDatabases("SELECT * FROM root r WHERE r.id='" + dbname + "'", null)
                .toBlocking().first().getResults();
        if (databaseList.size() == 0) {
            try {
                Database databaseDefinition = new Database();
                databaseDefinition.setId(dbname);
                client.createDatabase(databaseDefinition, null).toCompletable().await();
                System.out.println("CosmosDB database created with success: name = " + dbname);
            } catch (Exception e) {
                // TODO: Something has gone terribly wrong.
                e.printStackTrace();
                return;
            }
        }

    }

    static void createCollection(AsyncDocumentClient client, String dbname, String collectionName, String partKeys,
                                 String uniqueKeys) {
        List<DocumentCollection> collectionList = client.queryCollections(getDatabaseString(dbname),
                "SELECT * FROM root r WHERE r.id='" + collectionName + "'", null).toBlocking().first().getResults();

        if (collectionList.size() == 0) {
            try {
                String databaseLink = getDatabaseString(dbname);
                DocumentCollection collectionDefinition = new DocumentCollection();
                collectionDefinition.setId(collectionName);
                PartitionKeyDefinition partitionKeyDef = new PartitionKeyDefinition();
                partitionKeyDef.setPaths(Arrays.asList(partKeys));
                collectionDefinition.setPartitionKey(partitionKeyDef);

                if (uniqueKeys != null) {
                    UniqueKeyPolicy uniqueKeyDef = new UniqueKeyPolicy();
                    UniqueKey uniqueKey = new UniqueKey();
                    uniqueKey.setPaths(Arrays.asList(uniqueKeys));
                    uniqueKeyDef.setUniqueKeys(Arrays.asList(uniqueKey));
                    collectionDefinition.setUniqueKeyPolicy(uniqueKeyDef);
                }

                client.createCollection(databaseLink, collectionDefinition, null).toCompletable().await();
                System.out.println("CosmosDB collection created with success: name = " + collectionName + "@" + dbname);

            } catch (Exception e) {
                // TODO: Something has gone terribly wrong.
                e.printStackTrace();
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    static RedisCache createRedis(Azure azure, String rgName, String name, Region region) {
        try {
            Creatable<RedisCache> redisCacheDefinition = azure.redisCaches().define(name).withRegion(region)
                    .withNewResourceGroup(rgName).withBasicSku(0);

            return azure.redisCaches().create(redisCacheDefinition).get(redisCacheDefinition.key());
        } finally {
            System.out.println("Redis cache created with success: name = " + name + "@" + region);
        }
    }

    synchronized static void dumpRedisCacheInfo(String propFilename, RedisCache cache) throws IOException {
        RedisAccessKeys redisAccessKey = cache.regenerateKey(RedisKeyType.PRIMARY);
        synchronized(AzureManagement.class) {
            Files.write(Paths.get(propFilename),
                    ("REDIS_KEY=" + redisAccessKey.primaryKey() + "\n").getBytes(),
                    StandardOpenOption.APPEND);
            Files.write(Paths.get(propFilename), ("REDIS_URL=" + cache.hostName() + "\n").getBytes(),
                    StandardOpenOption.APPEND);
        }
    }

    static void deleteResourceGroup(Azure azure, String rgName) {
        azure.resourceGroups().deleteByName(rgName);
    }

    static void createCosmosContainers(CosmosDBAccount accountCosmosDB, boolean createDB) {
        AsyncDocumentClient cosmosClient = getDocumentClient(accountCosmosDB);
        if(createDB) createDatabase(cosmosClient, AZURE_COSMOSDB_DATABASE);
        createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, USERS_COLLECTION, "/name", "/name");
        createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, POSTS_COLLECTION, "/community", null);
        createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, COMMUNITIES_COLLECTION, "/name", "/name");
        createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, LIKES_COLLECTION, "/post_id", null);
    }

}
