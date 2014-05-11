package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.thrift.TException;


public class HelloServiceImpl implements HelloService.Iface {

    @Override
    public User getUser(int id, String name, int age) throws Xception, TException {
        User user = new User();
        user.setAge(11);
        user.setName("tom");
        user.setId(1);
        return user;
    }


    @Override
    public String getString(String str) throws TException {
        Map<String, String> headerMap = HttpHeaderHolder.get();
        System.out.println("------------------" + headerMap.get("bomb"));
        return str;
    }


    @Override
    public void sayHello(String str) throws TException {
        Map<String, String> headerMap = HttpHeaderHolder.get();
        System.out.println("------------------" + headerMap.get("bomb"));
        System.out.println(str);
    }

}
