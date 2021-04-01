package com.tkupoluyi.selenium_browser_test;

import java.io.FileOutputStream;
import java.io.IOException;

public class MainExecution {
    public static void main(String[] args) throws IOException {
        ChromeExecution forwardExecution = null;
        ChromeExecution backwardExecution = null;

        try {
            if (args.length <= 1) {
                forwardExecution = new ChromeExecution("https://adf.ly/");
                backwardExecution = new ChromeExecution("https://adf.ly/");
                backwardExecution.setBackward();
            } else if (args.length <= 2) {
                forwardExecution = new ChromeExecution(args[0], args[1]);
                backwardExecution = new ChromeExecution(args[0], args[1]);
                backwardExecution.setBackward();
            } else if (args.length <= 3) {
                (new FileOutputStream(args[1])).close();
                forwardExecution = new ChromeExecution(args[0], args[1], args[2]);
                backwardExecution = new ChromeExecution(args[0], args[1], args[2]);
                backwardExecution.setBackward();
            } else {
                (new FileOutputStream(args[1])).close();
                forwardExecution = new ChromeExecution(args[0], args[1], args[2], args[3]);
                backwardExecution = new ChromeExecution(args[0], args[1], args[2], args[3]);
                backwardExecution.setBackward();
            }
        } catch (Exception ex) {
            System.out.println("There was an error creating chrome execution");
            System.exit(9);
        }

        try {
            forwardExecution.start();
            backwardExecution.start();
            forwardExecution.join();
            backwardExecution.join();
        } catch(InterruptedException ex) {
            System.out.println("Thread interrupted with error: "+ ex.getClass());
        } catch(Exception ex) {
            if (forwardExecution != null) {
                forwardExecution.interrupt();
                forwardExecution.isError = true;
            }
            if (backwardExecution != null) {
                backwardExecution.interrupt();
                backwardExecution.isError = true;
            }
        } finally {
            if ((forwardExecution != null && forwardExecution.isTimeout) || (backwardExecution != null && backwardExecution.isTimeout)) {
                System.exit(11);
            }
            if ((forwardExecution != null && forwardExecution.isError) || (backwardExecution != null && backwardExecution.isError)) {
                System.exit(9);
            }
            System.out.println("Complete");
        }
        System.exit(1);
    }

}
