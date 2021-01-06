package com.tkupoluyi.selenium_browser_test;
public class MainExecution {
    public static void main(String[] args) {
        for (int i=0; i< args.length; i++) {System.out.println(args[i]);}
        if (args.length <= 1) {
            ChromeExecution execution = new ChromeExecution("http://colorado.edu");
            execution.execute();
        } else if (args.length <= 2){
            ChromeExecution execution = new ChromeExecution(args[0], args[1]);
            execution.execute();
        } else {
            ChromeExecution execution = new ChromeExecution(args[0], args[1], args[2]);
            execution.execute();
        }
        System.exit(0);
    }

}
