/*
 * Copyright 2014 Szymon Bielak <bielakszym@gmail.com> and
 *     Michał Rus <https://michalrus.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aware.simulator.table;

import com.aware.simulator.AbstractEvent;

import java.util.UUID;

public class Battery_Sim implements AbstractEvent {

    public int id() { return _id; }
    public long timestamp() { return _timestamp; }
    public UUID device() { return _device; }

    private final int _id;
    private final long _timestamp;
    private final UUID _device;

    public final int batteryStatus;
    public final int batteryLevel;
    public final int batteryScale;
    public final int batteryVoltage;
    public final int batteryTemperature;
    public final int batteryAdaptor;
    public final int batteryHealth;
    public final String batteryTechnology;


    public Battery_Sim(int id, long timestamp, UUID device, int batteryStatus, int batteryLevel, int batteryScale,
                   int batteryVoltage, int batteryTemperature, int batteryAdaptor, int health, String technology) {

        this._id = id;
        this._timestamp = timestamp;
        this._device = device;
        this.batteryStatus = batteryStatus;
        this.batteryLevel = batteryLevel;
        this.batteryScale = batteryScale;
        this.batteryVoltage = batteryVoltage;
        this.batteryTemperature = batteryTemperature;
        this.batteryAdaptor = batteryAdaptor;
        this.batteryHealth = health;
        this.batteryTechnology = technology;
    }

    public String toString(){
        return "["+id()+"] - ["+timestamp()+"] - ["+device()+"] - ["+ batteryStatus +"] - ["+ batteryLevel +"] - ["+ batteryScale +"]" +
                " - ["+ batteryVoltage +"] - ["+ batteryTemperature + "] - ["+ batteryAdaptor +"] - ["+ batteryHealth +"] - "+ batteryTechnology +"";
    }

}
