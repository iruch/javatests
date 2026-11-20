package ru.iruch.javatests.history;

final class Durations {
    private Durations() {}

    static String format(int totalSeconds) {
        int seconds = Math.max(0, totalSeconds);
        int hours = seconds / 3600;
        int minutes = seconds % 3600 / 60;
        int rest = seconds % 60;
        if (hours > 0) return hours + " ч " + minutes + " мин";
        if (minutes > 0) return minutes + " мин " + rest + " с";
        return rest + " с";
    }
}
