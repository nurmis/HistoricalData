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

public class Wifi_Sim implements AbstractEvent {

    public int id() { return _id; }
    public long timestamp() { return _timestamp; }
    public UUID device() { return _device; }

    private final int _id;
    private final long _timestamp;
    private final UUID _device;

    public final String bssid;
    public final String ssid;
    public final String security;
    public final String label;
    public final int frequency;
    public final int rssi;

    public Wifi_Sim(int id, long timestamp, UUID device, String bssid, String ssid, String security, int frequency, int rssi, String label) {
        this._id = id;
        this._timestamp = timestamp;
        this._device = device;
        this.bssid = bssid;
        this.ssid = ssid;
        this.security = security;
        this.frequency = frequency;
        this.rssi = rssi;
        this.label = label;
    }

    public String toString() {
        return "["+id()+"] - ["+timestamp()+"] - ["+device()+"] - "+ bssid +" - "+ ssid +" - "+ security +" - ["+ frequency +"] - ["+ rssi +"] - "+ label +"";
    }

}
