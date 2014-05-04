package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.alibaba.dubbo.remoting.http.HttpBinder;


/**
 * 
 * @author yankai913@gmail.com
 * @date 2014-4-30
 */
public class ThriftProtocol extends AbstractProxyProtocol {

    public static final int DEFAULT_PORT = 80;

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();

    private final Map<String, ThriftServiceExporter> skeletonMap =
            new ConcurrentHashMap<String, ThriftServiceExporter>();

    private HttpBinder httpBinder;

    private class InternalHandler implements HttpHandler {

        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException,
                ServletException {
            String uri = request.getRequestURI();
            ThriftServiceExporter skeleton = skeletonMap.get(uri);
            if (!request.getMethod().equalsIgnoreCase("POST")) {
                response.setStatus(500);
            }
            else {
                RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
                try {
                    skeleton.handleRequest(request, response);
                }
                catch (Throwable e) {
                    // 这里需要做容错，防止未捕获的异常
                    throw new ServletException(e.getMessage());
                }
            }
        }

    }


    public ThriftProtocol() {
        super(ThriftProtocol.class);
    }


    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }


    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }


    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        String addr = url.getIp() + ":" + url.getPort();
        HttpServer server = serverMap.get(addr);
        if (server == null) {
            server = httpBinder.bind(url, new InternalHandler());
            serverMap.put(addr, server);
        }
        final ThriftServiceExporter httpThriftServiceExporter = new ThriftServiceExporter();
        httpThriftServiceExporter.setServiceInterface(type);
        httpThriftServiceExporter.setService(impl);
        try {
            httpThriftServiceExporter.afterPropertiesSet();
        }
        catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
        final String path = url.getAbsolutePath();
        skeletonMap.put(path, httpThriftServiceExporter);
        return new Runnable() {
            public void run() {
                skeletonMap.remove(path);
            }
        };
    }


    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        return null;
    }

}
