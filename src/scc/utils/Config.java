package scc.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {

    private static final int PROJECT_ENOUGH_SIZE = 2;
    private static Map<String,Config> configs = new HashMap<>(PROJECT_ENOUGH_SIZE);

    private Properties props;

    private Config(String path) throws IOException {
        InputStream input = new FileInputStream(path);
        props = new Properties();
        props.load(input);
    }

    public static Config getInstance(String path) throws IOException {
        if (configs.containsKey(path))
            return configs.get(path);
        else {
            Config config = new Config(path);
            configs.put(path, config);
            return config;
        }
    }

    public Properties getProperties(){
        return props;
    }
}
