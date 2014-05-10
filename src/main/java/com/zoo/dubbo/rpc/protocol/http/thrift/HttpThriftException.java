package com.zoo.dubbo.rpc.protocol.http.thrift;

/**
 * 
 * @author yankai913@gmail.com
 * @date 2014-5-10
 */

public class HttpThriftException extends Exception {

    private static final long serialVersionUID = 3933204599867966017L;


    public HttpThriftException() {
        super();
    }


    public HttpThriftException(String message) {
        super(message);
    }


    public HttpThriftException(String message, Throwable cause) {
        super(message, cause);
    }


    public HttpThriftException(Throwable cause) {
        super(cause);
    }
}
