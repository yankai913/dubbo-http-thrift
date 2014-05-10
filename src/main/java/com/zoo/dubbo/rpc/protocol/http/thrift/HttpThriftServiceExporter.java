package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;

import com.alibaba.dubbo.remoting.http.HttpHandler;


/**
 * 
 * @author yankai913@gmail.com
 * @date 2014-5-4
 */
public class HttpThriftServiceExporter {

    private static final String THRIFT_CONTENT_TYPE = HttpExecutor.CONTENT_TYPE_SERIALIZED_OBJECT;

    protected static ConcurrentMap<Class<?>, TProcessor> serviceClass2tProcessorMap =
            new ConcurrentHashMap<Class<?>, TProcessor>();

    protected static TMultiplexedProcessor tMultiplexedProcessor = new TMultiplexedProcessor();

    public List<HttpHandler> extHandlerList = new ArrayList<HttpHandler>();

    private Object service;

    private Class<?> serviceInterface;

    private Object proxy;


    public String getContentType() {
        return THRIFT_CONTENT_TYPE;
    }


    public Object getService() {
        return service;
    }


    public void setService(Object service) {
        this.service = service;
    }


    public Class<?> getServiceInterface() {
        return serviceInterface;
    }


    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }


    public Object getProxy() {
        return proxy;
    }


    public void setProxy(Object proxy) {
        this.proxy = proxy;
    }


    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        //
        response.setContentType(THRIFT_CONTENT_TYPE);
        InputStream inputStream = request.getInputStream();
        ;
        OutputStream outputStream = response.getOutputStream();
        ;
        String serviceName = getServiceInterface().getName();
        TProtocol proto = HttpThriftTool.newProtocol(inputStream, outputStream, serviceName);
        TProcessor processor = null;
        try {
            processor = getTProcessor(getServiceInterface(), getService());
            // register
            tMultiplexedProcessor.registerProcessor(serviceName, processor);
            tMultiplexedProcessor.process(proto, proto);
        }
        catch (Throwable t) {
            inputStream.reset();
            TMessage tmessage = proto.readMessageBegin();
            HttpThriftTool.createErrorTMessage(proto, tmessage.name, tmessage.seqid,
                "Server-Side Error:" + t.toString());
        }
        finally {
            proto.getTransport().flush();
            proto.getTransport().close();
        }
    }


    public void afterPropertiesSet() {
        // TODO 这里可以对service做增强处理，返回代理对象proxy
    }


    TProcessor getTProcessor(Class<?> serviceIface, Object serviceImpl) {
        try {
            if (serviceImpl == null) {
                throw new IllegalStateException("serviceImpl is null, can not create TProcessor");
            }
            TProcessor processor = serviceClass2tProcessorMap.get(serviceIface);
            if (processor == null) {
                String iface = serviceIface.getName();
                String processorServiceName = iface.substring(0, iface.lastIndexOf("$")) + "$Processor";
                Class<?> proServiceClazz = Class.forName(processorServiceName);
                processor =
                        (TProcessor) proServiceClazz.getConstructor(serviceIface).newInstance(serviceImpl);
                serviceClass2tProcessorMap.putIfAbsent(serviceIface, processor);
            }
            return processor;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    void filter() {
        for (HttpHandler handler : extHandlerList) {

        }
    }
}
