package com.aware.simulator;

import com.aware.Battery;
import com.aware.simulator.noisiness.*;

import com.aware.simulator.table.*;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import com.aware.Aware;
import com.aware.utils.Aware_Plugin;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import com.aware.Accelerometer;
import com.aware.Gravity;

/**
 * Created by Comet on 16/03/16.
 */
public class ContextDataReading extends Service {
    //Battery_Sensor.Battery_Service batservice;
    Accelerometer accService;
    Aware awareService;
    //Barometer barometerservice;
    //Gravity gravityservice;
    //Gyroscope gyroscopeservice;
    //Locations locationsservice;
    //WiFi wifiservice;*/
    boolean bound = false;

    private ServiceConnection sensorConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            //Battery_Sensor.Battery_Service.OnReceiveBinder bBinder = (Battery_Sensor.Battery_Service.OnReceiveBinder) service;
            accService = Accelerometer.getService();
            awareService = Aware.getService();
            //batservice = bBinder.getService();
            //barometerservice = Barometer.getService();
            //gravityservice = Gravity.getService();
            //gyroscopeservice = Gyroscope.getService();
            //locationsservice = Locations.getService();
            //wifiservice = WiFi.getService();*/
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    public void onCreate() {
        Log.d("Tester", "at ContextDataReading onCreate");

        Intent intent = new Intent(this, Accelerometer.class);
        startService(intent);
        bindService(intent, sensorConnection, Context.BIND_AUTO_CREATE);
        //Context awareContext = getApplicationContext();
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        try {

            final DataSource ds = new MysqlDataSource("awareframework.com", 3306, "", "", "");
            final DataNoiser dn = new SimpleDataNoiser(1391062684000L, 3600000L, 0.3, 0.1, 0.4);
            final AwareSimulator sim = new AwareSimulator(ds, dn, 1459238545303L, UUID.fromString("756b94a0-538e-49ff-bf64-b8f31636e1e3"));

            sim.setSpeed(1.0);
            Log.d("Tester", "after setSpeed");

            sim.accelerometer.addListener(new AwareSimulator.Listener<Accelerometer_Sim>() {
                public void onEvent(Accelerometer_Sim event) {
                    float[] values = new float[3];
                    values[0] = (float) event.doubleValues0;
                    values[1] = (float) event.doubleValues1;
                    values[2] = (float) event.doubleValues2;
                    SensorEvent accEvent = CreateAccelerometerEvent(values, event);
                    Context context = getApplicationContext();

                    if (bound) {
                        Log.d("Tester", "accEvent timestamp: " + accEvent.timestamp);
                        accService.onSensorChanged(accEvent);
                        /*try{
                            Log.d("Tester", "at if");
                            Context accContext = createPackageContext("com.aware.phone", Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
                            Log.d("Tester", "after createPackageContext");
                            Class acc = accContext.getClassLoader().loadClass("com.aware.phone.Aware_Client");
                            Log.d("Tester", "after loadClass");
                            //Class params[] = {SensorEvent.class};
                            Class[] cArg = new Class[1];
                            cArg[0] = SensorEvent.class;
                            Method oSC = acc.getDeclaredMethod("AccelerometerTestInterface", cArg);
                            Log.d("Tester", "after getMethod");
                            oSC.invoke(accContext, cArg);
                            Log.d("Tester", "Accelerometer val 0" + accEvent.values);
                        } catch (PackageManager.NameNotFoundException nnfe){
                            Log.d("Tester", "NNFE");
                        } catch (ClassNotFoundException e) {
                            Log.d("Tester", "CNFE");
                        } catch (NoSuchMethodException e){
                            e.printStackTrace();
                            Log.d("Tester", "NSME");
                        } catch (IllegalAccessException e){
                            Log.d("Tester", "IAE");
                        } catch (InvocationTargetException e){
                            Log.d("Tester", "ITE");
                        }*/
                    }
                }
            });

            /*sim.applicationsForeground.addListener(new AwareSimulator.Listener<ApplicationsForeground_Sim>() {
                public void onEvent(ApplicationsForeground_Sim event) {
                    Log.d("Tester", "ApplicationsForeground: " + event);
                    //AccessibilityEvent aEvent = CreateForegroundEvent(event);
                }
            });*/

            sim.battery.addListener(new AwareSimulator.Listener<Battery_Sim>() {
                public void onEvent(Battery_Sim event) {
                    //Log.d("Tester", "Battery_Sim:" + event);
                    /*Intent intent = new Intent();

                    intent.setAction(Intent.ACTION_BATTERY_CHANGED);
                    intent.putExtra("TIMESTAMP", event.timestamp());
                    intent.putExtra("DEVICE_ID", Objects.toString(event.device()));
                    intent.putExtra("STATUS", event.batteryStatus);
                    intent.putExtra("LEVEL", event.batteryLevel);
                    intent.putExtra("SCALE", event.batteryScale);
                    intent.putExtra("VOLTAGE", event.batteryVoltage);
                    intent.putExtra("EXTRA_TEMPERATURE", event.batteryTemperature);
                    intent.putExtra("EXTRA_PLUGGED", event.batteryAdaptor);
                    intent.putExtra("EXTRA_HEALTH", event.batteryHealth);
                    intent.putExtra("EXTRA_TECHNOLOGY", event.batteryTechnology);*/

                    /*FakeBatteryManager fakeManager = new FakeBatteryManager();
                    fakeManager.setProperties(event.batteryStatus,
                            event.batteryLevel, event.batteryScale, event.batteryVoltage,
                            event.batteryTemperature, event.batteryAdaptor, event.batteryHealth,
                            event.batteryTechnology);
                    //Log.d("Tester", "fakemanagerin status:" + fakeManager.EXTRA_STATUS);
                    intent.putExtra("FAKEDATA", true);
                    intent.putExtra("FAKEMANAGER", fakeManager);*/

                    //BatteryManager fakeManager = CreateBatteryManager(event);
                    //Log.d("Tester: ", "Fakemanager: " + fakeManager.toString());

                    if (bound) {
                        //batservice.onReceive(getApplicationContext(), intent);
                    }
                }
            });

            sim.batteryCharges.addListener(new AwareSimulator.Listener<BatteryCharges_Sim>() {
                public void onEvent(BatteryCharges_Sim event) {
                    Intent intent = new Intent();
                    //Log.d("Tester", "CHARGE");

                    intent.setAction(Intent.ACTION_POWER_CONNECTED);
                    if (bound) {
                        //batservice.onReceive(getApplicationContext(), intent);
                    }
                }
            });

            sim.batteryDischarges.addListener(new AwareSimulator.Listener<BatteryDischarges_Sim>() {
                public void onEvent(BatteryDischarges_Sim event) {
                    //Log.d("Tester", "BatteryDischarges: " + event);
                    //Log.d("Tester", "DISCHARGE");
                    Intent intent = new Intent();

                    intent.setAction(Intent.ACTION_POWER_DISCONNECTED);
                    if (bound) {
                        //batservice.onReceive(getApplicationContext(), intent);
                    }
                }
            });

            /*sim.barometer.addListener(new AwareSimulator.Listener<Barometer_Sim>() {
                public void onEvent(Barometer_Sim event) {
                    SensorEvent BarometerEvent = CreateBarometerEvent(event);
                    if (bound) {
                        //barometerservice.onSensorChanged(BarometerEvent);
                    }
                }
            });*/

            sim.gravity.addListener(new AwareSimulator.Listener<Gravity_Sim>() {
                public void onEvent(Gravity_Sim event) {

                    SensorEvent GravityEvent = CreateGravityEvent(event);
                    if (bound) {
                        //Log.d("Tester", "Gravity: " + event);
                        //gravityservice.onSensorChanged(GravityEvent);
                        /*try{
                            Log.d("Tester", "at if");
                            Context accContext = createPackageContext("com.aware.Aware*", Context.CONTEXT_IGNORE_SECURITY);
                            Log.d("Tester", "after createPackageContext");
                            Class acc = accContext.getClassLoader().loadClass("com.aware.Aware.Gravity");
                            Log.d("Tester", "after loadClass");
                            Class params[] = {};
                            acc.getMethod("Gravity.onSensorChanged", params).invoke(acc, GravityEvent);
                        } catch (PackageManager.NameNotFoundException nnfe){
                            Log.d("Tester", "NNFE");
                        } catch (ClassNotFoundException e) {
                            Log.d("Tester", "CNFE");
                        } catch (NoSuchMethodException e){
                            Log.d("Tester", "NSME");
                        } catch (IllegalAccessException e){
                            Log.d("Tester", "IAE");
                        } catch (InvocationTargetException e){
                            Log.d("Tester", "ITE");
                        }*/
                    }
                }
            });

            sim.gyroscope.addListener(new AwareSimulator.Listener<Gyroscope_Sim>() {
                public void onEvent(Gyroscope_Sim event) {
                    //Log.d("Tester:", "Gyroscope: " +event );
                    SensorEvent GyroscopeEvent = CreateGyroscopeEvent(event);
                    if (bound) {
                        //gyroscopeservice.onSensorChanged(GyroscopeEvent);
                    }
                }
            });

            sim.locations.addListener(new AwareSimulator.Listener<Locations_Sim>() {
                public void onEvent(Locations_Sim event) {
                    Location newLocation = createLocation(event);
                    if (bound) {
                        //locationsservice.onLocationChanged(newLocation);
                    }
                }
            });

            sim.sensorWifi.addListener(new AwareSimulator.Listener<SensorWifi_Sim>() {
                public void onEvent(SensorWifi_Sim event) {
                    //if (bound) {
                        //wifiservice.BackgroundService(Intent intent);
                        //Log.d("Tester", "SensorWifi: " + event);
                    //}
                }
            });

            /*sim.wifi.addListener(new AwareSimulator.Listener<Wifi_Sim>() {
                public void onEvent(Wifi_Sim event) {
                    Log.d("Tester", "Wifi: " + event);
                }
            });*/

            sim.start();

        } catch (SQLException|ClassNotFoundException e) {
            Log.d("Tester", "ClassNotFoundException");
            e.printStackTrace();
        }
    }

    public static SensorEvent CreateAccelerometerEvent(float[] values, Accelerometer_Sim vals) {
        try {
            Constructor<SensorEvent> c = SensorEvent.class.getDeclaredConstructor(int.class);

            c.setAccessible(true);
            SensorEvent event = c.newInstance(values.length);
            System.arraycopy(values, 0, event.values, 0, values.length);
            event.timestamp = vals.timestamp();
            event.accuracy = vals.accuracy;
            return event;

        } catch (InvocationTargetException e) {
            Log.d("Tester", "InvovationTargetException");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            Log.d("Tester", "InstationException");
        } catch (IllegalAccessException e) {
            Log.d("Tester", "IllegalAccessException");
        }
    return null;
    }

    public static SensorEvent CreateBarometerEvent(Barometer_Sim event) {
        try {
            Constructor<SensorEvent> c = SensorEvent.class.getDeclaredConstructor(int.class);
            c.setAccessible(true);
            SensorEvent BarometerEvent = c.newInstance(1);

            BarometerEvent.values[0] = event.values_0.floatValue();
            BarometerEvent.accuracy = event.accuracy;

            return BarometerEvent;

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SensorEvent CreateGravityEvent(Gravity_Sim event) {
        try {
            Constructor<SensorEvent> c = SensorEvent.class.getDeclaredConstructor(int.class);

            c.setAccessible(true);
            SensorEvent GravityEvent = c.newInstance(3);

            float[] values = new float[3];
            values[0] = event.values_0.floatValue();
            values[1] = event.values_1.floatValue();
            values[2] = event.values_2.floatValue();
            System.arraycopy(values, 0, GravityEvent.values, 0, values.length);

            GravityEvent.accuracy = event.accuracy;

            return GravityEvent;

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SensorEvent CreateGyroscopeEvent(Gyroscope_Sim event) {
        try {
            Constructor<SensorEvent> c = SensorEvent.class.getDeclaredConstructor(int.class);

            c.setAccessible(true);
            SensorEvent GyroscopeEvent = c.newInstance(3);

            GyroscopeEvent.values[0] = event.axis_x.floatValue();
            GyroscopeEvent.values[1] = event.axis_y.floatValue();
            GyroscopeEvent.values[2] = event.axis_z.floatValue();
            GyroscopeEvent.accuracy = event.accuracy;

            return GyroscopeEvent;

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*public BatteryManager CreateBatteryManager(Battery_Sim event) {
        try{

            Log.d("Tester", "at create battery manager");
            Constructor<BatteryManager> c = BatteryManager.class.getDeclaredConstructor(new Class[0]);
            Log.d("Tester", "at new instance");
            c.setAccessible(true);
            Log.d("Tester", "after set constructor accessible");
            BatteryManager fakeManager = c.newInstance(new Object[0]);

            //BatteryManager fakeManager = (BatteryManager)this.getSystemService(BATTERY_SERVICE);
            Log.d("Tester", "after create fakemanager");
            Field status = BatteryManager.class.getField("EXTRA_STATUS");
            Log.d("Tester", "after create field");
            status.setAccessible(true);
            Log.d("Tester", "after set field accessible");
            status.setInt(fakeManager, event.batteryStatus);
            Log.d("Tester", "BatteryManager:" + fakeManager.EXTRA_STATUS);

            return fakeManager;

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }catch (IllegalAccessException e) {
            e.printStackTrace();
        }catch (NoSuchFieldException e) {
            e.printStackTrace();
        }catch (InvocationTargetException e){
            e.printStackTrace();
        }catch (InstantiationException e ){
            e.printStackTrace();
        }

        return null;
    }*/

    /*public static AccessibilityEvent CreateForegroundEvent(ApplicationsForeground_Sim event) {
        try {
            Constructor<AccessibilityEvent> c = AccessibilityEvent.class.getDeclaredConstructor(int.class);
            c.setAccessible(true);
            AccessibilityEvent aEvent = c.newInstance();
            aEvent.setPackageName(event.packageName);
            aEvent.setEventType(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
            return aEvent;

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }catch (NoSuchMethodException e) {
            e.printStackTrace();
        }catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
        e.printStackTrace();
        }
        return null;
}*/
    public static Location createLocation(Locations_Sim event){
        Location newLocation = new Location("fakeLoc");

        newLocation.setAccuracy(((float) event.accuracy));
        newLocation.setAltitude(event.doubleAltitude);
        newLocation.setBearing((float) event.doubleBearing);
        newLocation.setLatitude(event.doubleLatitude);
        newLocation.setLongitude(event.doubleLongitude);
        newLocation.setSpeed((float) event.doubleSpeed);
        newLocation.setProvider(event.provider);

        return newLocation;
    }

    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

}