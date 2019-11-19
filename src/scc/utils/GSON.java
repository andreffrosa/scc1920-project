package scc.utils;

import java.lang.reflect.Type;

import com.google.gson.Gson;

public class GSON {

    private static Gson gson = new Gson();

    public static String toJson(Object o){
        return gson.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> c){
        return gson.fromJson(json, c);
    }
    
    public static <T> T fromJson(String json, Type type){
        return gson.fromJson(json, type);
    }
    
}
