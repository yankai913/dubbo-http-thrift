package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.rpc.RpcInvocation;


/**
 * 
 * @author yankai913@gmail.com
 * @date 2014-5-9
 */
public class HttpThriftServiceFactoryBean {

    private Class<?> serviceInterface;

    private String serviceUrl;

    private HttpExecutor httpExecutor;

    private Object serviceProxy;

    private com.alibaba.dubbo.common.URL url;


    public HttpThriftServiceFactoryBean() {

    }


    public HttpExecutor getHttpExecutor() {
        return httpExecutor;
    }


    public void setHttpExecutor(HttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }


    public Object getServiceProxy() {
        return serviceProxy;
    }


    public void setServiceProxy(Object serviceProxy) {
        this.serviceProxy = serviceProxy;
    }


    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }


    public final Class<?> getServiceInterface() {
        return serviceInterface;
    }


    public String getServiceUrl() {
        return serviceUrl;
    }


    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }


    public com.alibaba.dubbo.common.URL getUrl() {
        return url;
    }


    public void setUrl(com.alibaba.dubbo.common.URL url) {
        this.url = url;
    }


    public Object getObject() {
        InvocationHandler invocationHandler = new InnerInvocationHanlder(serviceInterface, httpExecutor, url);
        Class<?>[] interfaces = new Class<?>[] { getServiceInterface() };
        // TODO 注意CLASSLOADER，可缓存
        ClassLoader classLoader = HttpThriftServiceFactoryBean.class.getClassLoader();
        this.serviceProxy = Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
        return this.serviceProxy;
    }

    public static class InnerInvocationHanlder implements InvocationHandler {

        private Class<?> serviceInterface;

        private HttpExecutor httpExecutor;

        private com.alibaba.dubbo.common.URL url;


        public InnerInvocationHanlder(Class<?> serviceInterface, HttpExecutor httpExecutor,
                com.alibaba.dubbo.common.URL url) {
            this.serviceInterface = serviceInterface;
            this.httpExecutor = httpExecutor;
            this.url = url;
        }


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // TODO 发请求
            RpcInvocation rpcInvocation = new RpcInvocation();
            rpcInvocation.setParameterTypes(method.getParameterTypes());
            rpcInvocation.setArguments(args);
            rpcInvocation.setMethodName(method.getName());
            rpcInvocation.setAttachment(Constants.INTERFACE_KEY, serviceInterface.getName());

            return httpExecutor.syncExecuteRequest(url, rpcInvocation);
        }

    }

}
