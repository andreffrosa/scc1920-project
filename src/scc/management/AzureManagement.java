package scc.management;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.cosmosdb.ConnectionPolicy;
import com.microsoft.azure.cosmosdb.ConsistencyLevel;
import com.microsoft.azure.cosmosdb.Database;
import com.microsoft.azure.cosmosdb.DocumentCollection;
import com.microsoft.azure.cosmosdb.PartitionKeyDefinition;
import com.microsoft.azure.cosmosdb.UniqueKey;
import com.microsoft.azure.cosmosdb.UniqueKeyPolicy;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.cosmosdb.CosmosDBAccount;
import com.microsoft.azure.management.cosmosdb.CosmosDBAccount.DefinitionStages.WithConsistencyPolicy;
import com.microsoft.azure.management.cosmosdb.KeyKind;
import com.microsoft.azure.management.redis.RedisAccessKeys;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.redis.RedisKeyType;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.storage.AccessTier;
import com.microsoft.azure.management.storage.BlobContainer;
import com.microsoft.azure.management.storage.PublicAccess;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountKey;
import com.microsoft.azure.management.storage.StorageAccountSkuType;
import com.microsoft.rest.LogLevel;

public class AzureManagement {
	// Auth file location
	// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11
	// This file should be created by running in the console:
	// az ad sp create-for-rbac --sdk-auth > azure.auth
	private static final String AZURE_AUTH_LOCATION = "azure.auth";
	private static final String AZURE_PROPS_LOCATION = "azurekeys.props";

	private static final String MY_SUFFIX = "48043"; // Add your suffix here

	private static final String AZURE_STORAGE_NAME = "sccstoregeeuwest" + MY_SUFFIX;
	private static final String AZURE_BLOB_MEDIA = "images";

	private static final String AZURE_COSMOSDB_NAME = "scc-cosmos-" + MY_SUFFIX;
	private static final String AZURE_COSMOSDB_DATABASE = "scc-cosmosdb-" + MY_SUFFIX;

	private static final String AZURE_REDIS_NAME = "scc-redis-" + MY_SUFFIX;

	private static final String AZURE_SERVERLESS_NAME = "scc-serverless-" + MY_SUFFIX;

	private static final String AZURE_RG_EUWEST = "scc-backend-euwest-" + MY_SUFFIX;
	private static final String AZURE_RG_SERVERLESS_EUWEST = "scc-serverless-euwest-" + MY_SUFFIX;

	public static Azure cretaeManagementClient(String authFile) throws CloudException, IOException {
		File credFile = new File(AZURE_AUTH_LOCATION);

		Azure azure = Azure.configure().withLogLevel(LogLevel.BASIC).authenticate(credFile).withDefaultSubscription();
		System.out.println("Azure client created with success");
		return azure;
	}

	public static ResourceGroup createResourceGroup(Azure azure, String rgName, Region region) {
        ResourceGroup resourceGroup = azure.resourceGroups().define(rgName)
                .withRegion(region)
                .create();
        return resourceGroup;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////// Azure Storage Account CODE
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static StorageAccount createStorageAccount(Azure azure, String rgName, String name, Region region) {
		StorageAccount storageAccount = azure.storageAccounts().define(name).withRegion(region)
				.withNewResourceGroup(rgName).withGeneralPurposeAccountKindV2().withAccessFromAllNetworks()
				.withBlobStorageAccountKind().withAccessTier(AccessTier.HOT)
				.withSku(StorageAccountSkuType.STANDARD_RAGRS).create();
		System.out.println("Storage account created with success: name = " + name + " ; group = " + rgName
				+ " ; region = " + region.name());
		return storageAccount;
	}

	private static BlobContainer createBlobContainer(Azure azure, String rgName, String accountName,
			String containerName) {
		BlobContainer container = azure.storageBlobContainers().defineContainer(containerName)
				.withExistingBlobService(rgName, accountName).withPublicAccess(PublicAccess.BLOB).create();
		System.out.println("Blob container created with success: name = " + containerName + " ; group = " + rgName
				+ " ; account = " + accountName);
		return container;
	}

	public synchronized static void dumpStorageKey(String propFilename, StorageAccount account) throws IOException {
		List<StorageAccountKey> storageAccountKeys = account.getKeys();
		storageAccountKeys = account.regenerateKey(storageAccountKeys.get(0).keyName());
		
		Files.write(Paths.get(propFilename),
				("BLOB_KEY=DefaultEndpointsProtocol=https;AccountName=" + account.name() + ";AccountKey="
						+ storageAccountKeys.get(0).value() + ";EndpointSuffix=core.windows.net\n").getBytes(),
				StandardOpenOption.APPEND);
		StringBuffer cmd = new StringBuffer();
		cmd.append("az functionapp config appsettings set --name ");
		cmd.append(AZURE_SERVERLESS_NAME);
		cmd.append(" --resource-group ");
		cmd.append(AZURE_RG_SERVERLESS_EUWEST);
		cmd.append(" --settings \"BlobStoreConnection=DefaultEndpointsProtocol=https;AccountName=");
		cmd.append(account.name());
		cmd.append(";AccountKey=");
		cmd.append(storageAccountKeys.get(0).value());
		cmd.append(";EndpointSuffix=core.windows.net\"\n");
		synchronized(AzureManagement.class) {
			Files.write(Paths.get("set-app-config.sh"), cmd.toString().getBytes(),
				StandardOpenOption.APPEND);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////// COSMOS DB CODE
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static CosmosDBAccount createCosmosDBAccount(Azure azure, String rgName, String name, Region region,
			Region region2) {
		WithConsistencyPolicy step = azure.cosmosDBAccounts().define(name).withRegion(region)
				.withExistingResourceGroup(rgName).withDataModelSql();
		CosmosDBAccount account = null;
		if (region2 != null) {
			account = step.withSessionConsistency().withWriteReplication(region2)
					.withMultipleWriteLocationsEnabled(true).create();
		} else
			account = step.withSessionConsistency().withWriteReplication(region).create();
		System.out.println("CosmosDB account created with success: name = " + name + " ; group = " + rgName
				+ " ; region = " + region.name() + " ; region 2 = " + (region2 == null ? "null" : region2.name()));
		return account;
	}

	public synchronized static void dumpCosmosDBKey(String propFilename, CosmosDBAccount account) throws IOException {
		account.regenerateKey(KeyKind.PRIMARY);
		Files.write(Paths.get(propFilename),
				("COSMOSDB_KEY=" + account.listKeys().primaryMasterKey() + "\n").getBytes(), StandardOpenOption.APPEND);
		Files.write(Paths.get(propFilename), ("COSMOSDB_URL=" + account.documentEndpoint() + "\n").getBytes(),
				StandardOpenOption.APPEND);
		Files.write(Paths.get(propFilename), ("COSMOSDB_DATABASE=" + AZURE_COSMOSDB_DATABASE + "\n").getBytes(),
				StandardOpenOption.APPEND);
		
		StringBuffer cmd = new StringBuffer();
		cmd.append("az functionapp config appsettings set --name ");
		cmd.append(AZURE_SERVERLESS_NAME);
		cmd.append(" --resource-group ");
		cmd.append(AZURE_RG_SERVERLESS_EUWEST);
		cmd.append(" --settings \"AzureCosmosDBConnection=AccountEndpoint=");
		cmd.append(account.documentEndpoint());
		cmd.append(";AccountKey=");
		cmd.append(account.listKeys().primaryMasterKey());
		cmd.append("\";");
		synchronized(AzureManagement.class) {
			Files.write(Paths.get("set-app-config.sh"), cmd.toString().getBytes(),
				StandardOpenOption.APPEND);
		}
	}

	public static AsyncDocumentClient getDocumentClient(CosmosDBAccount account) {
		ConnectionPolicy connectionPolicy = ConnectionPolicy.GetDefault();
		// connectionPolicy.setConnectionMode(ConnectionMode.Direct);
		AsyncDocumentClient client = new AsyncDocumentClient.Builder().withServiceEndpoint(account.documentEndpoint())
				.withMasterKeyOrResourceToken(account.listKeys().primaryMasterKey())
				.withConnectionPolicy(connectionPolicy).withConsistencyLevel(ConsistencyLevel.Session).build();
		System.out.println("CosmosDB client created with success: name = " + account.name());
		return client;
	}

	/**
	 * Returns the string to access a CosmosDB database
	 *
	 *            Name of collection
	 * @return
	 */
	static String getDatabaseString(String dbname) {
		return String.format("/dbs/%s", dbname);
	}

	/**
	 * Returns the string to access a CosmosDB collection names col
	 * 
	 * @param col
	 *            Name of collection
	 * @return
	 */
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
				partitionKeyDef.setPaths(Arrays.asList(partKeys.split(",")));
				collectionDefinition.setPartitionKey(partitionKeyDef);

				if (uniqueKeys != null) {
					UniqueKeyPolicy uniqueKeyDef = new UniqueKeyPolicy();
					UniqueKey uniqueKey = new UniqueKey();
					uniqueKey.setPaths(Arrays.asList(uniqueKeys.split(",")));
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

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////// COSMOS DB CODE
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	public static RedisCache createRedis(Azure azure, String rgName, String name, Region region) {
		Creatable<RedisCache> redisCacheDefinition = azure.redisCaches().define(name).withRegion(region)
				.withNewResourceGroup(rgName).withBasicSku(0);

		return azure.redisCaches().create(redisCacheDefinition).get(redisCacheDefinition.key());
	}

	public synchronized static void dumpRedisCacheInfo(String propFilename, RedisCache cache) throws IOException {
		RedisAccessKeys redisAccessKey = cache.regenerateKey(RedisKeyType.PRIMARY);
		Files.write(Paths.get(propFilename),
				("REDIS_KEY=" + redisAccessKey.primaryKey() + "\n").getBytes(),
				StandardOpenOption.APPEND);
		Files.write(Paths.get(propFilename), ("REDIS_URL=" + cache.hostName() + "\n").getBytes(),
				StandardOpenOption.APPEND);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////// AZURE DELETE CODE
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static void deleteResourceGroup(Azure azure, String rgName) {
		azure.resourceGroups().deleteByName(rgName);
	}

	public static void main(String[] args) {
		try {
			if (args.length == 1 && args[0].equalsIgnoreCase("-delete")) {
				Azure azure = cretaeManagementClient(AZURE_AUTH_LOCATION);
				deleteResourceGroup(azure, AZURE_RG_EUWEST);
				deleteResourceGroup(azure, AZURE_RG_SERVERLESS_EUWEST);
			} else {
				Azure azure0 = cretaeManagementClient(AZURE_AUTH_LOCATION);
				createResourceGroup( azure0, AZURE_RG_EUWEST, Region.EUROPE_WEST);
				createResourceGroup( azure0, AZURE_RG_SERVERLESS_EUWEST, Region.EUROPE_WEST);
				Files.deleteIfExists(Paths.get(AZURE_PROPS_LOCATION));
				
				if(!Files.exists(Paths.get(AZURE_PROPS_LOCATION)))
					Files.createFile(Paths.get(AZURE_PROPS_LOCATION));
				
				Files.write(Paths.get(AZURE_PROPS_LOCATION), 
							("# Date : " + new SimpleDateFormat().format(new Date()) + "\n").getBytes(),
							StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				//Files.deleteIfExists(Paths.get(AZURE_PROPS_LOCATION));
				Files.deleteIfExists(Paths.get("set-app-config.sh"));
				Files.write(Paths.get("set-app-config.sh"), "".getBytes(),
						StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				new Thread(() -> {
					try {
						Azure azure = cretaeManagementClient(AZURE_AUTH_LOCATION);
						StorageAccount accountStorage = createStorageAccount(azure, AZURE_RG_EUWEST, AZURE_STORAGE_NAME,
								Region.EUROPE_WEST);
						dumpStorageKey(AZURE_PROPS_LOCATION, accountStorage);
						createBlobContainer(azure, AZURE_RG_EUWEST, AZURE_STORAGE_NAME, AZURE_BLOB_MEDIA);
					} catch (Exception e) {
						System.err.println("Error while creating storage resources");
						e.printStackTrace();
					}
				}).start();

				new Thread(() -> {
					try {
						Azure azure = cretaeManagementClient(AZURE_AUTH_LOCATION);
						CosmosDBAccount accountCosmosDB = createCosmosDBAccount(azure, AZURE_RG_EUWEST,
								AZURE_COSMOSDB_NAME, Region.EUROPE_WEST, null);
						dumpCosmosDBKey(AZURE_PROPS_LOCATION, accountCosmosDB);
						AsyncDocumentClient cosmosClient = getDocumentClient(accountCosmosDB);
						createDatabase(cosmosClient, AZURE_COSMOSDB_DATABASE);
						createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, "Users", "/name", "/name");
						createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, "Posts", "/community", null);
						createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, "Communities", "/name", "/name");
						createCollection(cosmosClient, AZURE_COSMOSDB_DATABASE, "Likes", "/post_id", "/post_id,/user_names");
					} catch (Exception e) {
						System.err.println("Error while creating cosmosdb resources");
						e.printStackTrace();
					}
				}).start();
				
				new Thread(() -> {
					try {
						Azure azure = cretaeManagementClient(AZURE_AUTH_LOCATION);
						RedisCache cache = createRedis(azure, AZURE_RG_EUWEST, AZURE_REDIS_NAME, Region.EUROPE_WEST);
						dumpRedisCacheInfo(AZURE_PROPS_LOCATION, cache);
					} catch (Exception e) {
						System.err.println("Error while creating redis resources");
						e.printStackTrace();
					}
				}).start();
			}
		} catch (Exception e) {
			System.err.println("Error while creating resources");
			e.printStackTrace();
		}
	}

}
