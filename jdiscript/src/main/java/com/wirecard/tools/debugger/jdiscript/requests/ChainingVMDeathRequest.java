package com.wirecard.tools.debugger.jdiscript.requests;

import com.wirecard.tools.debugger.jdiscript.handlers.OnVMDeath;

public interface ChainingVMDeathRequest extends JDIScriptEventRequest {

    //Chaining EventRequest methods
    ChainingVMDeathRequest addCountFilter(int count);
    ChainingVMDeathRequest disable();
    ChainingVMDeathRequest enable();
    ChainingVMDeathRequest putProperty(Object key, Object value);
    ChainingVMDeathRequest setEnabled(boolean val);
    ChainingVMDeathRequest setSuspendPolicy(int policy);

    ChainingVMDeathRequest addHandler(OnVMDeath handler);
}
