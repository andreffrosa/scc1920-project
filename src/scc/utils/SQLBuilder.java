package scc.utils;

public class SQLBuilder {

    private static final String SELECT = "SELECT ";

    public static String select(String[] fields){
        String toReturn = SELECT;

        for (String field: fields) {
            toReturn = toReturn + " " + field +", ";
        }

        return toReturn;
    }

    public static String select(String field){
        return String.format("%s %s", SELECT, field);
    }

}
