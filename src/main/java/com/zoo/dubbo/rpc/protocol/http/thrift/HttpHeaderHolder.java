package com.zoo.dubbo.rpc.protocol.http.thrift;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author yankai913@gmail.com
 * @date 2014-5-11
 */
public class HttpHeaderHolder {

    private static ThreadLocal<Map<String, String>> httpHeaderHolder = new ThreadLocal<Map<String, String>>();


    public static Map<String, String> get() {
        Map<String, String> map = httpHeaderHolder.get();
        // clear
        set(null);
        return map;
    }


    public static void set(Map<String, String> map) {
        httpHeaderHolder.set(map);
    }


    public static void set(String key, String value) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(key, value);
        set(map);
    }

}
