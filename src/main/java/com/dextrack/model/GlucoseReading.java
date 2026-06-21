package com.dextrack.model;

import java.time.Instant;

public class GlucoseReading {
    public enum Trend {
        NONE("—"),
        DOUBLE_UP("↑↑"),
        SINGLE_UP("↑"),
        FORTY_FIVE_UP("↗"),
        FLAT("→"),
        FORTY_FIVE_DOWN("↘"),
        SINGLE_DOWN("↓"),
        DOUBLE_DOWN("↓↓"),
        NOT_COMPUTABLE("?"),
        RATE_OUT_OF_RANGE("~");

        public final String arrow;
        Trend(String arrow) { this.arrow = arrow; }
    }

    private final int value;
    private final Instant timestamp;
    private final Trend trend;

    public GlucoseReading(int value, Instant timestamp, Trend trend) {
        this.value = value;
        this.timestamp = timestamp;
        this.trend = trend;
    }

    public int getValue() { return value; }
    public Instant getTimestamp() { return timestamp; }
    public Trend getTrend() { return trend; }
}
