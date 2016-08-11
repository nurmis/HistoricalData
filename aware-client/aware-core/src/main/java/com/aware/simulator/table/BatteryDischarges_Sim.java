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

public class BatteryDischarges_Sim implements AbstractEvent {

    public int id() { return _id; }
    public long timestamp() { return _timestamp; }
    public UUID device() { return _device; }

    private final int _id;
    private final long _timestamp;
    private final UUID _device;

    public final int batteryStart;
    public final int batteryEnd;
    public final double doubleEndTimestamp;

    public BatteryDischarges_Sim(int id, long timestamp, UUID device, int batteryStart, int batteryEnd, double doubleEndTimestamp) {
        this._id = id;
        this._timestamp = timestamp;
        this._device = device;
        this.batteryStart = batteryStart;
        this.batteryEnd = batteryEnd;
        this.doubleEndTimestamp = doubleEndTimestamp;
    }

    public String toString(){
        return "["+id()+"] - ["+timestamp()+"] - ["+device()+"] - ["+ batteryStart +"] - ["+ batteryEnd +"] - ["+ doubleEndTimestamp +"]";
    }
}
