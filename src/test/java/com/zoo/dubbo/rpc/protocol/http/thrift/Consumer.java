package com.zoo.dubbo.rpc.protocol.http.thrift;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Consumer {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:demo-consumer.xml");
        ctx.start();
        HelloService.Iface demoService = (HelloService.Iface)ctx.getBean("helloService");
        demoService.sayHello("hello world");
        String str = demoService.getString("hello world");
        System.out.println(str);
    }
}
