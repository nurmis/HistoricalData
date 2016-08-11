package com.aware.simulator.table;

import com.aware.simulator.AbstractEvent;

import java.util.UUID;

public class Barometer_Sim implements AbstractEvent {

    public int id() { return _id; }
    public long timestamp() { return _timestamp; }
    public UUID device() { return _device; }

    private final int _id;
    private final long _timestamp;
    private final UUID _device;

    public final Double values_0;
    public final int accuracy;
    public final String label;

    public Barometer_Sim(int id, long timestamp, UUID device, Double values_0, int accuracy, String label) {
        this._id = id;
        this._timestamp = timestamp;
        this._device = device;
        this.values_0 = values_0;
        this.accuracy = accuracy;
        this.label = label;

    }

    public String toString() {
        return "["+id()+"] - ["+timestamp()+"] - ["+device()+"] - ["+ values_0 +"] - ["+ accuracy +"] - " + label + "";
    }

}
