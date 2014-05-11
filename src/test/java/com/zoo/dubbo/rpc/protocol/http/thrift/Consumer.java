package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Consumer {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:demo-consumer.xml");
        ctx.start();
        HelloService.Iface demoService = (HelloService.Iface)ctx.getBean("helloService");
        //
        Map<String, String> map = new HashMap<String, String>();
        map.put("bomb", "one");
        HttpHeaderHolder.set(map);
        demoService.sayHello("hello world");
        //
        Map<String, String> map2 = new HashMap<String, String>();
        map2.put("bomb", "two");
        HttpHeaderHolder.set(map2);
        String str = demoService.getString("hello world");
        System.out.println(str);
    }
}
