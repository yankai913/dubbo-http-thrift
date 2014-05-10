package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.rpc.RpcInvocation;


/**
 * 
 * @author yankai913@gmail.com
 * @date 2014-5-10
 */
public class HttpThriftTool {

    public static final ConcurrentMap<String, Constructor<?>> methodSign2constructorMap =
            new ConcurrentHashMap<String, Constructor<?>>();

    public static final ConcurrentMap<String, Class<?>> cachedClassMap =
            new ConcurrentHashMap<String, Class<?>>();

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    public static final String _ARGS = "_args";

    public static final String _RESULT = "_result";


    public static TBase<?, ?> getTBaseObject(Class<?> clazz, Class<?>[] parameterTypes, Object[] initArgs)
            throws Exception {
        StringBuilder keyBuf = new StringBuilder(clazz.getName());
        if (parameterTypes != null) {
            for (Class<?> t : parameterTypes) {
                keyBuf.append(".").append(t.getName());
            }
        }
        else {
            keyBuf.append(".").append(parameterTypes);
        }
        String key = keyBuf.toString();
        Constructor<?> constructor = methodSign2constructorMap.get(key);
        if (constructor == null) {
            methodSign2constructorMap.putIfAbsent(key, clazz.getConstructor(parameterTypes));
            constructor = methodSign2constructorMap.get(key);
        }
        return (TBase<?, ?>) constructor.newInstance(initArgs);
    }


    public static Class<?> getTBaseClass_args(String serviceName, String methodName) throws Exception {
        String argsServiceName = getArgsClassName(serviceName, methodName, _ARGS);
        return getTBaseClass(argsServiceName);
    }


    public static Class<?> getTBaseClass_result(String serviceName, String methodName) throws Exception {
        String argsServiceName = getArgsClassName(serviceName, methodName, _RESULT);
        return getTBaseClass(argsServiceName);
    }


    public static byte[] serialize(RpcInvocation rpcInvocation) throws Exception {
        String serviceName = rpcInvocation.getAttachment(Constants.PATH_KEY);
        // TODO ClassLoader
        Class<?>[] parameterTypes = rpcInvocation.getParameterTypes();
        Object[] initArgs = rpcInvocation.getArguments();
        // SEE libthrift-0.9.1 new Feature ==>
        // https://issues.apache.org/jira/browse/THRIFT-563
        String newMethodName = serviceName + TMultiplexedProtocol.SEPARATOR + rpcInvocation.getMethodName();
        // TODO 这里可以优化
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TProtocol oprot = newProtocol(null, baos, serviceName);
        // write 1
        oprot.writeMessageBegin(new TMessage(newMethodName, TMessageType.CALL, SEQUENCE.incrementAndGet()));
        // get serviceMethodArgsClassName
        Class<?> argsServiceClazz = getTBaseClass_args(serviceName, rpcInvocation.getMethodName());
        TBase<?, ?> _args = getTBaseObject(argsServiceClazz, parameterTypes, initArgs);
        // write 2
        _args.write(oprot);
        // TODO 这里可以再优化，减少内存复制
        return baos.toByteArray();
    }


    public static TProtocol newProtocol(InputStream inputStream, OutputStream outputStream, String serviceName) {
        TIOStreamTransport trans = new TIOStreamTransport(inputStream, outputStream);
        TBinaryProtocol protocol = new TBinaryProtocol(trans);
        return protocol;
    }


    public static Class<?> getTBaseClass(String argsServiceName) throws Exception {
        Class<?> clazz = cachedClassMap.get(argsServiceName);
        if (clazz == null) {
            cachedClassMap.putIfAbsent(argsServiceName, Class.forName(argsServiceName));
            clazz = cachedClassMap.get(argsServiceName);
        }
        return clazz;
    }


    public static String getArgsClassName(String serviceName, String methodName, String tag) {
        return serviceName.substring(0, serviceName.lastIndexOf("$")) + "$" + methodName + tag;
    }


    public static void dserialize(TBase base, byte[] bytes) throws Exception {
        TDeserializer tDeserializer = new TDeserializer();
        // TODO 反序列化
    }


    public static Object getResult(TBase<?, ?> _result) throws Exception {
        try {
            Field success = _result.getClass().getDeclaredField("success");// hard
                                                                           // code
            success.setAccessible(true);
            Object object = success.get(_result);
            if (object != null) {
                return object;
            }
        }
        catch (NoSuchFieldException e) {// 没有success字段说明方法是void
            return void.class;
        }
        Field[] fields = _result.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (f.getModifiers() == Modifier.PUBLIC && TBase.class.isAssignableFrom(f.getType())
                    && Exception.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                if (f.get(_result) != null) {
                    throw (Exception) f.get(_result);
                }
            }
        }
        throw new TApplicationException(TApplicationException.MISSING_RESULT, "unknown result");
    }


    public static void createErrorTMessage(TProtocol oprot, String methodName, int id, String errMsg)
            throws Exception {
        TMessage tmessage = new TMessage(methodName, TMessageType.EXCEPTION, id);
        oprot.writeMessageBegin(tmessage);
        oprot.writeMessageEnd();
        TApplicationException ex = new TApplicationException(TApplicationException.INTERNAL_ERROR, errMsg);
        try {
            ex.write(oprot);
        }
        catch (TException e) {
            e.printStackTrace();
        }
    }
}
