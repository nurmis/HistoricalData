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

package com.aware.simulator;


import android.util.Log;

import com.aware.simulator.table.*;

import java.sql.*;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MysqlDataSource implements DataSource {

    private final Statement statement;

    public MysqlDataSource(String host, int port, String user, String pass, String database) throws SQLException, ClassNotFoundException {
        Class.forName(com.mysql.jdbc.Driver.class.getCanonicalName());
        final Connection connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?user=" + user + "&password=" + pass);
        statement = connection.createStatement();
    }

    private ResultSet query(String table, UUID device, int id, long timestamp, int limit) throws SQLException {
        //System.out.println("-------------RUNNING QUERY (" + table + ",id>" + id + ",lim=" + limit + ")-------------");
        return statement.executeQuery("SELECT * FROM " + table + " WHERE " +
                " _id > " + id + " AND " +
                " timestamp >= " + timestamp + " AND " +
                " device_id = '" + device + "' " +
                " ORDER BY _id ASC " +
                " LIMIT " + limit);
    }

    public Source<Accelerometer_Sim> accelerometer() {
        return new Source<Accelerometer_Sim>() {
            public List<Accelerometer_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<Accelerometer_Sim> rv = new ArrayList<Accelerometer_Sim>();
                try {
                    ResultSet result = query("accelerometer", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new Accelerometer_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getDouble("double_values_0"),
                                result.getDouble("double_values_1"),
                                result.getDouble("double_values_2"),
                                result.getInt("accuracy"),
                                result.getString("label")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
            }
        };
    }

    public Source<ApplicationsForeground_Sim> applicationsForeground() {
        return new Source<ApplicationsForeground_Sim>() {
            public List<ApplicationsForeground_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<ApplicationsForeground_Sim> rv = new ArrayList<ApplicationsForeground_Sim>();
                try {
                    ResultSet result = query("applications_foreground", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new ApplicationsForeground_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getString("package_name"),
                                result.getString("application_name"),
                                result.getBoolean("is_system_app")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
            }
        };
    }

    public Source<Battery_Sim> battery() {
        return new Source<Battery_Sim>() {
            public List<Battery_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<Battery_Sim> rv = new ArrayList<Battery_Sim>();
                try {
                    ResultSet result = query("battery", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new Battery_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getInt("battery_status"),
                                result.getInt("battery_level"),
                                result.getInt("battery_scale"),
                                result.getInt("battery_voltage"),
                                result.getInt("battery_temperature"),
                                result.getInt("battery_adaptor"),
                                result.getInt("battery_health"),
                                result.getString("battery_technology")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
            }
        };
    }

    public Source<BatteryCharges_Sim> batteryCharges() {
        return new Source<BatteryCharges_Sim>() {
            public List<BatteryCharges_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<BatteryCharges_Sim> rv = new ArrayList<BatteryCharges_Sim>();
                try {
                    ResultSet result = query("battery_charges", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new BatteryCharges_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getInt("battery_start"),
                                result.getInt("battery_end"),
                                result.getDouble("double_end_timestamp")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
            }
        };
    }

    public Source<BatteryDischarges_Sim> batteryDischarges() {
        return new Source<BatteryDischarges_Sim>() {
            public List<BatteryDischarges_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<BatteryDischarges_Sim> rv = new ArrayList<BatteryDischarges_Sim>();
                try {
                    ResultSet result = query("battery_discharges", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new BatteryDischarges_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getInt("battery_start"),
                                result.getInt("battery_end"),
                                result.getDouble("double_end_timestamp")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
            }
        };
    }
    public Source<Barometer_Sim> barometer() {
        return new Source<Barometer_Sim>() {
            public List<Barometer_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<Barometer_Sim> rv = new ArrayList<Barometer_Sim>();
                try {
                    ResultSet result = query("barometer", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new Barometer_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getDouble("double_values_0"),
                                result.getInt("accuracy"),
                                result.getString("label")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
                }
        };
    }

    public Source<Gravity_Sim> gravity() {
        return new Source<Gravity_Sim>() {
            public List<Gravity_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<Gravity_Sim> rv = new ArrayList<Gravity_Sim>();
                try {
                    ResultSet result = query("gravity", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new Gravity_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getDouble("double_values_0"),
                                result.getDouble("double_values_1"),
                                result.getDouble("double_values_2"),
                                result.getInt("accuracy")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
            }
        };
    }

    public Source<Gyroscope_Sim> gyroscope() {
        return new Source<Gyroscope_Sim>() {
            public List<Gyroscope_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<Gyroscope_Sim> rv = new ArrayList<Gyroscope_Sim>();
                try {
                    ResultSet result = query("gyroscope", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new Gyroscope_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getDouble("axis_x"),
                                result.getDouble("axis_y"),
                                result.getDouble("axis_z"),
                                result.getInt("accuracy"),
                                result.getString("label")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
            }
        };
    }

    public Source<Locations_Sim> locations() {
        return new Source<Locations_Sim>() {
            public List<Locations_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<Locations_Sim> rv = new ArrayList<Locations_Sim>();
                try {
                    ResultSet result = query("locations", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new Locations_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getDouble("double_latitude"),
                                result.getDouble("double_longitude"),
                                result.getDouble("double_bearing"),
                                result.getDouble("double_speed"),
                                result.getDouble("double_altitude"),
                                result.getString("provider"),
                                result.getDouble("accuracy"),
                                result.getString("label")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
            }
        };
    }

    public Source<SensorWifi_Sim> sensorWifi() {
        return new Source<SensorWifi_Sim>() {
            public List<SensorWifi_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<SensorWifi_Sim> rv = new ArrayList<SensorWifi_Sim>();
                try {
                    ResultSet result = query("sensor_wifi", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new SensorWifi_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getString("mac_address"),
                                result.getString("ssid"),
                                result.getString("bssid")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
            }
        };
    }

    /*public Source<Wifi_Sim> wifi() {
        return new Source<Wifi_Sim>() {
            public List<Wifi_Sim> apply(UUID device, int withIdGreaterThan, long withTimestampGreaterEqualTo, int number) {
                List<Wifi_Sim> rv = new ArrayList<Wifi_Sim>();
                try {
                    ResultSet result = query("wifi", device, withIdGreaterThan, withTimestampGreaterEqualTo, number);
                    while (result.next())
                        rv.add(new Wifi_Sim(
                                result.getInt("_id"),
                                result.getLong("timestamp"),
                                UUID.fromString(result.getString("device_id")),
                                result.getString("bssid"),
                                result.getString("ssid"),
                                result.getString("security"),
                                result.getInt("frequency"),
                                result.getInt("rssi"),
                                result.getString("label")));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rv;
            }
        };
    }*/
}
