package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.springframework.util.StringUtils;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;


/**
 * ThreadSafe, Singleton
 * @author yankai913@gmail.com
 * @date 2014-5-6
 */
public class HttpExecutor {

    public static final String CONTENT_TYPE_SERIALIZED_OBJECT = "application/x-thrift";

    public static final String HTTP_METHOD_POST = "POST";

    public static final String HTTP_HEADER_ACCEPT_LANGUAGE = "Accept-Language";

    public static final String HTTP_HEADER_ACCEPT_ENCODING = "Accept-Encoding";

    public static final String HTTP_HEADER_CONTENT_ENCODING = "Content-Encoding";

    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

    public static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";

    public static final String ENCODING_GZIP = "gzip";

    public static final int SERIALIZED_INVOCATION_BYTE_ARRAY_INITIAL_SIZE = 1024;

    // add new TMessageType instead of Response.CLIENT_TIMEOUT,
    // Response.SERVER_TIMEOUT
    public static final byte T_CLIENT_TIMEOUT = 127;

    public static final byte T_SERVER_TIMEOUT = 126;

    protected String contentType = CONTENT_TYPE_SERIALIZED_OBJECT;

    protected boolean acceptGzipEncoding = true;

    public HttpExecutor() {
        
    }
    
    protected void setAcceptGzipEncoding(boolean acceptGzipEncoding) {
        this.acceptGzipEncoding = acceptGzipEncoding;
    }


    public boolean isAcceptGzipEncoding() {
        return this.acceptGzipEncoding;
    }


    protected HttpURLConnection openConnection(String url) throws Exception {
        URLConnection urlConnection = new java.net.URL(url).openConnection();
        if (!(urlConnection instanceof HttpURLConnection)) {
            throw new IOException(" URL = [" + url + "] is not an HTTP URL");
        }
        HttpURLConnection con = (HttpURLConnection) urlConnection;
        con.setDoOutput(true);
        con.setRequestMethod(HTTP_METHOD_POST);
        con.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, contentType);
        // TODO language
        con.setRequestProperty(HTTP_HEADER_ACCEPT_LANGUAGE, StringUtils.toLanguageTag(Locale.getDefault()));
        if (isAcceptGzipEncoding()) {
            con.setRequestProperty(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
        }
        return con;
    }


    protected boolean isGzipResponse(HttpURLConnection con) {
        String encodingHeader = con.getHeaderField(HTTP_HEADER_CONTENT_ENCODING);
        return (encodingHeader != null && encodingHeader.toLowerCase().indexOf(ENCODING_GZIP) != -1);
    }

    String convertToService(com.alibaba.dubbo.common.URL url) {
        //TODO GET serviceUrl
        return "";
    }


    HttpURLConnection getConnection(com.alibaba.dubbo.common.URL url) throws Exception {
        String serviceUrl = convertToService(url);
        URLConnection urlConnection = new java.net.URL(serviceUrl).openConnection();
        if (!(urlConnection instanceof HttpURLConnection)) {
            throw new IOException(" URL = [" + url + "] is not an HTTP URL");
        }
        HttpURLConnection con = (HttpURLConnection) urlConnection;
        con.setDoOutput(true);
        con.setRequestMethod(HTTP_METHOD_POST);
        con.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, contentType);
        // TODO CONTENT-LENGTH
        // TODO LANGUAGE
        con.setRequestProperty(HTTP_HEADER_ACCEPT_LANGUAGE, StringUtils.toLanguageTag(Locale.getDefault()));
        if (isAcceptGzipEncoding()) {
            con.setRequestProperty(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
        }
        //get parameter from com.alibaba.dubbo.common.URL
        con.setReadTimeout(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
        con.setConnectTimeout(url.getParameter(Constants.CONNECT_TIMEOUT_KEY,
            Constants.DEFAULT_CONNECT_TIMEOUT));

        return con;
    }


    public Object syncExecuteRequest(com.alibaba.dubbo.common.URL url, RpcInvocation rpcInvocation)
            throws Exception {
        HttpURLConnection con = getConnection(url);
        OutputStream outputStream = con.getOutputStream();
        // serialize rpcInvocation
        byte[] requestBody = HttpThriftTool.serialize(rpcInvocation);
        con.setRequestProperty(HTTP_HEADER_CONTENT_LENGTH, String.valueOf(requestBody.length));
        outputStream.write(requestBody);
        outputStream.flush();
        InputStream is = null;
        if (isGzipResponse(con)) {
            is = new GZIPInputStream(con.getInputStream());
        }
        else {
            is = con.getInputStream();
        }
        // parse response，这里可以尝试转异步，暂时这里先同步
        Object value = parseResponseInput(is, rpcInvocation);
        return value;
    }


    Object parseResponseInput(InputStream inputStream, RpcInvocation rpcInvocation) throws Exception {
        String serviceName = rpcInvocation.getAttachment(Constants.PATH_KEY);
        String methodName = rpcInvocation.getMethodName();
        TProtocol iprot = HttpThriftTool.newProtocol(inputStream, null, serviceName);
        TMessage msg = iprot.readMessageBegin();
        if (msg.type == TMessageType.EXCEPTION) {
            TApplicationException x = TApplicationException.read(iprot);
            iprot.readMessageEnd();
            throw x;
        }
        if (msg.type == T_CLIENT_TIMEOUT || msg.type == T_SERVER_TIMEOUT) {
            // throw new TimeoutException(msg.type == T_SERVER_TIMEOUT,
            // channel, getTimeoutMessage(true));
            throw new HttpThriftException("client is timeout");
        }
        Class<?> clazz = HttpThriftTool.getTBaseClass_result(serviceName, methodName);
        TBase<?, ?> _result = HttpThriftTool.getTBaseObject(clazz, null, null);
        _result.read(iprot);
        Object value = HttpThriftTool.getResult(_result);
        RpcResult result = new RpcResult(value);
        return result;
    }
}
