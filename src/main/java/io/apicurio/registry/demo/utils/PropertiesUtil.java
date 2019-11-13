package io.apicurio.registry.demo.utils;

import java.util.Properties;

/**
 * @author Ales Justin
 */
public class PropertiesUtil {
    public static Properties properties(String[] args) {
        Properties properties = new Properties(System.getProperties());
        for (String arg : args) {
            if (arg.contains("=")) {
                String[] split = arg.split("=");
                properties.put(split[0], split[1]);
            }
        }
        return properties;
    }

    public static String property(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            value = System.getenv(key.toUpperCase().replace(".", "_"));
            if (value == null) {
                value = defaultValue;
            }
        }
        return value;
    }
}
