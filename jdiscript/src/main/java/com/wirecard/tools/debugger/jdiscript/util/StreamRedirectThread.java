/*
 * @(#)StreamRedirectThread.java    1.6 05/11/17
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
/*
 * Copyright (c) 1997-2001 by Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */
package com.wirecard.tools.debugger.jdiscript.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * StreamRedirectThread is a thread which copies its input to its output and
 * terminates when it completes.
 *
 * @version @(#) StreamRedirectThread.java 1.6 05/11/17 04:47:11
 * @author Robert Field
 */
class StreamRedirectThread extends Thread {
    private final BufferedReader in;
    private final PrintStream out;

    /**
     * Set up for copy.
     *
     * @param name
     *            Name of the thread
     * @param in
     *            Stream to copy from
     * @param out
     *            Stream to copy to
     */
    StreamRedirectThread(String name, InputStream in, OutputStream out) {
        this(name, in, new PrintStream(out));
    }

    StreamRedirectThread(String name, InputStream in, PrintStream out) {
        super(name);
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = out;
        setPriority(Thread.MAX_PRIORITY - 1);
    }

    /**
     * Copy.
     */
    @Override
    public void run() {
        try {
            String str = "";
            while (str != null && !isInterrupted()) {
                str = in.readLine();
                if(str != null) {
                    out.println(str);
                }
            }
        } catch (IOException exc) {
            if(!isInterrupted()) {
                exc.printStackTrace();
            }
        }
    }
}
