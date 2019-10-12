package scc.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    public static Properties loadConfig(String path) throws IOException {

        InputStream input = new FileInputStream(path);

        Properties props = new Properties();
        // load a properties file
        props.load(input);
        return props;
    }

}
