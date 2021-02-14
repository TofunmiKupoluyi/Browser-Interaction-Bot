package com.tkupoluyi.selenium_browser_test;
public class MainExecution {
    public static void main(String[] args) {

        ChromeExecution execution = new ChromeExecution("http://adf.ly");
        execution.runForward();
        execution.runBackward();

        System.exit(0);
    }

}
