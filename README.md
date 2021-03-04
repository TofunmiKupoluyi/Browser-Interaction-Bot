# PageInteractionBot
This is a bot that attempts to identify all interactable elements within a page and interact with them. It relies on Selenium (with ChromeDevTools) and  ChromeDriver.

## Execution
To run this tool, ensure you have Java, Chrome and the corresponding version of <a href="https://chromedriver.chromium.org/downloads">ChromeDriver</a> installed.<br/>
With this setup, you can run the tool by executing:<br/>
```
java -jar browser_run.jar
```
The ```browser_run.jar``` file is included in the root of this project.

### What Happens?
We open the page (on two windows, we will cover why two windows shortly).
We identify all the elements on the page via a depth-first-search over the HTML document.<br/>
From this list of elements, we identify all the event listeners attached to each element using the ChromeDevTools protocol.<br/>
On the first window, we traverse the list of elements, simulating each event that may occur. The events currently covered include:<br/>
```click```
```mousedown```
```mouseup```
```focus```
```blur```
```mouseover```
```mouseenter```
```mouseout```
```mouseleave```
```keydown```
```keypress```
```keyup```
```input```
```dblclick```
```load```
```change```
```drag```
```dragstart```
```dragend```<br/>
Please feel free to extend this list by creating Selenium actions for events not currently covered.
On the second window, we do the same but traverse the list backwards. In addition to creating redundancy, this also helps in situations where on the first traversal, we open elements that we cannot close due to the order of the elements on the HTML document.
