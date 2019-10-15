package scc.config;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {

    public  @Context ServletContext context;
    private static final int PROJECT_ENOUGH_SIZE = 2;
    private static Map<String,Config> configs = new HashMap<>(PROJECT_ENOUGH_SIZE);

    private Properties props;

    private Config(String path) throws IOException {
        InputStream is = context.getResourceAsStream(path);
        props = new Properties();
        props.load(is);
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
