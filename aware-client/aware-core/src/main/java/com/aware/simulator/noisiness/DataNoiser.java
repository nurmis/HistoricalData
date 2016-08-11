package com.aware.simulator.noisiness;

import com.aware.simulator.table.*;
import com.aware.simulator.*;


public interface DataNoiser {

    interface Noiser<T extends AbstractEvent> {
        T apply(T event);
    }

    public Noiser<Accelerometer_Sim> accelerometer();
    public Noiser<ApplicationsForeground_Sim> applicationsForeground();
    public Noiser<Battery_Sim> battery();
    public Noiser<BatteryCharges_Sim> batteryCharges();
    public Noiser<BatteryDischarges_Sim> batteryDischarges();
    public Noiser<Barometer_Sim> barometer();
    public Noiser<Gravity_Sim> gravity();
    public Noiser<Gyroscope_Sim> gyroscope();
    public Noiser<Locations_Sim> locations();
    public Noiser<SensorWifi_Sim> sensorWifi();
    //public Noiser<Wifi_Sim> wifi();

}
