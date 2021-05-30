# BrowserInteractionBot
This is a bot that attempts to identify all interactable elements within a page and interact with them. It relies on Selenium (with ChromeDevTools) and  ChromeDriver.

## Execution
To run this tool, ensure you have Java, Chrome and the corresponding version of <a href="https://chromedriver.chromium.org/downloads">ChromeDriver</a> installed.<br/>
With this setup, you can run the tool by executing:
<br/>
```
java -jar browser_run.jar [url_to_run] [output_directory] [url_of_proxy]
```
<br/>
The browser_run.jar file is included in the root of this project.

## What Happens When You Run?
- We identify all events on the page using Chrome Devtools.
- We determine dependent events using breadth-first search.
- We trigger all events, taking screenshots of the triggered events.
- We output screenshots of all triggered events as well as an event dependency graph, in the output_directory. By default, this is set to "screenshots".
- The events currently covered include:<br/>
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
```dragend```\
Please feel free to create custom event handlers to extend the list of handled events or even improve on the implementation of currently handled events. The default event handler is contained in the "event_handling" folder.
