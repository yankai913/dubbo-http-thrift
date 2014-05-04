package com.zoo.dubbo.rpc.protocol.http.thrift;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Producer {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:demo-provider.xml");
        ctx.start();
        System.in.read();
    }
}
