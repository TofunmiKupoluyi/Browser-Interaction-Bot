package com.tkupoluyi.browser_interaction_bot;

import com.tkupoluyi.browser_interaction_bot.event_handling.DefaultEventHandler;
import com.tkupoluyi.browser_interaction_bot.exceptions.InteractionBotException;

import java.io.IOException;

public class MainExecution {
    public static void main(String[] args) throws IOException, InteractionBotException {
        ChromeExecution execution = null;

        try {
            if (args.length <= 0) {
                execution = new ChromeExecution("https://colorado.edu/", new DefaultEventHandler());
            } else if (args.length <= 1) {
                execution = new ChromeExecution(args[0], new DefaultEventHandler());
            } else if (args.length <= 2) {
                execution = new ChromeExecution(args[0], new DefaultEventHandler(), args[1]);
            } else if (args.length <= 3) {
                execution = new ChromeExecution(args[0], new DefaultEventHandler(), args[1], args[2]);
            } else {
                execution = new ChromeExecutionFromTrace(args[0], new DefaultEventHandler(), args[1], args[2], args[3]);
            }
        } catch (Exception ex) {
            System.out.println("There was an error creating chrome execution");
            System.out.println(ex);
            System.exit(9);
        }

        try {
            execution.execute();
        } catch (InteractionBotException ex) {
            System.out.println(ex.getExceptionType());
            System.exit(9);
        } catch (Exception ex) {
            System.exit(9);
        }
        System.exit(1);

    }

}
