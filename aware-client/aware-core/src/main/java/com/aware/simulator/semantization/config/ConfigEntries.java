package com.aware.simulator.semantization.config;

public enum ConfigEntries {
    TIMESTAMP("timestamp"),
    WEEK_DAY("weekday"),
    GEOGRAPHICAL("geographical"),
    SPEED("speed"),
    ACTIVITY_NAME("name"),
    ACTIVITY_CONFIDENCE("confidence"),
    RECEIVED_BYTES("received_bytes"),
    SENT_BYTES("sent_bytes"),
    RECEIVED_PACKETS("received_packets"),
    SENT_PACKETS("sent_packets"),
    APP_NAME("name"),
    APP_USAGE("usage_time");

    private String value;

    ConfigEntries(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
