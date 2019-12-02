package com.wirecard.tools.debugger.common;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import static java.lang.String.format;

public class TailSSHThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(TailSSHThread.class);

    private Thread t;
    private String threadName;
    private String processFlowGeneratorId;
    private SimpMessagingTemplate messagingTemplate;

    public TailSSHThread(String name, String processFlowGeneratorId, SimpMessagingTemplate messagingTemplate) {
        this.threadName = name;
        this.processFlowGeneratorId = processFlowGeneratorId;
        this.messagingTemplate = messagingTemplate;
        System.out.println("Creating " + threadName);
    }

    public void run() {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(GlobalVariables.jdiContainer.get(processFlowGeneratorId).getUser(), GlobalVariables.jdiContainer.get(processFlowGeneratorId).getIp());
            session.setPassword(GlobalVariables.jdiContainer.get(processFlowGeneratorId).getPassword());
            Hashtable<String, String> config = new Hashtable<String, String>();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(15000);
            session.setServerAliveInterval(15000);

            ChannelExec m_channelExec = (ChannelExec) session.openChannel("exec");
            // String cmd = "tail -f /root/MicroService/processflow_bologinflow_1.0_7001/jar_run/nohup.out";
            String cmd = format("tail -10f %s", GlobalVariables.jdiContainer.get(processFlowGeneratorId).getLogPath());
            m_channelExec.setCommand(cmd);
            InputStream m_in = m_channelExec.getInputStream();
            m_channelExec.connect();
            BufferedReader m_bufferedReader = new BufferedReader(new InputStreamReader(m_in));
            while (GlobalVariables.jdiContainer.get(processFlowGeneratorId).isListenLogger()) {
                if (m_bufferedReader.ready()) {
                    String line = m_bufferedReader.readLine();
                    messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), DebuggerConstant.LOGGER_PREFIX + line);
                }
                Thread.sleep(100);
            }
            m_bufferedReader.close();
            m_channelExec.sendSignal("SIGINT");
            m_channelExec.disconnect();
            session.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage(), ex);
        }
        logger.info("exit logger");
    }

    public void start() {
        System.out.println("Starting " + threadName);
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }
}
