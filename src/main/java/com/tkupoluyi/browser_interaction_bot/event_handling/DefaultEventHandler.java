package com.tkupoluyi.browser_interaction_bot.event_handling;

import com.tkupoluyi.browser_interaction_bot.BrowserInteractions;
import com.tkupoluyi.browser_interaction_bot.Event;
import com.tkupoluyi.browser_interaction_bot.event_handling.EventHandler;
import com.tkupoluyi.browser_interaction_bot.exceptions.InteractionBotException;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.remote.UnreachableBrowserException;

public class DefaultEventHandler implements EventHandler {
    int unreachableCnt;
    ChromeDriver driver;

    public DefaultEventHandler() {
        unreachableCnt = 0;
    }

    public void setDriver(ChromeDriver driver) {
        this.driver = driver;
    }

    @Override
    public void triggerEvent(Event event) throws InteractionBotException {
        String xpath = event.getXpath();
        String eventType = event.getEventType();
        WebElement element = findElementByXpath(xpath);
        try {
            Actions actions = new Actions(driver);
            if (eventType.equals("click") || eventType.equals("mousedown") || eventType.equals("mouseup") || eventType.equals("focus") || eventType.equals("blur")) {
                actions.moveToElement(element).click(element).build().perform();
            } else if (eventType.equals("mouseover") || eventType.equals("mouseenter")) {
                actions.moveToElement(element).build().perform();
            } else if (eventType.equals("mouseout") || eventType.equals("mouseleave")) {
                actions.moveToElement(element).build().perform();
                actions.moveByOffset(100, 100).build().perform();
            } else if (eventType.equals("keydown") || eventType.equals("keypress") || eventType.equals("keyup") || eventType.equals("input")) {
                actions.moveToElement(element).click(element).sendKeys("ABCD").build().perform();
            } else if (eventType.equals("dblclick")) {
                actions.moveToElement(element).doubleClick(element).build().perform();
            } else if (eventType.equals("change")) {
                actions.moveToElement(element).click(element).sendKeys("ABCD").build().perform();
            } else if (eventType.equals("drag") || eventType.equals("dragstart") || eventType.equals("dragend")) {
                actions.moveToElement(element).dragAndDropBy(element, 100, 0).perform();
            } else if (eventType.equals("baseEvent")){
                System.out.println("Base event triggered" + eventType);
            } else {
                throw new InteractionBotException(InteractionBotException.INTERACTION_NOT_SUPPORTED);
            }
        } catch(MoveTargetOutOfBoundsException | JavascriptException | NullPointerException ignored) {
            // ignored
        } catch (UnreachableBrowserException ex) {
            handleUnreachableBrowserException();
        } catch (Exception ex) {
            throw new InteractionBotException(InteractionBotException.INTERACTION_EXCEPTION, ex.getMessage());
        }
        BrowserInteractions.closeExtraneousTabs(driver, 1);
    }

    private WebElement findElementByXpath(String xpath) throws InteractionBotException {
        boolean webElementFound = false;
        WebElement element = null;

        while (!webElementFound) {
            try {
                element = driver.findElement(By.xpath(xpath)); // It is at this point that a NoSuchElementException is triggered
                webElementFound = true;
            } catch (NotFoundException ex) {
                throw new InteractionBotException(InteractionBotException.ELEMENT_NOT_FOUND);
            } catch (UnreachableBrowserException ex) {
                handleUnreachableBrowserException();
            } catch (Exception ex) {
                throw new InteractionBotException(InteractionBotException.UNSPECIFIED_EXCEPTION, ex.getMessage());
            }
        }

        return element;
    }

    private void handleUnreachableBrowserException() throws InteractionBotException {
        unreachableCnt += 1;
        if (unreachableCnt < 30) {
            System.out.println("Browser Unreachable. Retrying in 1 second, "+(unreachableCnt)+"/30");
            try { Thread.sleep(1000); } catch (InterruptedException ignore) { }
        }
        throw new InteractionBotException(InteractionBotException.UNREACHEABLE_BROWSER);
    }
}
