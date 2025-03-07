package utils;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Properties;

public class DriverManager {

    private static AppiumDriver driver;
    private static final Properties properties = ConfigReader.loadProperties();

    /**
     * Initializes the Appium driver based on platform and device type.
     * Ensures only one driver instance is active at a time.
     *
     * @throws Exception if driver initialization fails
     */
    public static void initializeDriver() throws Exception {
        if (driver != null) {
            return; // Prevents multiple driver instances
        }

        URI appiumServerURI = new URI(properties.getProperty("appium.server.url"));
        URL appiumServerURL = appiumServerURI.toURL();
        String platformName = properties.getProperty("platform.name");

        switch (platformName.toLowerCase()) {
        case "android":
            if (properties.getProperty("android.device.type").equalsIgnoreCase("emulator")) {
                startEmulator(properties.getProperty("android.emulator.name"));
                while (!isEmulatorBooted()) {
                    System.out.println("Waiting for emulator to boot...");
                    Thread.sleep(5000);
                }
                System.out.println("Emulator is fully booted.");
            }
            driver = new AndroidDriver(appiumServerURL, getAndroidOptions());
            break;

        case "ios":
            driver = new AppiumDriver(appiumServerURL, getIosOptions());
            break;

        default:
            throw new RuntimeException("Unsupported platform: " + platformName);
    }
}

    /**
     * Configures and returns Android-specific options for Appium.
     *
     * @return UiAutomator2Options configured for Android testing
     */
    private static UiAutomator2Options getAndroidOptions() {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setAutomationName("UiAutomator2")
               .setPlatformName(properties.getProperty("platform.name"))
               .setIgnoreHiddenApiPolicyError(true)
               .setDeviceName(properties.getProperty("android.device.name").equalsIgnoreCase("emulator")
                             ? properties.getProperty("android.emulator.name")
                             : properties.getProperty("android.real.name"))
               .setAppPackage(properties.getProperty("app.package"))
               .setAppWaitActivity(properties.getProperty("app.activity"))
               .setNoReset(true)  // Keep app data and do not uninstall
               .setCapability("dontStopAppOnReset", true);  // Keeping background tasks running in the app

        // If the app is not installed, provide the APK path to install it
        if (!isAppInstalled(properties.getProperty("app.package"))) {
            options.setApp(properties.getProperty("android.app.path")); // Install only if not installed
        }

        if (properties.getProperty("android.device.type").equalsIgnoreCase("emulator")) {
            options.setAvd(properties.getProperty("android.emulator.name"))
                   .setAvdLaunchTimeout(Duration.ofSeconds(180))
                   .setAvdReadyTimeout(Duration.ofSeconds(60));
        }

        if (properties.getProperty("android.device.type").equalsIgnoreCase("real")) {
            options.setUdid(properties.getProperty("android.device.udid"));
        }

        return options;
    }
    
    
    private static XCUITestOptions getIosOptions() {
    	
    	XCUITestOptions options = new XCUITestOptions();
    	options.setAutomationName("XCUITest")
    			.setPlatformName(properties.getProperty("platform.name"))
    			.setDeviceName(properties.getProperty("ios.device.name").equalsIgnoreCase("simulator")
    					?properties.getProperty("ios.simulator.nam")
    					:properties.getProperty("ios.device.name"))
    			.setApp(properties.getProperty("ios.app.path"));
    	
    	if (properties.getProperty("ios.device.type").equalsIgnoreCase("real")) {
            options.setUdid(properties.getProperty("ios.device.udid"));
        }
    	
		return options;
    	
    }



    /**
     * Checks whether the Android emulator has completed booting.
     *
     * @return true if the emulator is fully booted, false otherwise
     */
    private static boolean isEmulatorBooted() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("adb", "shell", "getprop", "sys.boot_completed");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            return "1".equals(output);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Starts an Android emulator in the background.
     *
     * @param emulatorName the name of the emulator to start
     */
    private static void startEmulator(String emulatorName) {
        if (emulatorName == null || emulatorName.isEmpty()) {
            throw new RuntimeException("Emulator name is missing in configuration.");
        }

        try {
            System.out.println("Starting emulator: " + emulatorName);
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "start emulator -avd " + emulatorName);
            processBuilder.redirectErrorStream(true);
            processBuilder.start();
        } catch (IOException e) {
            System.out.println("Failed to start emulator: " + e.getMessage());
        }
    }

    
    /**
     * Checks whether a specific app is installed on the connected device.
     *
     * @param packageName The package name of the app to check.
     * @return true if the app is installed, false otherwise.
     */
    private static boolean isAppInstalled(String packageName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("adb", "shell", "pm", "list", "packages", packageName);
            //can also use adb shell pm list packages | findstr packageName
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output;
            while ((output = reader.readLine()) != null) {
                if (output.contains(packageName)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Error checking if app is installed: " + e.getMessage());
        }
        return false;
    }


    /**
     * Retrieves the current Appium driver instance.
     *
     * @return the active AppiumDriver instance
     * @throws RuntimeException if the driver is not initialized
     */
    public static AppiumDriver getDriver() {
        if (driver == null) {
            throw new RuntimeException("Driver is not initialized. Call initializeDriver() first.");
        }
        return driver;
    }

    /**
     * Quits the Appium driver and releases resources.
     */
    public static void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
}
