package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;


/**
 * 
 * @author yankai913@gmail.com
 * @date 2014-5-4
 */
public class ThriftTransport extends TTransport {
    private InputStream input;
    private OutputStream output;


    public ThriftTransport(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }


    @Override
    public boolean isOpen() {
        // Buffer is always open
        return true;
    }


    @Override
    public void open() throws TTransportException {
        // Buffer is always open
    }


    @Override
    public void close() {
        try {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public int read(byte[] buffer, int offset, int length) throws TTransportException {
        try {
            int readableBytes = input.available();
            int bytesToRead = length > readableBytes ? readableBytes : length;
            input.read(buffer, offset, bytesToRead);
            return bytesToRead;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }


    @Override
    public void write(byte[] buffer, int offset, int length) throws TTransportException {
        try {
            output.write(buffer, offset, length);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public InputStream getInput() {
        return input;
    }


    public OutputStream getOutput() {
        return output;
    }
}
