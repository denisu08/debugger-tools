Steps:
1. Tools > Service > Code Generator
    * Add button debug, then open dialog.
    * Select function list (sync with current status in function).
    * Add debug breakpoint in stage.
    * Click icon toolbar to simulate process.
    
Features:
1. Icon Toolbar:
    * Attach/Detach: connect/disconnect remote debug in runtime server.
    * Next: stop to next stage from current breakpoint.
    * Resume: stop to next breakpoint.
    * Mute: disable all breakpoints.
2. Add variable, click plus green button.
3. View: Stage List & Variable List(System + Custom)

Data Projection: ProcessFlowGenerator + IP > Service

TASK:
- When open popup, send ping to load & decompile source code. (COMMAND: PING)
- If connection has been attached, user change function dropdown (sync breakpoint in backend, generate breakpoint events) (COMMAND: SYNC_BR).
- When connect is triggered, activate all breakpoint that has isDebug flag is true.
- When current line breakpoint has changed from backend, function dropdown is changed automatically based on current status break event.
- Add custom expression watcher.
- Add break filter by criteria.
- Class Empty, do nothing, only flag breakpoint

How to build:
1. gradlew.bat build
2. gradlew.bat jar
3. remove (2) tools.jar in bundle build
4. insert tool_38 into build jar


Pseudocode 

generateNode:
if type == group {
    if(groupType == condition) {
        loop details
        call generateNodeCondition();   // need parameter condition append
        condition << rules
        start
            call generateNode();   // need parameter condition append
        end
    } else if(groupType == loop) {
        loop details
        start
            call generateNode();   // need parameter condition append
        end
    } else if(groupType == loopWithPaging) {
        loop details
        start
            call generateNode();   // need parameter condition append
        end
    } else if(groupType == multithread) {
        loop details
        start
            call generateNode();   // need parameter condition append
        end       
    }
} else {    // single
    createNode()
} 

createNode:
