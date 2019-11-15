package scc.storage;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.List;

public class Redis {

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

    //Basic KV operations
    public static void putInList(String key, String[] jsonRepresentations) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.lpush(key, jsonRepresentations);
        }
    }

    public static List<String> getList(String key, int pageSize){
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.lrange(key, 0, pageSize);
        }
    }

    public static String get(String key){
        return getList(key, 1).get(0);
    }

    public static void set(String key, String jsonRepresentation){

    }

    //HyperLogLog operations
    public static void increment(String counterId){
        try (Jedis jedis = jedisPool.getResource()){
            jedis.pfadd(counterId, "plusOne");
        }
    }

    public static long getProbabilisticCount(String counterId){
        try( Jedis jedis = jedisPool.getResource()){
            return jedis.pfcount(counterId);
        }

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
