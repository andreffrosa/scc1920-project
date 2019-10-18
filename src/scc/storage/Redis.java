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

    public static void init(String redisHostName, String password){
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

    public static void putInList(String key, String[] jsonRepresentations){
        try (Jedis jedis = jedisPool.getResource()) {
            Long cnt = jedis.lpush(key, jsonRepresentations);
            if (cnt > 5)
                jedis.ltrim(key, 0, TOP_LIMIT);
        }
    }

    public static List<String> getList(String key){
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.lrange(key, 0, TOP_LIMIT);
        }
    }

    public static void set(String key, String jsonRepresentation){
        putInList(key, new String[]{jsonRepresentation});
    }

    public static String get(String key){
        return getList(key).get(0);
    }
}
