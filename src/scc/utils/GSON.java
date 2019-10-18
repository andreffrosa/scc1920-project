package scc.utils;

import com.google.gson.Gson;

public class GSON {

    private static Gson gson = new Gson();

    public static String toJson(Object o){
        return gson.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> c){
        return gson.fromJson(json, c);
    }
}
