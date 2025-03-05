package utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static Properties properties;

    static {
        loadProperties(); // Ensures properties are loaded at class initialization
    }

    public static Properties loadProperties() {
        properties = new Properties();
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException("Unable to find config.properties in the classpath.");
            }
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error loading config.properties: " + e.getMessage());
            throw new RuntimeException("Failed to load configuration file.", e);
        }
        return properties;
    }

    public static String getProperty(String key) {
        if (properties == null) {
            throw new IllegalStateException("Properties not initialized. Ensure loadProperties() is called.");
        }
        String value = properties.getProperty(key);
        if (value != null && (key.contains("path") || key.contains("directory"))) {
            return value.replace("/", File.separator).replace("\\", File.separator);
        }
        return value;
    }
}