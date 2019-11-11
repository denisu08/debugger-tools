package com.wirecard.tools.exercise;

public class HelloWorld {
    public HelloWorld() {
        this("World");
    }

    private String helloTo;

    public HelloWorld(String helloTo) {
        this.helloTo = helloTo;
    }

    public HelloWorld(String helloTo, String helloAnd) {
        this(helloTo + " and " + helloAnd);
    }

    public String sayHello() {
        return "Hello, " + this.helloTo;
    }

    public void setHelloTo(String helloTo) {
        this.helloTo = helloTo;
    }

    public String getHelloTo() {
        return this.helloTo;
    }

    public String toString() {
        return "HelloWorld{helloTo='" + this.helloTo + '\'' + '}';
    }
}
