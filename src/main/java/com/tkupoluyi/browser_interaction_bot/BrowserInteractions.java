package com.tkupoluyi.browser_interaction_bot;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;

public class BrowserInteractions {
    public static String openPage(ChromeDriver driver, String url) {
        try {
            driver.get(url);
            waitForPageLoad(driver);
        } catch (UnhandledAlertException ignored) { }

        return driver.getCurrentUrl(); // This sets url to what it is when page has loaded
    }

    public static void waitForPageLoad(ChromeDriver driver) {
        String loadState = driver.executeScript("return document.readyState").toString();
        while (!loadState.equals("complete")) {
            loadState = driver.executeScript("return document.readyState").toString();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void scrollToTop(ChromeDriver driver) {
        driver.executeScript("window.scrollTo(0, 0)");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void closeExtraneousTabs(ChromeDriver driver, int limit) {
        if (driver.getWindowHandles().size() < limit) {
            return;
        }
        else {
            try {
                String originalHandle = driver.getWindowHandle();
                for (String handle : driver.getWindowHandles()) {
                    if (!handle.equals(originalHandle)) {
                        driver.switchTo().window(handle);
                        driver.close();
                    }
                }
                driver.switchTo().window(originalHandle);
            } catch (NoSuchWindowException ex) {
                System.out.println("Window already closed");
            }
        }
    }

    public static void screenshot(ChromeDriver driver, String fileName) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) { }
        File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(scrFile, new File(fileName+ ".png"));
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            System.out.println("Error in screenshot");
        }
    }
}
