package scc.storage;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Redis {

	private static final boolean ACTIVE = true;
	private static final int TOP_LIMIT = 5;

	private static JedisPool jedisPool;

	public Redis(){ }

	static void init(String redisHostName, String password){
		jedisPool = new JedisPool(getJedisPoolConfig(), redisHostName, 6380, 1000, password,true);
	}

	private static JedisPoolConfig getJedisPoolConfig() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(128);
		poolConfig.setMaxIdle(128);
		poolConfig.setMinIdle(16);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		poolConfig.setMinEvictableIdleTimeMillis(Duration. ofSeconds(60).toMillis());
		poolConfig.setTimeBetweenEvictionRunsMillis(Duration. ofSeconds(30).toMillis());
		poolConfig.setNumTestsPerEvictionRun(3);
		poolConfig.setBlockWhenExhausted(true);
		return poolConfig;
	}

	public interface Operation {
		public Object execute(Jedis jedis);
	}

	@SuppressWarnings("unchecked")
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
	
	public static addToBoundedDictionary(String dictionary_key, int max_size, String key, String value) {
		executeOperation(jedis -> {
			remover
			if not in, add to hashMap
			
			lpush
			if count > max
			   remover cauda/fazertrim & remover do cicionario
			
			boolean isNew = (jedis.hsetnx(dictionary_key, key, value) == 1);
			jedis.zincrby(dictionary_key, 1, key;
			
			if(isNew) {
			    int count = jedis.hlen(dictionary_key);	
			    if(count > max_size) {
			    	// remover o pior do sorted set e removê-lo também do mapa
			    
			    }
			}
			
			return null;
			});
	}
	
	/*
    public static void putRaw(String key, byte[] data){
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key.getBytes(), data);
        }
    }

    public static byte[] getRaw(String key){
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key.getBytes());
        }
    }

    public static void set(String key, String jsonRepresentation){
        putInList(key, new String[]{jsonRepresentation});
    }
	 */
}
