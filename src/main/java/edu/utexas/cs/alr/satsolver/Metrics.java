package edu.utexas.cs.alr.satsolver;

import java.util.HashMap;
import java.util.Map;

public class Metrics {
    public enum Key {
        TOTAL, UNIT_PROPAGATION, BRANCH_PICKING, CONFLICT_ANALYSIS
    }

    private static Map<Key, Integer> counterMap;
    private static Map<Key, Long> elapsedTimeMap, startTimeMap;
    private static boolean enabled = false;

    static {
        counterMap = new HashMap<>();
        elapsedTimeMap = new HashMap<>();
        startTimeMap = new HashMap<>();
    }

    public static void setEnabled(boolean isEnabled) {
        Metrics.enabled = isEnabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void startTimer(Key key) {
        if (!enabled) return;
        startTimeMap.put(key, System.currentTimeMillis());
    }

    public static void stopTimer(Key key) {
        if (!enabled) return;
        Long startTime = startTimeMap.get(key);
        if (startTime == null) {
            Logger.log(String.format("WARNING: stopwatch has not been started for key '%s'.", key));
            return;
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        elapsedTimeMap.merge(key, elapsedTime, Long::sum);
        startTimeMap.remove(key);
    }

    public static long getElapsedTimeMillis(Key key) {
        return elapsedTimeMap.getOrDefault(key, -1L);
    }

    public static void incrementCounter(Key key) {
        counterMap.merge(key, 1, Integer::sum);
    }

    public static int getCounter(Key key) {
        return counterMap.getOrDefault(key, -1);
    }

}
