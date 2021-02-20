package com.tkupoluyi.selenium_browser_test;
public class MainExecution {
    public static void main(String[] args) {
        for (int i=0; i< args.length; i++) {System.out.println(args[i]);}
        if (args.length <= 1) {
            ChromeExecution forwardExecution = new ChromeExecution("https://edition.cnn.com");
            ChromeExecution backwardExecution = new ChromeExecution("https://edition.cnn.com");
            backwardExecution.setBackward();

            forwardExecution.start();
            backwardExecution.start();
            try {
                forwardExecution.join();
                backwardExecution.join();
            } catch(InterruptedException e) {

            } finally {
                System.out.println("Complete");
            }

        } else if (args.length <= 2){
            ChromeExecution forwardExecution = new ChromeExecution(args[0], args[1]);
            ChromeExecution backwardExecution = new ChromeExecution(args[0], args[1]);
            backwardExecution.setBackward();

            forwardExecution.start();
            backwardExecution.start();
            try {
                forwardExecution.join();
                backwardExecution.join();
            } catch(InterruptedException e) {

            } finally {
                System.out.println("Complete");
            }
        } else if (args.length <= 3) {
            ChromeExecution forwardExecution = new ChromeExecution(args[0], args[1], args[2]);
            ChromeExecution backwardExecution = new ChromeExecution(args[0], args[1], args[2]);
            backwardExecution.setBackward();

            forwardExecution.start();
            backwardExecution.start();
            try {
                forwardExecution.join();
                backwardExecution.join();
            } catch(InterruptedException e) {

            } finally {
                System.out.println("Complete");
            }
        } else if (args.length <= 4) {
            ChromeExecution forwardExecution = new ChromeExecution(args[0], args[1], args[2], args[3]);
            ChromeExecution backwardExecution = new ChromeExecution(args[0], args[1], args[2], args[3]);
            backwardExecution.setBackward();

            forwardExecution.start();
            backwardExecution.start();
            try {
                forwardExecution.join();
                backwardExecution.join();
            } catch(InterruptedException e) {

            } finally {
                System.out.println("Complete");
            }
        }
        System.exit(0);
    }

}
