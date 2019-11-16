package scc.storage;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.List;

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

	//Basic KV operations
	public static void putInList(String key, String[] jsonRepresentations) {
		executeOperation(jedis -> jedis.lpush(key, jsonRepresentations));
	}

	@SuppressWarnings("unchecked")
	public static List<String> getList(String key, int pageSize){
		return (List<String>) executeOperation(jedis -> jedis.lrange(key, 0, pageSize));
	}

	public static String get(String key){
		return getList(key, 1).get(0);
	}

	public static void set(String key, String jsonRepresentation){
		executeOperation(jedis -> jedis.set(key, jsonRepresentation));
	}

	//HyperLogLog operations
	public static void increment(String counterId){
		executeOperation(jedis -> jedis.pfadd(counterId, "plusOne"));
	}

	public static Long getProbabilisticCount(String counterId) {
		return (Long) executeOperation(jedis -> jedis.pfcount(counterId));
	}

	public static void decrement(String counterId) {
		// TODO Auto-generated method stub
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
