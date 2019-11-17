package scc.management;

import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.cosmosdb.CosmosDBAccount;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.StorageAccount;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import static scc.management.AzureManagement.*;

public class Deployment {


    public static void main(String[] args) {
        try {
            Azure azure = createManagementClient(AZURE_AUTH_LOCATION);
            if (args.length == 1) {
                switch (args[0].toLowerCase()) {
                    case "-delete":
                        deleteResourceGroup(azure, AZURE_RG_SERVERLESS_REGION1);
                        deleteResourceGroup(azure, AZURE_RG_REGION1);
                        deleteResourceGroup(azure, AZURE_RG_REGION2);
                        deleteResourceGroup(azure, AZURE_RG_SERVERLESS_REGION2);
                        break;
                    case "simple-init":
                        createResourceGroup(azure, AZURE_RG_EUWEST, Region.EUROPE_WEST);
                        createResourceGroup(azure, AZURE_RG_SERVERLESS_EUWEST, Region.EUROPE_WEST);
                        Files.deleteIfExists(Paths.get(AZURE_PROPS_REGION1_LOCATION));

                        if (!Files.exists(Paths.get(AZURE_PROPS_REGION1_LOCATION)))
                            Files.createFile(Paths.get(AZURE_PROPS_REGION1_LOCATION));

                        Files.write(Paths.get(AZURE_PROPS_REGION1_LOCATION),
                                ("# Date : " + new SimpleDateFormat().format(new Date()) + "\n").getBytes(),
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        //Files.deleteIfExists(Paths.get(AZURE_PROPS_LOCATION));
                        Files.deleteIfExists(Paths.get("set-app-config.sh"));
                        Files.write(Paths.get("set-app-config.sh"), "".getBytes(),
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                        new Thread(() -> {
                            try {
                                Azure azure1 = createManagementClient(AZURE_AUTH_LOCATION);
                                StorageAccount accountStorage = createStorageAccount(azure1, AZURE_RG_EUWEST, AZURE_STORAGE_REGION1_NAME,
                                        Region.EUROPE_WEST);
                                dumpStorageKey(AZURE_PROPS_REGION1_LOCATION, AZURE_FUNCTIONS_SETTINGS_REGION1,
                                        AZURE_SERVERLESS_REGION1_NAME, AZURE_RG_SERVERLESS_REGION1, accountStorage);
                                createBlobContainer(azure1, AZURE_RG_EUWEST, AZURE_STORAGE_REGION1_NAME, AZURE_BLOB_MEDIA);
                            } catch (Exception e) {
                                System.err.println("Error while creating storage resources");
                                e.printStackTrace();
                            }
                        }).start();
                        new Thread(() -> {
                            try {
                                Azure azure2 = createManagementClient(AZURE_AUTH_LOCATION);
                                CosmosDBAccount accountCosmosDB = createReplicatedCosmosDBAccount(azure2, AZURE_RG_REGION1,
                                        AZURE_COSMOSDB_NAME, REGION1, null);
                                dumpCosmosDBKey(AZURE_PROPS_REGION1_LOCATION, AZURE_FUNCTIONS_SETTINGS_REGION1,
                                        AZURE_SERVERLESS_REGION1_NAME, AZURE_RG_SERVERLESS_REGION1, accountCosmosDB);
                                createCosmosContainers(accountCosmosDB, true);
                            } catch (Exception e) {
                                System.err.println("Error while creating cosmosdb resources");
                                e.printStackTrace();
                            }
                        }).start();

                        new Thread(() -> {
                            try {
                                Azure azure2 = createManagementClient(AZURE_AUTH_LOCATION);
                                RedisCache cache = createRedis(azure2, AZURE_RG_EUWEST, AZURE_REDIS_REGION1_NAME, Region.EUROPE_WEST);
                                dumpRedisCacheInfo(AZURE_PROPS_REGION1_LOCATION, cache);
                            } catch (Exception e) {
                                System.err.println("Error while creating redis resources");
                                e.printStackTrace();
                            }
                        }).start();
                        break;
                    case "init-georeplicated":

                        //Creating Resource Groups
                        Azure azure0 = createManagementClient(AZURE_AUTH_LOCATION);
                        createResourceGroup(azure0, AZURE_RG_REGION1, REGION1);
                        Files.deleteIfExists(Paths.get(AZURE_PROPS_REGION1_LOCATION));
                        Files.write(Paths.get(AZURE_PROPS_REGION1_LOCATION),
                                ("# Date : " + new SimpleDateFormat().format(new Date()) + "\n").getBytes(),
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        Files.deleteIfExists(Paths.get(AZURE_FUNCTIONS_SETTINGS_REGION1));
                        Files.write(Paths.get(AZURE_FUNCTIONS_SETTINGS_REGION1), "".getBytes(),
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                        createResourceGroup(azure0, AZURE_RG_REGION2, REGION2);
                        Files.deleteIfExists(Paths.get(AZURE_PROPS_REGION2_LOCATION));
                        Files.write(Paths.get(AZURE_PROPS_REGION2_LOCATION),
                                ("# Date : " + new SimpleDateFormat().format(new Date()) + "\n").getBytes(),
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        Files.deleteIfExists(Paths.get(AZURE_FUNCTIONS_SETTINGS_REGION2));
                        Files.write(Paths.get(AZURE_FUNCTIONS_SETTINGS_REGION2), "".getBytes(),
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                        //Creating Blob Storage
                        new Thread(() -> {
                            try {
                                Azure azure1 = createManagementClient(AZURE_AUTH_LOCATION);
                                StorageAccount accountStorage = createStorageAccount(azure1, AZURE_RG_REGION1, AZURE_STORAGE_REGION1_NAME,
                                        REGION1);
                                dumpStorageKey(AZURE_PROPS_REGION1_LOCATION, AZURE_FUNCTIONS_SETTINGS_REGION1,
                                        AZURE_SERVERLESS_REGION1_NAME, AZURE_RG_SERVERLESS_REGION1, accountStorage);
                                createBlobContainer(azure1, AZURE_RG_REGION1, AZURE_STORAGE_REGION1_NAME, AZURE_BLOB_MEDIA);

                                accountStorage = createStorageAccount(azure1, AZURE_RG_REGION2, AZURE_STORAGE_REGION2_NAME,
                                        REGION2);
                                dumpStorageKey(AZURE_PROPS_REGION2_LOCATION, AZURE_FUNCTIONS_SETTINGS_REGION2,
                                        AZURE_SERVERLESS_REGION2_NAME, AZURE_RG_SERVERLESS_REGION2, accountStorage);
                                createBlobContainer(azure1, AZURE_RG_REGION2, AZURE_STORAGE_REGION2_NAME, AZURE_BLOB_MEDIA);
                            } catch (Exception e) {
                                System.err.println("Error while creating storage resources");
                                e.printStackTrace();
                            }
                        }).start();

                        //Creating CosmosDB accounts
                        new Thread(() -> {
                            try {
                                Azure azure2 = createManagementClient(AZURE_AUTH_LOCATION);
                                CosmosDBAccount accountCosmosDB = createReplicatedCosmosDBAccount(azure2, AZURE_RG_REGION1,
                                        AZURE_COSMOSDB_NAME, REGION1, REGION2);
                                dumpCosmosDBKey(AZURE_PROPS_REGION1_LOCATION, AZURE_FUNCTIONS_SETTINGS_REGION1,
                                        AZURE_SERVERLESS_REGION1_NAME, AZURE_RG_SERVERLESS_REGION1, accountCosmosDB);

                                dumpCosmosDBKey(AZURE_PROPS_REGION2_LOCATION, AZURE_FUNCTIONS_SETTINGS_REGION2,
                                        AZURE_SERVERLESS_REGION2_NAME, AZURE_RG_SERVERLESS_REGION2, accountCosmosDB);

                                AsyncDocumentClient cosmosClient = getDocumentClient(accountCosmosDB);
                                createDatabase(cosmosClient, AZURE_COSMOSDB_DATABASE);
                                createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, "Users", "/name", "/name");
                                createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, "Posts", "/community", null);
                                createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, "Communities", "/name", null);
                            } catch (Exception e) {
                                System.err.println("Error while creating cosmosdb resources");
                                e.printStackTrace();
                            }
                        }).start();

                        //Creating Redis
                        new Thread(() -> {
                            try {
                                Azure azure3 = createManagementClient(AZURE_AUTH_LOCATION);
                                RedisCache cache = createRedis(azure3, AZURE_RG_REGION1, AZURE_REDIS_REGION1_NAME,
                                        REGION1);
                                dumpRedisCacheInfo(AZURE_PROPS_REGION1_LOCATION, cache);

                                cache = createRedis(azure3, AZURE_RG_REGION2, AZURE_REDIS_REGION2_NAME,
                                        REGION2);
                                dumpRedisCacheInfo(AZURE_PROPS_REGION2_LOCATION, cache);

                            } catch (Exception e) {

                                System.err.println("Error while creating functions resources");
                                e.printStackTrace();
                            }
                        }).start();
                }
            } else
                System.out.println("Usage:\njava GeoReplication init region1 region2");

        } catch (Exception e) {
            System.err.println("Error while creating resources");
            e.printStackTrace();
        }
    }
}
