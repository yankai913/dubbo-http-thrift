package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.remoting.RemoteAccessException;

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
public class HttpThriftProtocol extends AbstractProxyProtocol {

    public static final String NAME = "httpt";

    public static final int DEFAULT_PORT = 80;

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();

    private final Map<String, HttpThriftServiceExporter> skeletonMap =
            new ConcurrentHashMap<String, HttpThriftServiceExporter>();

    private HttpBinder httpBinder;

    private class InternalHandler implements HttpHandler {

        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException,
                ServletException {
            String uri = request.getRequestURI();
            HttpThriftServiceExporter skeleton = skeletonMap.get(uri);
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


    public HttpThriftProtocol() {
        super(HttpThriftProtocol.class);
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
        final HttpThriftServiceExporter httpThriftServiceExporter = new HttpThriftServiceExporter();
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


    @SuppressWarnings("unchecked")
    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        HttpThriftServiceFactoryBean httpThriftServiceFactoryBean = new HttpThriftServiceFactoryBean();
        httpThriftServiceFactoryBean.setUrl(url);
        httpThriftServiceFactoryBean.setServiceInterface(type);
        HttpExecutor httpExecutor = new HttpExecutor();
        httpThriftServiceFactoryBean.setHttpExecutor(httpExecutor);
        return (T) httpThriftServiceFactoryBean.getObject();
    }


    protected int getErrorCode(Throwable e) {
        if (e instanceof RemoteAccessException) {
            e = e.getCause();
        }
        if (e != null) {
            Class<?> cls = e.getClass();
            // 是根据测试Case发现的问题，对RpcException.setCode进行设置
            if (SocketTimeoutException.class.equals(cls)) {
                return RpcException.TIMEOUT_EXCEPTION;
            }
            else if (IOException.class.isAssignableFrom(cls)) {
                return RpcException.NETWORK_EXCEPTION;
            }
            else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                return RpcException.SERIALIZATION_EXCEPTION;
            }
        }
        return super.getErrorCode(e);
    }
}
