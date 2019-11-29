package com.wirecard.tools.debugger.common;

public class Writer extends Object {
    Writer() {
    }

    public static String[] strings =
            {
                    "Hello World",
                    "Goodbye World"
            };

    public static void main(String[] args)
            throws java.io.IOException {

        java.io.PrintWriter pw =
                new java.io.PrintWriter(new java.io.FileOutputStream("out.txt"), true);

        for (String s : strings) {
            pw.println(s);
            System.in.read();
        }

        pw.close();
    }
}
