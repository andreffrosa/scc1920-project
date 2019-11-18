package scc.storage;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

public class Redis {

	// TODO: Ler isto de um ficheiro de configs? Não sei se vale a pena
	/**Acho que vale*/
	public static final boolean ACTIVE = true;
	public static final String TOP_POSTS = "top_posts";
	public static final int TOP_POSTS_LIMIT = 200;
	public static final String TOTAL_LIKES = "total_likes";
	public static final int TOTAL_LIKES_LIMIT = 200;
	public static final String DAYLY_LIKES = "dayly_likes";
	public static final int DAYLY_LIKES_LIMIT = 200;
	public static final String TOTAL_REPLIES = "total_replies";
	public static final int TOTAL_REPLIES_LIMIT = 200;
	public static final String DAYLY_REPLIES = "dayly_replies";
	public static final int DAYLY_REPLIES_LIMIT = 200;
	public static final String TOP_USERS = "top_users";
	public static final int TOP_USERS_LIMIT = 30;
	public static final String TOP_COMMUNITIES = "top_communities";
	public static final int TOP_COMMUNITIES_LIMIT = 5;
	public static final String TOP_IMAGES = "top_images";
	public static final int TOP_IMAGES_LIMIT = 5;

	private static JedisPool jedisPool;

	//public Redis(){ }

	static void init(String redisHostName, String password) {
		jedisPool = new JedisPool(getJedisPoolConfig(), redisHostName, 6380, 1000, password, true);
	}

	private static JedisPoolConfig getJedisPoolConfig() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(128);
		poolConfig.setMaxIdle(128);
		poolConfig.setMinIdle(16);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
		poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
		poolConfig.setNumTestsPerEvictionRun(3);
		poolConfig.setBlockWhenExhausted(true);
		return poolConfig;
	}

	public interface Operation {
		public Object execute(Jedis jedis);
	}

	private static Object executeOperation(Operation op) {
		Object result = null;
		if(ACTIVE) {
			try (Jedis jedis = jedisPool.getResource()) {
				result = op.execute(jedis);
			}
		}
		return result;
	}
	
	// Strings
	public static String getKey(String key) {
		return (String) executeOperation(jedis -> jedis.get(key));
	}

	public static void setKey(String key, String value){
		executeOperation(jedis -> jedis.set(key, value));
	}

	// List
	public static void putInList(String key, String... values) {
		executeOperation(jedis -> jedis.lpush(key, values));
	}

	@SuppressWarnings("unchecked")
	public static List<String> getList(String key, int pageSize){ // TODO: Para que é o size?
		return (List<String>) executeOperation(jedis -> jedis.lrange(key, 0, pageSize));
	}
	
	public static void putInBoundedList(String key, int max_size, String... values) {
		executeOperation(jedis -> {
			Long count = jedis.lpush(key, values);
			if(count > max_size)
				jedis.ltrim(key, 0, max_size);
			
			return null;
			});
	}

	//HyperLogLog operations
	public static void addToHyperLog(String hyperlog_id, String value){
		executeOperation(jedis -> jedis.pfadd(hyperlog_id, value));
	}

	public static Long getProbabilisticCount(String hyperlog_id) {
		return (Long) executeOperation(jedis -> jedis.pfcount(hyperlog_id));
	}
	
	// Dictionary
	public static void addToDictionary(String dictionary_key, String key, String value) {
		executeOperation(jedis -> jedis.hset(dictionary_key, key, value));
	}

	public static String getFromDictionary(String dictionary_key, String key) {
		return (String) executeOperation(jedis -> jedis.hget(dictionary_key, key));
	}
	
	// LRU Set
	
	public interface PutOperation {
		public void execute(Jedis jedis, Transaction tx, String set_key, String item_key);
	}
	
	@SuppressWarnings("unchecked")
	public static void LRUSetPut(String set_key, int max_size, String item_key, PutOperation on_insert, PutOperation on_remove) {
		executeOperation(jedis -> {
			long score = System.currentTimeMillis();
			
			Transaction tx = jedis.multi();
			tx.zadd("zset:" + set_key, score, item_key);
			tx.zrange("zset:", 0, 0);
			tx.zcount("zset:", Double.MIN_VALUE, Double.MAX_VALUE);
			on_insert.execute(jedis, tx, set_key, item_key);
			List<Object> results = tx.exec();
			
			String lowest_id = ( (Set<String>) results.get(1) ).iterator().next();
			int current_size = (int)((Long)results.get(2)).longValue();
			
			if(current_size > max_size) {
				tx = jedis.multi();
				tx.zrem("zset:" + set_key, lowest_id);
				on_remove.execute(jedis, tx, set_key, lowest_id);
				results = tx.exec();
			}
			
			return null;
			});
	}

	public static Object LRUSetGet(String set_key, String item_key, Operation op) {
		return executeOperation(jedis -> {

			Object value = op.execute(jedis);
			
			if(value != null) {
				long score = System.currentTimeMillis();
				jedis.zadd("zset:" + set_key, score, item_key);
			}
			
			return value;
			});
	}
	
	public interface LRUMapOP {
		public Object execute(Jedis jedis, Transaction tx, String set_key, String item_key);
	}
	
	@SuppressWarnings("unchecked")
	public static List<Object> LRUSetMap(String set_key, LRUMapOP op) {
		return (List<Object>) executeOperation(jedis -> {

			Set<String> item_keys = jedis.zrange("zset:" + set_key, Long.MIN_VALUE, Long.MAX_VALUE);
			
			Transaction tx = jedis.multi();
			
			for( String item_key : item_keys ) {
				op.execute(jedis, tx, set_key, item_key);
			}
			List<Object> results = tx.exec();
			
			return results;
			});
	}
	
	public static void LRUDictionaryPut(String set_key, int max_size, String item_key, String value) {
		LRUSetPut(set_key, max_size, item_key, 
				(Jedis jedis, Transaction tx, String set_key_, String item_key_) -> tx.hset("h:" + set_key, item_key, value), 
				(Jedis jedis, Transaction tx, String set_key_, String lowest_id) -> tx.hdel("h:" + set_key, lowest_id));
	}
	
	public static String LRUDictionaryGet(String set_key, String item_key) {
		return (String) LRUSetGet(set_key, item_key, (Jedis jedis) -> jedis.hget("h:" + set_key, item_key) );
	}
	
	public static void LRUHyperLogPut(String set_key, int max_size, String item_key, String value) {
		LRUSetPut(set_key, max_size, item_key, 
				(Jedis jedis, Transaction tx, String set_key_, String item_key_) -> tx.pfadd("pf:" + set_key + ":" + item_key, value), 
				(Jedis jedis, Transaction tx, String set_key_, String lowest_id) -> tx.del("pf:" + set_key + ":" + item_key, value));
	}
	
	public static Long LRUHyperLogGet(String set_key, String item_key) {
		return (Long) LRUSetGet(set_key, item_key, (Jedis jedis) -> jedis.pfcount("pf:" + set_key + ":" + item_key) );
	}
}
