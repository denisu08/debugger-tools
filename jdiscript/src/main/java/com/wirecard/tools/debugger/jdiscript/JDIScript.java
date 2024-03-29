package com.wirecard.tools.debugger.jdiscript;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.wirecard.tools.debugger.jdiscript.events.DebugEventDispatcher;
import com.wirecard.tools.debugger.jdiscript.events.EventThread;
import com.wirecard.tools.debugger.jdiscript.requests.*;
import com.wirecard.tools.debugger.jdiscript.handlers.DebugEventHandler;
import com.wirecard.tools.debugger.jdiscript.handlers.OnAccessWatchpoint;
import com.wirecard.tools.debugger.jdiscript.handlers.OnBreakpoint;
import com.wirecard.tools.debugger.jdiscript.handlers.OnClassPrepare;
import com.wirecard.tools.debugger.jdiscript.handlers.OnClassUnload;
import com.wirecard.tools.debugger.jdiscript.handlers.OnException;
import com.wirecard.tools.debugger.jdiscript.handlers.OnMethodEntry;
import com.wirecard.tools.debugger.jdiscript.handlers.OnMethodExit;
import com.wirecard.tools.debugger.jdiscript.handlers.OnModificationWatchpoint;
import com.wirecard.tools.debugger.jdiscript.handlers.OnMonitorContendedEnter;
import com.wirecard.tools.debugger.jdiscript.handlers.OnMonitorContendedEntered;
import com.wirecard.tools.debugger.jdiscript.handlers.OnMonitorWait;
import com.wirecard.tools.debugger.jdiscript.handlers.OnMonitorWaited;
import com.wirecard.tools.debugger.jdiscript.handlers.OnStep;
import com.wirecard.tools.debugger.jdiscript.handlers.OnThreadDeath;
import com.wirecard.tools.debugger.jdiscript.handlers.OnThreadStart;
import com.wirecard.tools.debugger.jdiscript.handlers.OnVMDeath;
import com.wirecard.tools.debugger.jdiscript.handlers.Once;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingAccessWatchpointRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingBreakpointRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingClassPrepareRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingClassUnloadRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingExceptionRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingMethodEntryRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingMethodExitRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingModificationWatchpointRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingMonitorContendedEnterRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingMonitorContendedEnteredRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingMonitorWaitRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingMonitorWaitedRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingStepRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingThreadDeathRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingThreadStartRequest;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingVMDeathRequest;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.AccessWatchpointRequest;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.ClassUnloadRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;
import com.sun.jdi.request.MonitorContendedEnterRequest;
import com.sun.jdi.request.MonitorContendedEnteredRequest;
import com.sun.jdi.request.MonitorWaitRequest;
import com.sun.jdi.request.MonitorWaitedRequest;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.ThreadDeathRequest;
import com.sun.jdi.request.ThreadStartRequest;
import com.sun.jdi.request.VMDeathRequest;

/**
 * Top-level class encapsulating common operations for working with
 * JDI and jdiscript.
 * <p>
 * <ul>
 *  <li>
 *    Pulls the {@link EventRequestManager} create*Request
 *    methods up to the top level, shortens their names, returns
 *    chainable requests, and allows handlers to be specified when
 *    requests are created.
 *  </li>
 *  <li>
 *    Pulls the {@link EventRequestManager} *Requests
 *    methods up to the top level and adds filtering by handler.
 *  </li>
 *  <li>
 *    Runs the associated VM by setting up and starting the jdiscript
 *    {@link DebugEventDispatcher} and {@link EventThread}.
 *  </li>
 *  <li>
 *    Provides convenience methods for specifying handlers for field
 *    access and modification.
 *  </li>
 * </ul>
 */
public class JDIScript {

    private final VirtualMachine vm;
    private final EventRequestManager erm;

    public JDIScript(VirtualMachine vm) {
        this.vm = vm;
        this.erm = vm.eventRequestManager();
    }

    /**
     * @return The underlying {@link VirtualMachine}.
     */
    public VirtualMachine vm() {
        return vm;
    }

    /**
     * Run the underlying {@link VirtualMachine} with no VM
     * event handlers and no timeout.  Note that you may still add
     * handlers for other events created with this class's *Request
     * methods.
     */
    public void run() {
        run(0);
    }

    /**
     * Run the underlying {@link VirtualMachine} with no VM
     * event handlers and a given timeout, in millis.  Note that
     * you may still add handlers for other events created with
     * this class's *Request methods.
     *
     * @param millis    Timeout in millis.
     */
    public void run(long millis) {
        List<DebugEventHandler> empty = Collections.emptyList();
        run(empty, millis);
    }

    /**
     * Run the underlying {@link VirtualMachine} with a single VM
     * event handler and no timeout.  Note that you may still add
     * handlers for other events created with this class's *Request
     * methods.
     *
     * @param handler  A DebugEventHandler for VM events.
     */
    public void run(DebugEventHandler handler) {
        run(handler, 0);
    }

    /**
     * Run the underlying {@link VirtualMachine} with multiple VM
     * event handlers and no timeout.  Note that you may still add
     * handlers for other events created with this class's *Request
     * methods.
     *
     * @param handlers  A list of DebugEventHandlers for VM events.
     */
    public void run(List<DebugEventHandler> handlers) {
        run(handlers, 0);
    }

    /**
     * Run the underlying {@link VirtualMachine} with a single VM
     * event handler and a given timeout, in millis.  Note that you
     * may still add handlers for other events created with this
     * class's *Request methods.
     *
     * @param handler  A DebugEventHandler for VM events.
     * @param millis    Timeout in millis.
     */
    public void run(DebugEventHandler handler, long millis) {
        run(Collections.singletonList(handler), millis);
    }

    /**
     * Run the underlying {@link VirtualMachine} with a multiple VM
     * event handlers and a given timeout, in millis.  Note that you
     * may still add handlers for other events created with this
     * class's *Request methods.
     *
     * @param handlers  A list of DebugEventHandlers for VM events.
     * @param millis    Timeout in millis.
     */
    public void run(List<DebugEventHandler> handlers, long millis) {
        DebugEventDispatcher dispatcher = new DebugEventDispatcher();
        dispatcher.addHandlers(handlers);
        EventThread eventThread = new EventThread(vm, dispatcher);
        eventThread.start();
        try {
            eventThread.join(millis);
        } catch(InterruptedException exc) {
            //TODO: handle this?
        }

    }

    // Convenience methods for creating EventRequests, that will automatically
    // set the handler as a property so that the Dispatcher works correctly.

    /**
     * @see EventRequestManager#createAccessWatchpointRequest
     */
    public ChainingAccessWatchpointRequest accessWatchpointRequest(Field field) {
        return accessWatchpointRequest(field, null);
    }

    /**
     * @see EventRequestManager#createAccessWatchpointRequest
     */
    public ChainingAccessWatchpointRequest accessWatchpointRequest(Field field, OnAccessWatchpoint handler) {
        return EventRequestProxy.proxy(erm.createAccessWatchpointRequest(field),
                     ChainingAccessWatchpointRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createBreakpointRequest
     */
    public ChainingBreakpointRequest breakpointRequest(Location location) {
        return breakpointRequest(location, null);
    }

    /**
     * @see EventRequestManager#createBreakpointRequest
     */
    public ChainingBreakpointRequest breakpointRequest(Location location, OnBreakpoint handler) {
        return EventRequestProxy.proxy(erm.createBreakpointRequest(location),
                     ChainingBreakpointRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createClassPrepareRequest
     */
    public ChainingClassPrepareRequest classPrepareRequest() {
        return classPrepareRequest(null);
    }

    /**
     * @see EventRequestManager#createClassPrepareRequest
     */
    public ChainingClassPrepareRequest classPrepareRequest(OnClassPrepare handler) {
        return EventRequestProxy.proxy(erm.createClassPrepareRequest(),
                     ChainingClassPrepareRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createClassUnloadRequest
     */
    public ChainingClassUnloadRequest classUnloadRequest() {
        return classUnloadRequest(null);
    }

    /**
     * @see EventRequestManager#createClassUnloadRequest
     */
    public ChainingClassUnloadRequest classUnloadRequest(OnClassUnload handler) {
        return EventRequestProxy.proxy(erm.createClassUnloadRequest(),
                     ChainingClassUnloadRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createExceptionRequest
     */
    public ChainingExceptionRequest exceptionRequest(ReferenceType refType,
                                             boolean notifyCaught,
                                             boolean notifyUncaught) {
        return exceptionRequest(refType, notifyCaught, notifyUncaught, null);
    }

    /**
     * @see EventRequestManager#createExceptionRequest
     */
    public ChainingExceptionRequest exceptionRequest(ReferenceType refType,
                                             boolean notifyCaught,
                                             boolean notifyUncaught,
                                             OnException handler) {
        return EventRequestProxy.proxy(erm.createExceptionRequest(refType, notifyCaught, notifyUncaught),
                     ChainingExceptionRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createMethodEntryRequest
     */
    public ChainingMethodEntryRequest methodEntryRequest() {
        return methodEntryRequest(null);
    }

    /**
     * @see EventRequestManager#createMethodEntryRequest
     */
    public ChainingMethodEntryRequest methodEntryRequest(OnMethodEntry handler) {
        return EventRequestProxy.proxy(erm.createMethodEntryRequest(),
                     ChainingMethodEntryRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createMethodExitRequest
     */
    public ChainingMethodExitRequest methodExitRequest() {
        return methodExitRequest(null);
    }

    /**
     * @see EventRequestManager#createMethodExitRequest
     */
    public ChainingMethodExitRequest methodExitRequest(OnMethodExit handler) {
        return EventRequestProxy.proxy(erm.createMethodExitRequest(),
                     ChainingMethodExitRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createModificationWatchpointRequest
     */
    public ChainingModificationWatchpointRequest modificationWatchpointRequest(Field field) {
        return modificationWatchpointRequest(field, null);
    }

    /**
     * @see EventRequestManager#createModificationWatchpointRequest
     */
    public ChainingModificationWatchpointRequest modificationWatchpointRequest(Field field, OnModificationWatchpoint handler) {
        return EventRequestProxy.proxy(erm.createModificationWatchpointRequest(field),
                     ChainingModificationWatchpointRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createMonitorContendedEnteredRequest
     */
    public ChainingMonitorContendedEnteredRequest monitorContendedEnteredRequest() {
        return monitorContendedEnteredRequest(null);
    }

    /**
     * @see EventRequestManager#createMonitorContendedEnteredRequest
     */
    public ChainingMonitorContendedEnteredRequest monitorContendedEnteredRequest(OnMonitorContendedEntered handler) {
        return EventRequestProxy.proxy(erm.createMonitorContendedEnteredRequest(),
                     ChainingMonitorContendedEnteredRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createMonitorContendedEnterRequest
     */
    public ChainingMonitorContendedEnterRequest monitorContendedEnterRequest() {
        return monitorContendedEnterRequest(null);
    }

    /**
     * @see EventRequestManager#createMonitorContendedEnterRequest
     */
    public ChainingMonitorContendedEnterRequest monitorContendedEnterRequest(OnMonitorContendedEnter handler) {
        return EventRequestProxy.proxy(erm.createMonitorContendedEnterRequest(),
                     ChainingMonitorContendedEnterRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createMonitorWaitedRequest
     */
    public ChainingMonitorWaitedRequest monitorWaitedRequest() {
        return monitorWaitedRequest(null);
    }

    /**
     * @see EventRequestManager#createMonitorWaitedRequest
     */
    public ChainingMonitorWaitedRequest monitorWaitedRequest(OnMonitorWaited handler) {
        return EventRequestProxy.proxy(erm.createMonitorWaitedRequest(),
                     ChainingMonitorWaitedRequest.class).addHandler(handler);
    }
    
    /**
     * @see EventRequestManager#createMonitorWaitRequest
     */
    public ChainingMonitorWaitRequest monitorWaitRequest() {
        return monitorWaitRequest(null);
    }

    /**
     * @see EventRequestManager#createMonitorWaitRequest
     */
    public ChainingMonitorWaitRequest monitorWaitRequest(OnMonitorWait handler) {
        return EventRequestProxy.proxy(erm.createMonitorWaitRequest(),
                     ChainingMonitorWaitRequest.class).addHandler(handler);
    }    

    /**
     * @see EventRequestManager#createStepRequest
     */
    public ChainingStepRequest stepRequest( ThreadReference thread,
                                            int size,
                                            int depth ) {
        return stepRequest(thread, size, depth, null);
    }

    /**
     * @see EventRequestManager#createStepRequest
     */
    public ChainingStepRequest stepRequest( ThreadReference thread,
                                            int size,
                                            int depth,
                                            OnStep handler) {
        return EventRequestProxy.proxy(erm.createStepRequest(thread, size, depth),
                     ChainingStepRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createThreadDeathRequest
     */
    public ChainingThreadDeathRequest threadDeathRequest() {
        return threadDeathRequest(null);
    }

    /**
     * @see EventRequestManager#createThreadDeathRequest
     */
    public ChainingThreadDeathRequest threadDeathRequest(OnThreadDeath handler) {
        return EventRequestProxy.proxy(erm.createThreadDeathRequest(),
                     ChainingThreadDeathRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createThreadStartRequest
     */
    public ChainingThreadStartRequest threadStartRequest() {
        return threadStartRequest(null);
    }

    /**
     * @see EventRequestManager#createThreadStartRequest
     */
    public ChainingThreadStartRequest threadStartRequest(OnThreadStart handler) {
        return EventRequestProxy.proxy(erm.createThreadStartRequest(),
                     ChainingThreadStartRequest.class).addHandler(handler);
    }

    /**
     * @see EventRequestManager#createVMDeathRequest
     */
    public ChainingVMDeathRequest vmDeathRequest() {
        return vmDeathRequest(null);
    }

    /**
     * @see EventRequestManager#createVMDeathRequest
     */
    public ChainingVMDeathRequest vmDeathRequest(OnVMDeath handler) {
        return EventRequestProxy.proxy(erm.createVMDeathRequest(),
                     ChainingVMDeathRequest.class).addHandler(handler);
    }

    // Convenience method for accessing only those EventRequests that are
    // associated with a given handler

    public List<AccessWatchpointRequest> accessWatchpointRequests(DebugEventHandler handler) {
        return filter(erm.accessWatchpointRequests(), handler);
    }

    public List<BreakpointRequest> breakpointRequests(DebugEventHandler handler) {
        return filter(erm.breakpointRequests(), handler);
    }

    public List<ClassPrepareRequest> classPrepareRequests(DebugEventHandler handler) {
        return filter(erm.classPrepareRequests(), handler);
    }

    public List<ClassUnloadRequest> classUnloadRequests(DebugEventHandler handler) {
        return filter(erm.classUnloadRequests(), handler);
    }

    public List<ExceptionRequest> exceptionRequests(DebugEventHandler handler) {
        return filter(erm.exceptionRequests(), handler);
    }

    public List<MethodEntryRequest> methodEntryRequests(DebugEventHandler handler) {
        return filter(erm.methodEntryRequests(), handler);
    }

    public List<MethodExitRequest> methodExitRequests(DebugEventHandler handler) {
        return filter(erm.methodExitRequests(), handler);
    }

    public List<ModificationWatchpointRequest> modificationWatchpointRequests(DebugEventHandler handler) {
        return filter(erm.modificationWatchpointRequests(), handler);
    }

    public List<MonitorContendedEnteredRequest> monitorContendedEnteredRequests(DebugEventHandler handler) {
        return filter(erm.monitorContendedEnteredRequests(), handler);
    }

    public List<MonitorContendedEnterRequest> monitorContendedEnterRequests(DebugEventHandler handler) {
        return filter(erm.monitorContendedEnterRequests(), handler);
    }

    public List<MonitorWaitedRequest> monitorWaitedRequests(DebugEventHandler handler) {
        return filter(erm.monitorWaitedRequests(), handler);
    }

    public List<MonitorWaitRequest> monitorWaitRequests(DebugEventHandler handler) {
        return filter(erm.monitorWaitRequests(), handler);
    }

    public List<StepRequest> stepRequests(DebugEventHandler handler) {
        return filter(erm.stepRequests(), handler);
    }

    public List<ThreadDeathRequest> threadDeathRequests(DebugEventHandler handler) {
        return filter(erm.threadDeathRequests(), handler);
    }

    public List<ThreadStartRequest> threadStartRequests(DebugEventHandler handler) {
        return filter(erm.threadStartRequests(), handler);
    }

    public List<VMDeathRequest> vmDeathRequests(DebugEventHandler handler) {
        return filter(erm.vmDeathRequests(), handler);
    }

    public <T extends EventRequest> List<T> filter(List<T> ers, DebugEventHandler handler) {
        return ers.stream()
            .filter(er -> DebugEventDispatcher.getHandlers(er).contains(handler))
            .collect(Collectors.toList());
    }

    public void deleteEventRequest(EventRequest eventRequest) {
        erm.deleteEventRequest(eventRequest);
    }

    public void deleteEventRequests(List<? extends EventRequest> eventRequests) {
        erm.deleteEventRequests(eventRequests);
    }

    /**
     * Print a stacktrace of the given event's thread to stdout.
     *
     * @param event   The event to print a trace for.
     */
    public void printTrace(LocatableEvent event) {
        printTrace(event, null, System.out);
    }

    /**
     * Print a stacktrace of the given event's thread to stdout with
     * a message.
     *
     * @param event   The event to print a trace for.
     * @param msg     A message to print with the stacktrace.
     */
    public void printTrace(LocatableEvent event, String msg) {
        printTrace(event, msg, System.out);
    }

    /**
     * Print a stacktrace of the given event's thread to the given stream
     * with a message.
     *
     * @param event   The event to print a trace for.
     * @param msg     A message to print with the stacktrace.
     * @param ps      The stream to print the trace to.
     */
    public void printTrace(LocatableEvent event, String msg, PrintStream ps) {
        long ts = System.currentTimeMillis();
        ThreadReference thread = event.thread();
        ps.printf("%s: Stacktrace for %s(name='%s', id='%s'): %s\n",
                  ts,
                  thread.type().name(),
                  thread.name(),
                  thread.uniqueID(),
                  (msg == null) ? "" : msg);
        try {
            for(StackFrame frame: thread.frames()) {
                ps.println("    " + frame.location());
            }
        }
        catch(IncompatibleThreadStateException e) {
            ps.println("    ** IncompatibleThreadStateException - " +
                       "thread not suspended in target VM, " +
                       "could not retrieve frames **");
        }
    }
    
    /**
     * Shortcut for the common pattern of decorating any class
     * on preparation.
     * <p>
     * Builds a {@link ClassPrepareRequest}, and adds the given 
     * {@link OnClassPrepare} handler.
     * <p>
     * TODO: what should this return?
     *
     * @param className  A class name suitable for use by
     *                   {@link ClassPrepareRequest#addClassFilter(String)}
     * @param handler    The callback to execute when the class is prepped.
     */
    public void onClassPrep(final OnClassPrepare handler) {
        classPrepareRequest()
            .addHandler(handler)
            .enable();
    }

    /**
     * Shortcut for the common pattern of decorating a particular class
     * on preparation.
     * <p>
     * Builds a {@link ClassPrepareRequest}, filters it for the given
     * class name, and adds the given {@link OnClassPrepare} handler.
     * <p>
     * TODO: what should this return?
     *
     * @param className  A class name suitable for use by
     *                   {@link ClassPrepareRequest#addClassFilter(String)}
     * @param handler    The callback to execute when the class is prepped.
     */
    public void onClassPrep(final String className,
                            final OnClassPrepare handler) {
        classPrepareRequest()
            .addClassFilter(className)
            .addHandler(handler)
            .enable();
    }

    /**
     * Shortcut for the common pattern of responding to field accesses.
     * <p>
     * Builds a {@link ClassPrepareRequest}, filters it for the given
     * class name, and adds an {@link OnClassPrepare} handler that
     * creates an {@link AccessWatchpointRequest} for the given field name.
     * Any field accesses are in turn handled by the given handler.
     * <p>
     * TODO: what should this return?
     *
     * @param className  A class name suitable for use by
     *                   {@link ClassPrepareRequest#addClassFilter(String)}
     * @param fieldName  A field name suitable for use by
     *                   {@link ReferenceType#fieldByName(String)}.  Must be
     *                   a field belonging to all classes matched by className.
     * @param handler    The callback to execute when the field is accessed.
     */
    public void onFieldAccess(final String className,
                              final String fieldName,
                              final OnAccessWatchpoint handler) {
        onClassPrep(className, ev -> { 
            Field field = ev.referenceType().fieldByName(fieldName);
            accessWatchpointRequest(field, handler).enable();
        });
    }

    /**
     * Shortcut for the common pattern of responding to field modifications.
     * <p>
     * Builds a {@link ClassPrepareRequest}, filters it for the given
     * class name, and adds an {@link OnClassPrepare} handler that
     * creates a {@link ModificationWatchpointRequest} for the given field name.
     * Any field modifications are in turn handled by the given handler.
     * <p>
     * TODO: what should this return?
     *
     * @param className  A class name suitable for use by
     *                   {@link ClassPrepareRequest#addClassFilter(String)}
     * @param fieldName  A field name suitable for use by
     *                   {@link ReferenceType#fieldByName(String)}.  Must be
     *                   a field belonging to all classes matched by className.
     * @param handler    The callback to execute when the field is modified.
     */
    public void onFieldModification(final String className,
                                    final String fieldName,
                                    final OnModificationWatchpoint handler) {
        onClassPrep(className, ev -> { 
            Field field = ev.referenceType().fieldByName(fieldName);
            modificationWatchpointRequest(field, handler).enable();
        });
    }

    /**
     * Shortcut for the common pattern of responding to particular method
     * invocations.
     * <p>
     * You would hope this could be handled with a simple
     * {@link MethodEntryRequest}, but unfortunately, it can't.
     * MethodEntryRequests can be filtered by class, instance, and
     * thread, but not down to individual methods.
     * <p>
     * This builds a {@link ClassPrepareRequest}, filters it for the
     * given class name, and adds an {@link OnClassPrepare} handler that
     * creates a {@link BreakpointRequest} for the given method name.
     * The resulting BreakpointEvent is in turn handled by the
     * given handler.  Note that if you refer to an overloaded method,
     * all matching methods will have a breakpoint set and be handled by
     * the given handler.  For greater control, use
     * {@link #onMethodInvocation(String, String, String, OnBreakpoint)}
     * <p>
     * TODO: what should this return?  A future for the BreakpointRequest?
     *
     * @param className  A class name suitable for use by
     *                   {@link ClassPrepareRequest#addClassFilter(String)}
     * @param methodName A method name suitable for use by
     *                   {@link ReferenceType#methodsByName(String)}.  Must be
     *                   a method belonging to all classes matched by className.
     * @param handler    The callback to execute when the method is invoked.
     */
    public void onMethodInvocation(final String className,
                                   final String methodName,
                                   final OnBreakpoint handler) {
        onClassPrep(className, ev -> {
            ev.referenceType().methodsByName(methodName).forEach(m -> 
                breakpointRequest(m.location(), handler).enable()
            );
        });
    }

    /**
     * Identical to {@link #onMethodInvocation(String, String, OnBreakpoint)},
     * extending filtering to include a method signature.
     * <p>
     * TODO: what should this return?  A future for the BreakpointRequest?
     *
     * @param className  A class name suitable for use by
     *                   {@link ClassPrepareRequest#addClassFilter(String)}
     * @param methodName A method name suitable for use by
     *                   {@link ReferenceType#methodsByName(String)}.  Must be
     *                   a method belonging to all classes matched by className.
     * @param methodSig  A method signature suitable for use by
     *                   {@link ReferenceType#methodsByName(String, String)}.
     *                   Must be a method belonging to all classes matched by
     *                   className.
     * @param handler    The callback to execute when the method is invoked.
     */
    public void onMethodInvocation(final String className,
                                   final String methodName,
                                   final String methodSig,
                                   final OnBreakpoint handler) {
        onClassPrep(className, ev -> { 
            ev.referenceType().methodsByName(methodName, methodSig).forEach(m ->
                breakpointRequest(m.location(), handler).enable()
            );
        });
    }
    
    /**
     * Creates a breakpointRequest for the exit from the currently executing method 
     * on the given thread.
     * 
     * @param thread
     * @param handler
     * @throws IncompatibleThreadStateException 
     * @throws AbsentInformationException 
     */
    public void onCurrentMethodExit(final ThreadReference thread,
                                    //final OnMethodExit handler) 
                                    final OnBreakpoint handler) 
        throws IncompatibleThreadStateException, AbsentInformationException 
    {
        List<Location> locs = thread.frame(0).location().method().allLineLocations();
        Location last = locs.get(locs.size()-1);
        breakpointRequest(last, handler)
            .addInstanceFilter(thread.frame(0).thisObject())
            .enable();
    }

    /**
     * Create a stepRequest and enable it.
     * 
     * @param thread
     * @param size
     * @param depth
     * @param handler
     */
    public void onStep(final ThreadReference thread,
    		           final int size,
    		           final int depth,
    		           final OnStep handler) {
    	stepRequest(thread, size, depth, handler).enable();
    }

    /**
     * onStep with size=StepRequest.STEP_MIN, depth=StepRequest.STEP_INTO
     * @param thread
     * @param handler
     */
    public void onStepInto(final ThreadReference thread, 
    		               final OnStep handler) {
    	onStep(thread, StepRequest.STEP_MIN, StepRequest.STEP_INTO, handler);
    }
    
    /**
     * onStep with size=StepRequest.STEP_MIN, depth=StepRequest.STEP_OVER
     * @param thread
     * @param handler
     */    
    public void onStepOver(final ThreadReference thread,
    					   final OnStep handler) {
    	onStep(thread, StepRequest.STEP_MIN, StepRequest.STEP_OVER, handler);
    }
    
    /**
     * onStep with size=StepRequest.STEP_MIN, depth=StepRequest.STEP_OUT
     * @param thread
     * @param handler
     */    
    public void onStepOut(final ThreadReference thread,
    					  final OnStep handler) {
        onStep(thread, StepRequest.STEP_MIN, StepRequest.STEP_OUT, handler); 
    }    
    
    /**
     * Create a handler that runs the given handler once and then disables
     * the event request that caused the handler to be invoked.
     * 
     * @param <K> The type of the inner handler
     * @param handler  The inner handler
     * @return An instance of {@link Once} cast to the same type as the inner handler.
     */
    @SuppressWarnings("unchecked")
    public <K extends DebugEventHandler> K once(K handler) {
    	return (K)new Once(handler);
    }
    
    public String fullName(Method method) {
    	final String refType = method.declaringType().name();
    	final String methName = method.name();
    	final String methSig = String.join(", ", method.argumentTypeNames());
    	return refType + "." + methName + "(" + methSig + ")";
    }

}
