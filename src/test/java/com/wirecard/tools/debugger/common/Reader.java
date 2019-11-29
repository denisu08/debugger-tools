package com.wirecard.tools.debugger.common;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

public class Reader extends Object {
    public Reader() {
    }

    public static void main(String[] args) throws Exception {
        /*java.io.FileInputStream in = new java.io.FileInputStream("out.txt");

        java.nio.channels.FileChannel fc = in.getChannel();
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(10);

        while (fc.read(bb) >= 0) {
            bb.flip();
            while (bb.hasRemaining()) {
                System.out.println((char) bb.get());
            }
            bb.clear();
        }

        System.exit(0);*/

        boolean running = true;
        BufferedInputStream reader = new BufferedInputStream(new FileInputStream("out.txt"));
        while (running) {
            if (reader.available() > 0) {
                System.out.print((char) reader.read());
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    running = false;
                }
            }
        }
    }
}
