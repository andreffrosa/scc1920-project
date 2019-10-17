package scc.storage;

import com.google.gson.Gson;

public class GSON {

    private static Gson gson = new Gson();

    public static String toJson(Object o){
        return gson.toJson(o);
    }
}
