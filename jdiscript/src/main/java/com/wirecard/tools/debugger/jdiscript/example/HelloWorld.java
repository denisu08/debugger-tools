package com.wirecard.tools.debugger.jdiscript.example;

//A test class you can use for simple debugging scripts.
public class HelloWorld {
    private String helloTo;

    public HelloWorld() {
        this("World");
    }

    public HelloWorld(String helloTo) {
        this.helloTo = helloTo;
    }

    public HelloWorld(String helloTo, String helloAnd) {
        this(helloTo + " and " + helloAnd);
    }

    public String sayHello() {
        return "Hello, " + helloTo;
    }

    public void setHelloTo(String helloTo) {
        this.helloTo = helloTo;
    }

    public String getHelloTo() {
        return helloTo;
    }

    public void startMe() {
        HelloWorld hello;

        hello = new HelloWorld();
        System.out.println(hello.sayHello());
        System.out.println(hello.sayHello());
        hello.setHelloTo("Barney");
        System.out.println(hello.sayHello());
        System.out.println(hello.sayHello());

        hello = new HelloWorld("Fred", "Wilma");
        System.out.println(hello.sayHello());
        System.out.println(hello.sayHello());
    }

    public static void main(String[] args) {
        new HelloWorld().startMe();
    }
}
