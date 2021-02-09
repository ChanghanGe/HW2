package edu.utexas.cs.alr.satsolver;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class Logger {
    private static boolean showDebug = false;

    public static void setShowDebug(boolean showDebug) {
        Logger.showDebug = showDebug;
    }

    public static boolean isDebugging() {
        return showDebug;
    }

    public static void log(Object... objs) {
        List<String> ret = Arrays.stream(objs).map(Object::toString).collect(toList());
        System.out.println(String.join(" ", ret));
    }

    public static void debug(Object... objs) {
        if (showDebug) log(objs);
    }
}