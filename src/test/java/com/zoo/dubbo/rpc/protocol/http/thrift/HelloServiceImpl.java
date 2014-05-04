package com.zoo.dubbo.rpc.protocol.http.thrift;

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
        return str;
    }

    @Override
    public void sayHello(String str) throws TException {
           System.out.println(str);
    }

}
