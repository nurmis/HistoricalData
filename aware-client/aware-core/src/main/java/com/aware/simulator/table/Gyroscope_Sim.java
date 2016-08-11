package com.aware.simulator.table;

import com.aware.simulator.AbstractEvent;

import java.util.UUID;

public class Gyroscope_Sim implements AbstractEvent {

    public int id() { return _id; }
    public long timestamp() { return _timestamp; }
    public UUID device() { return _device; }

    private final int _id;
    private final long _timestamp;
    private final UUID _device;

    public final Double axis_x;
    public final Double axis_y;
    public final Double axis_z;
    public final int accuracy;
    public final String label;

    public Gyroscope_Sim(int id, long timestamp, UUID device, Double axis_x, Double axis_y, Double axis_z, int accuracy, String label) {
        this._id = id;
        this._timestamp = timestamp;
        this._device = device;
        this.axis_x = axis_x;
        this.axis_y = axis_y;
        this.axis_z= axis_z;
        this.accuracy = accuracy;
        this.label = label;
    }

    public String toString() {
        return "["+id()+"] - ["+timestamp()+"] - ["+device()+"] - ["+ axis_x +"] - ["+ axis_y +"] - ["+ axis_z +"] ["+ accuracy +"] "+ label + "";
    }

}
