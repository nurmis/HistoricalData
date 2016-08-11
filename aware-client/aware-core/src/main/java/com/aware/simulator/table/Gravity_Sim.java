package com.aware.simulator.table;

import com.aware.simulator.AbstractEvent;

import java.util.UUID;

public class Gravity_Sim implements AbstractEvent {

    public int id() { return _id; }
    public long timestamp() { return _timestamp; }
    public UUID device() { return _device; }

    private final int _id;
    private final long _timestamp;
    private final UUID _device;

    public final Double values_0;
    public final Double values_1;
    public final Double values_2;
    public final int accuracy;

    public Gravity_Sim(int id, long timestamp, UUID device, Double values_0, Double values_1, Double values_2, int accuracy) {
        this._id = id;
        this._timestamp = timestamp;
        this._device = device;
        this.values_0 = values_0;
        this.values_1 = values_1;
        this.values_2 = values_2;
        this.accuracy = accuracy;
    }

    public String toString() {
        return "["+id()+"] - ["+timestamp()+"] - ["+device()+"] - ["+ values_0 +"] - ["+ values_1 +"] - ["+ values_2 +"] ["+ accuracy +"]" ;
    }

}
