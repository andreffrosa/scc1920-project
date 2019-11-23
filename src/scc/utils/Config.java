package scc.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scc.storage.BlobStorageClient;
import scc.storage.CosmosClient;
import scc.storage.Redis;

public class Config {

	// AZURE
	public static final String BLOB_STORAGE_CONNECTION_STRING = "BLOB_KEY";
	public static final String COSMOS_DB_MASTER_KEY = "COSMOSDB_KEY";
	public static final String COSMOS_DB_ENDPOINT = "COSMOSDB_URL";
	public static final String COSMOSDB_DATABASE = "COSMOSDB_DATABASE";
	public static final String REDIS_HOST_NAME = "REDIS_URL";
	public static final String CACHE_KEY = "REDIS_KEY";

	public static final String POSTS_CONTAINER = "Posts";
	public static final String LIKES_CONTAINER = "Likes";
	public static final String COMMUNITIES_CONTAINER = "Communities";
	public static final String USERS_CONTAINER = "Users";
	public static final String IMAGES_CONTAINER = "images";

	// REDIS
	public static final String INITIAL_PAGE = "initial_page";
	public static final String TOP_POSTS = "top_posts";
	public static final String TOP_POSTS_LIMIT = "TOP_POSTS_LIMIT";
	public static final String TOP_REPLIES = "top_posts";
	public static final String TOP_REPLIES_LIMIT = "TOP_REPLIES_LIMIT";
	public static final String TOTAL_LIKES = "total_likes";
	public static final String TOTAL_LIKES_LIMIT = "TOTAL_LIKES_LIMIT";
	public static final String DAYLY_LIKES = "dayly_likes";
	public static final String DAYLY_LIKES_LIMIT = "DAYLY_LIKES_LIMIT";
	public static final String TOTAL_REPLIES = "total_replies";
	public static final String TOTAL_REPLIES_LIMIT = "TOTAL_REPLIES_LIMIT";
	public static final String DAYLY_REPLIES = "dayly_replies";
	public static final String DAYLY_REPLIES_LIMIT = "DAYLY_REPLIES_LIMIT";
	public static final String TOP_USERS = "top_users";
	public static final String TOP_USERS_LIMIT = "TOP_USERS_LIMIT";
	public static final String TOP_COMMUNITIES = "top_communities";
	public static final String TOP_COMMUNITIES_LIMIT = "TOP_COMMUNITIES_LIMIT";
	public static final String TOP_IMAGES = "top_images";
	public static final String TOP_IMAGES_LIMIT = "TOP_IMAGES_LIMIT";

	// SYSTEM
	public static final String DEFAULT_REPLIES_PAGE_SIZE = "DEFAULT_REPLIES_PAGE_SIZE";
	public static final String DEFAULT_DEPTH = "DEFAULT_LEVEL";//3;
	public static final String MAX_INITIAL_PAGE_POSTS = "MAX_INITIAL_PAGE_POSTS";
	public static final String DEFAULT_INITIAL_PAGE_SIZE = "DEFAULT_INITIAL_PAGE_SIZE";//10;
	public static final String DEFAULT_INITIAL_PAGE_NUMBER = "DEFAULT_INITIAL_PAGE_NUMBER"; //1;

	// SEARCH
	public static final String SEARCH_SERVICE_NAME = "SearchServiceName";
	public static final String SEARCH_ADMIN_KEY = "SearchServiceAdminKey";
	public static final String SEARCH_QUERY_KEY = "SearchServiceQueryKey";
	public static final String SEARCH_INDEX = "cosmosdb-index";
	
	// FILES
	public static final String AZURE_PROPS_FILE = "azurekeys.props";
	public static final String REDIS_PROPS_FILE = "redis.props";
	public static final String SYSTEM_PROPS_FILE = "system.props";
	public static final String SEARCH_PROPS_FILE = "azure-search.props";

	private static Properties azureProperties, redisProperties, systemProperties, searchProperties;

	public enum PropType {
		AZURE, REDIS, SYSTEM, SEARCH
	};

	static Logger logger = LoggerFactory.getLogger(Config.class);
	
	public static synchronized void load() throws IOException {
		getProperties(PropType.AZURE);
		CosmosClient.init(azureProperties.getProperty(COSMOSDB_DATABASE), azureProperties.getProperty(COSMOS_DB_MASTER_KEY), azureProperties.getProperty(COSMOS_DB_ENDPOINT));
		BlobStorageClient.init(azureProperties.getProperty(BLOB_STORAGE_CONNECTION_STRING));
		Redis.init(azureProperties.getProperty(REDIS_HOST_NAME), azureProperties.getProperty(CACHE_KEY));

		getProperties(PropType.REDIS);
		getProperties(PropType.SYSTEM);
		
		logger.info("Loaded configuration files!");
	}

	public static synchronized Properties getProperties(PropType type) throws IOException {
		Properties to_return = null;
		if(type == PropType.AZURE) {
			if( azureProperties == null || azureProperties.size() == 0) {
				azureProperties = new Properties();
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(AZURE_PROPS_FILE);
				azureProperties.load(is);
			}
			to_return = azureProperties;	
		} else if(type == PropType.REDIS) {
			if( redisProperties == null || redisProperties.size() == 0) {
				redisProperties = new Properties();
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(REDIS_PROPS_FILE);
				redisProperties.load(is);
			}
			to_return = redisProperties;
		} else if(type == PropType.SYSTEM) {
			if( systemProperties == null || systemProperties.size() == 0) {
				systemProperties = new Properties();
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(SYSTEM_PROPS_FILE);
				systemProperties.load(is);
			}
			to_return = systemProperties;
		} else if(type == PropType.SEARCH) {
			if( searchProperties == null || searchProperties.size() == 0) {
				searchProperties = new Properties();
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(SEARCH_PROPS_FILE);
				searchProperties.load(is);
			}
			to_return = searchProperties;
		} else
			logger.error("Unrecognized properties type");

		return to_return;
	}

	public static synchronized String getAzureProperty(String key) {
		try {
			return Config.getProperties(PropType.AZURE).getProperty(key);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static synchronized String getRedisProperty(String key) {
		try {
			return Config.getProperties(PropType.REDIS).getProperty(key);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static synchronized String getSystemProperty(String key) {
		try {
			return Config.getProperties(PropType.SYSTEM).getProperty(key);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static synchronized String getSearchProperty(String key) {
		try {
			return Config.getProperties(PropType.SEARCH).getProperty(key);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}


