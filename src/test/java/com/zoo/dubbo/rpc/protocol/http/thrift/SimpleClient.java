package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.rpc.RpcInvocation;


public class SimpleClient {
    public static void main(String[] args) throws Exception {
        InputStream is = null;
        OutputStream os = null;
        try {
            String serviceName = HelloService.Iface.class.getName();
            String requestUrl = "http://localhost:80/" + serviceName;
            // serialize
            HttpExecutor httpExecutor = new HttpExecutor();
            RpcInvocation rpcInvocation = new RpcInvocation();
            rpcInvocation.setAttachment(Constants.INTERFACE_KEY, serviceName);
            rpcInvocation.setArguments(new Object[] { "hello world" });
            rpcInvocation.setParameterTypes(new Class<?>[] { String.class });
            Method method =
                    HelloService.Client.class.getMethod("getString", rpcInvocation.getParameterTypes());
            rpcInvocation.setMethodName(method.getName());
            byte[] requestBody = HttpThriftTool.serialize(rpcInvocation);
            // http connection
            URL url = new URL(requestUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-thrift");
            con.setRequestProperty("Content-Length", String.valueOf(requestBody.length));
            //extra header
            con.setRequestProperty("bomb", "three");
            //write
            os = con.getOutputStream();
            os.write(requestBody);
            os.flush();
            is = con.getInputStream();
            Object result = httpExecutor.parseResponseInput(is, rpcInvocation);
            System.out.println(result);
            System.out.println("end");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }

    }
}
