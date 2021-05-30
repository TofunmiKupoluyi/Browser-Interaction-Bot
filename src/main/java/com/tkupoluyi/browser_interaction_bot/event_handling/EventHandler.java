package com.tkupoluyi.browser_interaction_bot.event_handling;

import com.tkupoluyi.browser_interaction_bot.Event;
import com.tkupoluyi.browser_interaction_bot.exceptions.InteractionBotException;
import org.openqa.selenium.chrome.ChromeDriver;

public interface EventHandler {
    void setDriver(ChromeDriver driver);
    void triggerEvent(Event event) throws InteractionBotException;
}
