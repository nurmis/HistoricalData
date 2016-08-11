package com.aware.simulator.table;

import com.aware.simulator.AbstractEvent;

import java.util.UUID;

public class Accelerometer_Sim implements AbstractEvent{

    public int id() { return _id; }
    public long timestamp() { return _timestamp; }
    public UUID device() { return _device; }

    private final int _id;
    private final long _timestamp;
    private final UUID _device;

    public final double doubleValues0;
    public final double doubleValues1;
    public final double doubleValues2;
    public final int accuracy;
    public final String label;

    public Accelerometer_Sim(int id, long timestamp, UUID device, double doubleValues0, double doubleValues1, double doubleValues2, int accuracy, String label) {
        this._id = id;
        this._timestamp = timestamp;
        this._device = device;
        this.doubleValues0 = doubleValues0;
        this.doubleValues1 = doubleValues1;
        this.doubleValues2 = doubleValues2;
        this.accuracy = accuracy;
        this.label = label;
    }

    public String toString() {
        return "["+id()+"] - ["+timestamp()+"] - ["+device()+"] - ["+ doubleValues0 +"] - ["+ doubleValues1 +"] - ["+ doubleValues2 +"] - ["+ accuracy +"] - " + label + "";
    }
}
