package base;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import utils.DriverManager;

public class BaseTest {
    @BeforeSuite
    public void setUp() throws Exception {
        // Initialize the Appium driver
        DriverManager.initializeDriver();
    }

    @Test
    public void test() {
        // Your test code here
    }

    @AfterSuite
    public void tearDown() {
        // Quit the Appium driver
        DriverManager.quitDriver();
    }
}