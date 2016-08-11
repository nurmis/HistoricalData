package com.aware.simulator.noisiness;

import com.aware.simulator.table.*;

import java.util.Random;

public final class SimpleDataNoiser implements DataNoiser {
    private final long timestamp;
    private final long duration;
    private final double noiseProbability;
    private final double lower;
    private final double upper;
    private Random random = new Random();
    private Random randomInt = new Random();

    public SimpleDataNoiser(long timestamp, long duration, double noiseProbability, double lower, double upper) {
        this.timestamp=timestamp;
        this.duration=duration;
        this.noiseProbability=noiseProbability;
        this.lower=lower;
        this.upper=upper;
    }

    public Noiser<Accelerometer_Sim> accelerometer() {
        return new Noiser<Accelerometer_Sim>() {
            public Accelerometer_Sim apply(Accelerometer_Sim rv) {

                return rv;
            }
        };
    }

    public Noiser<ApplicationsForeground_Sim> applicationsForeground() {
        return new Noiser<ApplicationsForeground_Sim>() {
            public ApplicationsForeground_Sim apply(ApplicationsForeground_Sim rv) {
                return rv;
            }
        };
    }

    //int batteryVoltage, int batteryTemperature
    public Noiser<Battery_Sim> battery() {
        return new Noiser<Battery_Sim>() {
            public Battery_Sim apply(Battery_Sim rv) {
                if(rv.timestamp()>=timestamp && rv.timestamp()<=timestamp+duration && random.nextDouble()<noiseProbability){

            }
                return rv;
            }
        };
    }

    public Noiser<BatteryCharges_Sim> batteryCharges() {
        return new Noiser<BatteryCharges_Sim>() {
            public BatteryCharges_Sim apply(BatteryCharges_Sim rv) {
                return rv;
            }
        };
    }

    public Noiser<BatteryDischarges_Sim> batteryDischarges() {
        return new Noiser<BatteryDischarges_Sim>() {
            public BatteryDischarges_Sim apply(BatteryDischarges_Sim rv) {
                return rv;
            }
        };
    }
    public Noiser<Barometer_Sim> barometer(){
        return new Noiser<Barometer_Sim>() {
            public Barometer_Sim apply(Barometer_Sim rv) {
                return rv;
            }
        };
    }
    public Noiser<Gravity_Sim> gravity(){
        return new Noiser<Gravity_Sim>() {
            public Gravity_Sim apply(Gravity_Sim rv) {
                return rv;
            }
        };
    }

    public Noiser<Gyroscope_Sim> gyroscope(){
        return new Noiser<Gyroscope_Sim>() {
            public Gyroscope_Sim apply(Gyroscope_Sim rv) {
                return rv;
            }
        };
    }

    public Noiser<Locations_Sim> locations() {
        return new Noiser<Locations_Sim>() {
            public Locations_Sim apply(Locations_Sim rv) {
                if(rv.timestamp()>=timestamp && rv.timestamp()<=timestamp+duration && random.nextDouble()<noiseProbability) {
                    double latitude = rv.doubleLatitude*(1+(random.nextInt(2)*2-1)*(random.nextDouble()*(upper-lower)+lower));
                    latitude=Math.min(Math.max(latitude, -90.0), 90.0);
                    double longitude = rv.doubleLongitude*(1+(random.nextInt(2)*2-1)*(random.nextDouble()*(upper-lower)+lower));
                    longitude=Math.min(Math.max(longitude, -180.0), 180.0);
                    return new Locations_Sim(rv.id(), rv.timestamp(), rv.device(), latitude, longitude,
                            rv.doubleBearing, rv.doubleSpeed, rv.doubleAltitude, rv.provider, rv.accuracy, rv.label);
                }

                return rv;
            }
        };
    }
    public Noiser<SensorWifi_Sim> sensorWifi() {
        return new Noiser<SensorWifi_Sim>() {
            public SensorWifi_Sim apply(SensorWifi_Sim rv) {
                return rv;
            }
        };
    }

    public Noiser<Wifi_Sim> wifi() {
        return new Noiser<Wifi_Sim>() {
            public Wifi_Sim apply(Wifi_Sim rv) {
                return rv;
            }
        };
    }

}
