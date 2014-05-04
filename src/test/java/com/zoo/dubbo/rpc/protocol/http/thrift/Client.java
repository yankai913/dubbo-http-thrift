package com.zoo.dubbo.rpc.protocol.http.thrift;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Client {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:demo-consumer.xml");
        ctx.start();
        HelloService.Iface demoService = (HelloService.Iface)ctx.getBean("demoService");
        demoService.sayHello("hello world");
    }
}
