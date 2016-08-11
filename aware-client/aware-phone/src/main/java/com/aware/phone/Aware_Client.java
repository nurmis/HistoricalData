
package com.aware.phone;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.phone.ui.Aware_Activity;
import com.aware.phone.ui.Plugins_Manager;
import com.aware.utils.Http;
import com.aware.utils.Https;
import com.aware.utils.SSLManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.aware.Accelerometer;

/**
 * Preferences dashboard for the AWARE Aware
 * Allows the researcher to configure all the modules, start and stop modules and where logging happens.
 *
 * @author df
 */
public class Aware_Client extends Aware_Activity {

    private static final int DIALOG_ERROR_MISSING_PARAMETERS = 2;
    private static final int DIALOG_ERROR_MISSING_SENSOR = 3;

    /**
     * Used to disable sensors that are not applicable
     */
    private static boolean is_watch = false;

    private static SensorManager mSensorMgr;
    private static Context awareContext;
    private static PreferenceActivity clientUI;

    final private int REQUEST_CODE_PERMISSIONS = 999;

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
            case DIALOG_ERROR_MISSING_PARAMETERS:
                builder.setMessage("Some parameters are missing...");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                break;
            case DIALOG_ERROR_MISSING_SENSOR:
                builder.setMessage("This device is missing this sensor.");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                break;
        }
        return dialog;
    }

    private void defaultSettings() {
        final SharedPreferences prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        if (!prefs.contains("intro_done")) {
            final ViewGroup parent = (ViewGroup) findViewById(android.R.id.content);
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

            final View help_qrcode = inflater.inflate(R.layout.help_qrcode, null);
            final View help_menu = inflater.inflate(R.layout.help_menu, null);
            parent.addView(help_qrcode, params);
            help_qrcode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    parent.removeView(help_qrcode);
                    parent.addView(help_menu, params);
                }
            });

            help_menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    parent.removeView(help_menu);
                    prefs.edit().putBoolean("intro_done", true).commit();
                }
            });
        }

        developerOptions();
        servicesOptions();
        logging();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "AWARE external storage access required!", Toast.LENGTH_SHORT).show();
                Aware.stopAWARE();
                finish();
            } else {
                //Restart AWARE client now that we have the permission to write to external storage
                Intent preferences = new Intent(this, Aware_Client.class);
                preferences.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(preferences);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        is_watch = Aware.is_watch(this);
        awareContext = getApplicationContext();
        clientUI = this;

        //Start the Aware
        Intent startAware = new Intent(awareContext, Aware.class);
        startService(startAware);

        int storage = ContextCompat.checkSelfPermission(Aware_Client.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int camera = ContextCompat.checkSelfPermission(Aware_Client.this, Manifest.permission.CAMERA);
        int phone_state = ContextCompat.checkSelfPermission(Aware_Client.this, Manifest.permission.READ_PHONE_STATE);
        int phone_log = ContextCompat.checkSelfPermission(Aware_Client.this, Manifest.permission.READ_CALL_LOG);
        int contacts = ContextCompat.checkSelfPermission(Aware_Client.this, Manifest.permission.READ_CONTACTS);
        int sms = ContextCompat.checkSelfPermission(Aware_Client.this, Manifest.permission.READ_SMS);
        int location_network = ContextCompat.checkSelfPermission(Aware_Client.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int location_gps = ContextCompat.checkSelfPermission(Aware_Client.this, Manifest.permission.ACCESS_FINE_LOCATION);

        ArrayList<String> missing_permissions = new ArrayList<>();
        if (storage != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (camera != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.CAMERA);
        }
        if (phone_state != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (phone_log != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.READ_CALL_LOG);
        }
        if (contacts != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.READ_CONTACTS);
        }
        if (sms != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.READ_SMS);
        }
        if (location_network != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (location_gps != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (missing_permissions.size() > 0) {
            requestPermissions(missing_permissions.toArray(new String[missing_permissions.size()]), REQUEST_CODE_PERMISSIONS);
        }

        SharedPreferences prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        if (prefs.getAll().isEmpty() && Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID).length() == 0) {
            PreferenceManager.setDefaultValues(getApplicationContext(), getPackageName(), Context.MODE_PRIVATE, R.xml.aware_preferences, true);
            prefs.edit().commit(); //commit changes
        } else {
            PreferenceManager.setDefaultValues(getApplicationContext(), getPackageName(), Context.MODE_PRIVATE, R.xml.aware_preferences, false);
        }

        Map<String, ?> defaults = prefs.getAll();
        for (Map.Entry<String, ?> entry : defaults.entrySet()) {
            if (Aware.getSetting(getApplicationContext(), entry.getKey(), "com.aware").length() == 0) {
                Aware.setSetting(getApplicationContext(), entry.getKey(), entry.getValue(), "com.aware");
            }
        }

        if (Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID).length() == 0) {
            UUID uuid = UUID.randomUUID();
            Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID, uuid.toString());
        }
        if (Aware.getSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER).length() == 0) {
            Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER, "https://api.awareframework.com/index.php");
        }

        addPreferencesFromResource(R.xml.aware_preferences);
        setContentView(R.layout.aware_ui);

        defaultSettings();

        //Check if AWARE is active on the accessibility services
        if (!Aware.is_watch(awareContext)) {
            Applications.isAccessibilityServiceActive(awareContext);
        }
    }

    /**
     * AWARE services UI components
     */
    public void servicesOptions() {
        esm();
        accelerometer();
        applications();
        barometer();
        battery();
        bluetooth();
        communication();
        gyroscope();
        light();
        linear_accelerometer();
        locations();
        magnetometer();
        network();
        screen();
        wifi();
        processor();
        timeZone();
        proximity();
        rotation();
        telephony();
        gravity();
        temperature();
    }

    /**
     * ESM module settings UI
     */
    private void esm() {
        final PreferenceScreen mobile_esm = (PreferenceScreen) findPreference("esm");
        if (is_watch) {
            mobile_esm.setEnabled(false);
            return;
        }
        final CheckBoxPreference esm = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_ESM);
        esm.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_ESM).equals("true"));
        esm.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_ESM, esm.isChecked());
                if (esm.isChecked()) {
                    Aware.startESM(awareContext);
                    mobile_esm.setIcon(getResources().getDrawable(R.drawable.ic_action_esm_active));
                } else {
                    Aware.stopESM(awareContext);
                    mobile_esm.setIcon(getResources().getDrawable(R.drawable.ic_action_esm));
                }
                return true;
            }
        });
    }

    /**
     * Temperature module settings UI
     */
    private void temperature() {
        final PreferenceScreen temp_pref = (PreferenceScreen) findPreference("temperature");
        Sensor temp = mSensorMgr.getDefaultSensor(Sensor.TYPE_TEMPERATURE);
        if (temp != null) {
            temp_pref.setSummary(temp_pref.getSummary().toString().replace("*", " - Power: " + temp.getPower() + " mA"));
        } else {
            temp_pref.setSummary(temp_pref.getSummary().toString().replace("*", ""));
            temp_pref.setEnabled(false);
            return;
        }

        final CheckBoxPreference temperature = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_TEMPERATURE);
        temperature.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_TEMPERATURE).equals("true"));
        temperature.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mSensorMgr.getDefaultSensor(Sensor.TYPE_TEMPERATURE) == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    temperature.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_TEMPERATURE, false);
                    return false;
                }

                Aware.setSetting(awareContext, Aware_Preferences.STATUS_TEMPERATURE, temperature.isChecked());
                if (temperature.isChecked()) {
                    Aware.startTemperature(awareContext);
                    temp_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_temperature_active));
                } else {
                    Aware.stopTemperature(awareContext);
                    temp_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_temperature));
                }
                return true;
            }
        });

        final ListPreference frequency_temperature = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_TEMPERATURE);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_TEMPERATURE).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_TEMPERATURE);
            frequency_temperature.setSummary(freq);
        }
        frequency_temperature.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_TEMPERATURE));
        frequency_temperature.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_TEMPERATURE, newValue);
                frequency_temperature.setSummary((String) newValue);
                Aware.startTemperature(awareContext);
                return true;
            }
        });
    }

    /**
     * Accelerometer module settings UI
     */
    private void accelerometer() {

        final PreferenceScreen accel_pref = (PreferenceScreen) findPreference("accelerometer");
        Sensor temp = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (temp != null) {
            accel_pref.setSummary(accel_pref.getSummary().toString().replace("*", " - Power: " + temp.getPower() + " mA"));
        } else {
            accel_pref.setSummary(accel_pref.getSummary().toString().replace("*", ""));
            accel_pref.setEnabled(false);
            return;
        }

        final CheckBoxPreference accelerometer = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_ACCELEROMETER);
        accelerometer.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_ACCELEROMETER).equals("true"));
        accelerometer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    accelerometer.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_ACCELEROMETER, false);
                    return false;
                }

                Aware.setSetting(awareContext, Aware_Preferences.STATUS_ACCELEROMETER, accelerometer.isChecked());
                if (accelerometer.isChecked()) {
                    Aware.startAccelerometer(awareContext);
                    accel_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_accelerometer_active));
                } else {
                    Aware.stopAccelerometer(awareContext);
                    accel_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_accelerometer));
                }
                return true;
            }
        });

        final ListPreference frequency_accelerometer = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_ACCELEROMETER);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_ACCELEROMETER).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_ACCELEROMETER);
            frequency_accelerometer.setSummary(freq);
        }
        frequency_accelerometer.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_ACCELEROMETER));
        frequency_accelerometer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_ACCELEROMETER, (String) newValue);
                frequency_accelerometer.setSummary((String) newValue);
                Aware.startAccelerometer(awareContext);
                return true;
            }
        });
    }

    /**
     * Linear Accelerometer module settings UI
     */
    private void linear_accelerometer() {

        final PreferenceScreen linear_pref = (PreferenceScreen) findPreference("linear_accelerometer");
        Sensor temp = mSensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (temp != null) {
            linear_pref.setSummary(linear_pref.getSummary().toString().replace("*", " - Power: " + temp.getPower() + " mA"));
        } else {
            linear_pref.setSummary(linear_pref.getSummary().toString().replace("*", ""));
            linear_pref.setEnabled(false);
            return;
        }

        final CheckBoxPreference linear_accelerometer = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_LINEAR_ACCELEROMETER);
        linear_accelerometer.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_LINEAR_ACCELEROMETER).equals("true"));
        linear_accelerometer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (mSensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    linear_accelerometer.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_LINEAR_ACCELEROMETER, false);
                    return false;
                }
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_LINEAR_ACCELEROMETER, linear_accelerometer.isChecked());
                if (linear_accelerometer.isChecked()) {
                    Aware.startLinearAccelerometer(awareContext);
                    linear_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_linear_acceleration_active));
                } else {
                    Aware.stopLinearAccelerometer(awareContext);
                    linear_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_linear_acceleration_active));
                }
                return true;
            }
        });

        final ListPreference frequency_linear_accelerometer = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_LINEAR_ACCELEROMETER);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LINEAR_ACCELEROMETER).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LINEAR_ACCELEROMETER);
            frequency_linear_accelerometer.setSummary(freq);
        }
        frequency_linear_accelerometer.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LINEAR_ACCELEROMETER));
        frequency_linear_accelerometer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_LINEAR_ACCELEROMETER, (String) newValue);
                frequency_linear_accelerometer.setSummary((String) newValue);
                Aware.startLinearAccelerometer(awareContext);
                return true;
            }
        });
    }

    /**
     * Applications module settings UI
     */
    private void applications() {
        final PreferenceScreen apps_pref = (PreferenceScreen) findPreference("applications");
        final CheckBoxPreference notifications = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_NOTIFICATIONS);
        notifications.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_NOTIFICATIONS).equals("true"));
        notifications.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Applications.isAccessibilityServiceActive(awareContext) && notifications.isChecked()) {
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_NOTIFICATIONS, notifications.isChecked());
                    notifications.setChecked(true);
                    Aware.startApplications(awareContext);
                    apps_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_applications_active));
                    return true;
                }
                Applications.isAccessibilityServiceActive(awareContext);
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_NOTIFICATIONS, false);
                notifications.setChecked(false);
                return false;
            }
        });
        final CheckBoxPreference keyboard = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_KEYBOARD);
        keyboard.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_KEYBOARD).equals("true"));
        keyboard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Applications.isAccessibilityServiceActive(awareContext) && keyboard.isChecked()) {
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_KEYBOARD, keyboard.isChecked());
                    keyboard.setChecked(true);
                    Aware.startApplications(awareContext);
                    Aware.startKeyboard(awareContext);
                    apps_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_applications_active));
                    return true;
                }
                Applications.isAccessibilityServiceActive(awareContext);
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_KEYBOARD, false);
                keyboard.setChecked(false);
                Aware.stopKeyboard(awareContext);
                return false;
            }
        });
        final CheckBoxPreference crashes = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_CRASHES);
        crashes.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_CRASHES).equals("true"));
        crashes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Applications.isAccessibilityServiceActive(awareContext) && crashes.isChecked()) {
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_CRASHES, crashes.isChecked());
                    crashes.setChecked(true);
                    Aware.startApplications(awareContext);
                    apps_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_applications_active));
                    return true;
                }
                Applications.isAccessibilityServiceActive(awareContext);
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_CRASHES, false);
                crashes.setChecked(false);
                return false;
            }
        });
        final CheckBoxPreference applications = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_APPLICATIONS);
        if (Aware.getSetting(awareContext, Aware_Preferences.STATUS_APPLICATIONS).equals("true") && !Applications.isAccessibilityServiceActive(awareContext)) {
            Aware.setSetting(awareContext, Aware_Preferences.STATUS_APPLICATIONS, false);
            Aware.stopApplications(awareContext);
            apps_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_applications));
        }
        applications.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_APPLICATIONS).equals("true"));
        applications.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Applications.isAccessibilityServiceActive(awareContext) && applications.isChecked()) {
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_APPLICATIONS, true);
                    applications.setChecked(true);
                    Aware.startApplications(awareContext);
                    apps_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_applications_active));
                    return true;
                } else {
                    Applications.isAccessibilityServiceActive(awareContext);

                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_APPLICATIONS, false);
                    applications.setChecked(false);

                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_NOTIFICATIONS, false);
                    notifications.setChecked(false);

                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_CRASHES, false);
                    crashes.setChecked(false);

                    Aware.stopApplications(awareContext);
                    apps_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_applications));
                    return false;
                }
            }
        });

        final EditTextPreference frequency_applications = (EditTextPreference) findPreference(Aware_Preferences.FREQUENCY_APPLICATIONS);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_APPLICATIONS).length() > 0) {
            frequency_applications.setSummary(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_APPLICATIONS) + " seconds");
        }
        frequency_applications.setText(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_APPLICATIONS));
        frequency_applications.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_APPLICATIONS, (String) newValue);
                frequency_applications.setSummary((String) newValue + " seconds");
                Aware.startApplications(awareContext);
                return true;
            }
        });

        final CheckBoxPreference installations = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_INSTALLATIONS);
        installations.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_INSTALLATIONS).equals("true"));
        installations.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_INSTALLATIONS, installations.isChecked());
                if (installations.isChecked()) {
                    Aware.startInstallations(awareContext);
                    apps_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_applications_active));
                } else {
                    Aware.stopInstallations(awareContext);
                    apps_pref.setIcon(getResources().getDrawable(R.drawable.ic_action_applications));
                }
                return true;
            }
        });
    }

    /**
     * Battery module settings UI
     */
    private void battery() {
        final CheckBoxPreference battery = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_BATTERY);
        battery.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_BATTERY).equals("true"));
        battery.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_BATTERY, battery.isChecked());
                if (battery.isChecked()) {
                    Aware.startBattery(awareContext);
                } else {
                    Aware.stopBattery(awareContext);
                }
                return true;
            }
        });
    }

    /**
     * Bluetooth module settings UI
     */
    private void bluetooth() {
        final CheckBoxPreference bluetooth = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_BLUETOOTH);
        bluetooth.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_BLUETOOTH).equals("true"));
        bluetooth.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                if (btAdapter == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    bluetooth.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_BLUETOOTH, false);
                    return false;
                }

                Aware.setSetting(awareContext, Aware_Preferences.STATUS_BLUETOOTH, bluetooth.isChecked());
                if (bluetooth.isChecked()) {
                    Aware.startBluetooth(awareContext);
                } else {
                    Aware.stopBluetooth(awareContext);
                }
                return true;
            }
        });

        final EditTextPreference bluetoothInterval = (EditTextPreference) findPreference(Aware_Preferences.FREQUENCY_BLUETOOTH);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_BLUETOOTH).length() > 0) {
            bluetoothInterval.setSummary(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_BLUETOOTH) + " seconds");
        }
        bluetoothInterval.setText(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_BLUETOOTH));
        bluetoothInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_BLUETOOTH, (String) newValue);
                bluetoothInterval.setSummary((String) newValue + " seconds");
                Aware.startBluetooth(awareContext);
                return true;
            }
        });
    }

    /**
     * Communication module settings UI
     */
    private void communication() {
        final PreferenceScreen communications = (PreferenceScreen) findPreference("communication");
        if (is_watch) {
            communications.setEnabled(false);
            return;
        }

        final CheckBoxPreference calls = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_CALLS);
        calls.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_CALLS).equals("true"));
        calls.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_CALLS, calls.isChecked());
                if (calls.isChecked()) {
                    Aware.startCommunication(awareContext);
                } else {
                    Aware.stopCommunication(awareContext);
                }
                return true;
            }
        });

        final CheckBoxPreference messages = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_MESSAGES);
        messages.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_MESSAGES).equals("true"));
        messages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_MESSAGES, messages.isChecked());
                if (messages.isChecked()) {
                    Aware.startCommunication(awareContext);
                } else {
                    Aware.stopCommunication(awareContext);
                }
                return true;
            }
        });

        final CheckBoxPreference communication = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_COMMUNICATION_EVENTS);
        communication.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_COMMUNICATION_EVENTS).equals("true"));
        communication.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_COMMUNICATION_EVENTS, communication.isChecked());
                if (communication.isChecked()) {
                    Aware.startCommunication(awareContext);
                } else {
                    Aware.stopCommunication(awareContext);
                }
                return true;
            }
        });
    }

    /**
     * Gravity module settings UI
     */
    private void gravity() {
        final PreferenceScreen grav_pref = (PreferenceScreen) findPreference("gravity");
        Sensor temp = mSensorMgr.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (temp != null) {
            grav_pref.setSummary(grav_pref.getSummary().toString().replace("*", " - Power: " + temp.getPower() + " mA"));
        } else {
            grav_pref.setSummary(grav_pref.getSummary().toString().replace("*", ""));
            grav_pref.setEnabled(false);
            return;
        }

        final CheckBoxPreference gravity = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_GRAVITY);
        gravity.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_GRAVITY).equals("true"));
        gravity.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (mSensorMgr.getDefaultSensor(Sensor.TYPE_GRAVITY) == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    gravity.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_GRAVITY, false);
                    return false;
                }
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_GRAVITY, gravity.isChecked());
                if (gravity.isChecked()) {
                    Aware.startGravity(awareContext);
                } else {
                    Aware.stopGravity(awareContext);
                }
                return true;
            }
        });

        final ListPreference frequency_gravity = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_GRAVITY);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_GRAVITY).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_GRAVITY);
            frequency_gravity.setSummary(freq);
        }
        frequency_gravity.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_GRAVITY));
        frequency_gravity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_GRAVITY, (String) newValue);
                frequency_gravity.setSummary((String) newValue);
                Aware.startGravity(awareContext);
                return true;
            }
        });
    }

    /**
     * Gyroscope module settings UI
     */
    private void gyroscope() {
        final PreferenceScreen gyro_pref = (PreferenceScreen) findPreference("gyroscope");
        Sensor temp = mSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (temp != null) {
            gyro_pref.setSummary(gyro_pref.getSummary().toString().replace("*", " - Power: " + temp.getPower() + " mA"));
        } else {
            gyro_pref.setSummary(gyro_pref.getSummary().toString().replace("*", ""));
            gyro_pref.setEnabled(false);
            return;
        }

        final CheckBoxPreference gyroscope = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_GYROSCOPE);
        gyroscope.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_GYROSCOPE).equals("true"));
        gyroscope.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (mSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    gyroscope.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_GYROSCOPE, false);
                    return false;
                }
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_GYROSCOPE, gyroscope.isChecked());
                if (gyroscope.isChecked()) {
                    Aware.startGyroscope(awareContext);
                } else {
                    Aware.stopGyroscope(awareContext);
                }
                return true;
            }
        });

        final ListPreference frequency_gyroscope = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_GYROSCOPE);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_GYROSCOPE).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_GYROSCOPE);
            frequency_gyroscope.setSummary(freq);
        }
        frequency_gyroscope.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_GYROSCOPE));
        frequency_gyroscope.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_GYROSCOPE, (String) newValue);
                frequency_gyroscope.setSummary((String) newValue);
                Aware.startGyroscope(awareContext);
                return true;
            }
        });
    }

    /**
     * Location module settings UI
     */
    private void locations() {
        final PreferenceScreen locations = (PreferenceScreen) findPreference("locations");
        if (is_watch) {
            locations.setEnabled(false);
            return;
        }

        final CheckBoxPreference location_gps = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_LOCATION_GPS);
        location_gps.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_LOCATION_GPS).equals("true"));
        location_gps.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                LocationManager localMng = (LocationManager) awareContext.getSystemService(LOCATION_SERVICE);
                List<String> providers = localMng.getAllProviders();

                if (!providers.contains(LocationManager.GPS_PROVIDER)) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    location_gps.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_LOCATION_GPS, false);
                    return false;
                }

                Aware.setSetting(awareContext, Aware_Preferences.STATUS_LOCATION_GPS, location_gps.isChecked());
                if (location_gps.isChecked()) {
                    Aware.startLocations(awareContext);
                } else {
                    Aware.stopLocations(awareContext);
                }
                return true;
            }
        });

        final CheckBoxPreference location_network = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_LOCATION_NETWORK);
        location_network.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_LOCATION_NETWORK).equals("true"));
        location_network.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                LocationManager localMng = (LocationManager) awareContext.getSystemService(LOCATION_SERVICE);
                List<String> providers = localMng.getAllProviders();

                if (!providers.contains(LocationManager.NETWORK_PROVIDER)) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    location_gps.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_LOCATION_NETWORK, false);
                    return false;
                }

                Aware.setSetting(awareContext, Aware_Preferences.STATUS_LOCATION_NETWORK, location_network.isChecked());
                if (location_network.isChecked()) {
                    Aware.startLocations(awareContext);
                } else {
                    Aware.stopLocations(awareContext);
                }
                return true;
            }
        });

        final EditTextPreference gpsInterval = (EditTextPreference) findPreference(Aware_Preferences.FREQUENCY_LOCATION_GPS);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LOCATION_GPS).length() > 0) {
            gpsInterval.setSummary(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LOCATION_GPS) + " seconds");
        }
        gpsInterval.setText(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LOCATION_GPS));
        gpsInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_LOCATION_GPS, (String) newValue);
                gpsInterval.setSummary((String) newValue + " seconds");
                Aware.startLocations(awareContext);
                return true;
            }
        });

        final EditTextPreference networkInterval = (EditTextPreference) findPreference(Aware_Preferences.FREQUENCY_LOCATION_NETWORK);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LOCATION_NETWORK).length() > 0) {
            networkInterval.setSummary(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LOCATION_NETWORK) + " seconds");
        }
        networkInterval.setText(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LOCATION_NETWORK));
        networkInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_LOCATION_NETWORK, (String) newValue);
                networkInterval.setSummary((String) newValue + " seconds");
                Aware.startLocations(awareContext);
                return true;
            }
        });

        final EditTextPreference gpsAccuracy = (EditTextPreference) findPreference(Aware_Preferences.MIN_LOCATION_GPS_ACCURACY);
        if (Aware.getSetting(awareContext, Aware_Preferences.MIN_LOCATION_GPS_ACCURACY).length() > 0) {
            gpsAccuracy.setSummary(Aware.getSetting(awareContext, Aware_Preferences.MIN_LOCATION_GPS_ACCURACY) + " meters");
        }
        gpsAccuracy.setText(Aware.getSetting(awareContext, Aware_Preferences.MIN_LOCATION_GPS_ACCURACY));
        gpsAccuracy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.MIN_LOCATION_GPS_ACCURACY, (String) newValue);
                gpsAccuracy.setSummary((String) newValue + " meters");
                Aware.startLocations(awareContext);
                return true;
            }
        });

        final EditTextPreference networkAccuracy = (EditTextPreference) findPreference(Aware_Preferences.MIN_LOCATION_NETWORK_ACCURACY);
        if (Aware.getSetting(awareContext, Aware_Preferences.MIN_LOCATION_NETWORK_ACCURACY).length() > 0) {
            networkAccuracy.setSummary(Aware.getSetting(awareContext, Aware_Preferences.MIN_LOCATION_NETWORK_ACCURACY) + " meters");
        }
        networkAccuracy.setText(Aware.getSetting(awareContext, Aware_Preferences.MIN_LOCATION_NETWORK_ACCURACY));
        networkAccuracy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.MIN_LOCATION_NETWORK_ACCURACY, (String) newValue);
                networkAccuracy.setSummary((String) newValue + " meters");
                Aware.startLocations(awareContext);
                return true;
            }
        });

        final EditTextPreference expirateTime = (EditTextPreference) findPreference(Aware_Preferences.LOCATION_EXPIRATION_TIME);
        if (Aware.getSetting(awareContext, Aware_Preferences.LOCATION_EXPIRATION_TIME).length() > 0) {
            expirateTime.setSummary(Aware.getSetting(awareContext, Aware_Preferences.LOCATION_EXPIRATION_TIME) + " seconds");
        }
        expirateTime.setText(Aware.getSetting(awareContext, Aware_Preferences.LOCATION_EXPIRATION_TIME));
        expirateTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.LOCATION_EXPIRATION_TIME, (String) newValue);
                expirateTime.setSummary((String) newValue + " seconds");
                Aware.startLocations(awareContext);
                return true;
            }
        });
    }

    /**
     * Network module settings UI
     */
    private void network() {
        final PreferenceScreen networks = (PreferenceScreen) findPreference("network");
        if (is_watch) {
            networks.setEnabled(false);
            return;
        }

        final CheckBoxPreference network_traffic = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_NETWORK_TRAFFIC);
        network_traffic.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_NETWORK_TRAFFIC).equals("true"));
        network_traffic.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_NETWORK_TRAFFIC, network_traffic.isChecked());
                if (network_traffic.isChecked()) {
                    Aware.startTraffic(awareContext);
                } else {
                    Aware.stopTraffic(awareContext);
                }
                return true;
            }
        });

        final EditTextPreference frequencyTraffic = (EditTextPreference) findPreference(Aware_Preferences.FREQUENCY_NETWORK_TRAFFIC);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_NETWORK_TRAFFIC).length() > 0) {
            frequencyTraffic.setSummary(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_NETWORK_TRAFFIC) + " seconds");
        }
        frequencyTraffic.setText(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_NETWORK_TRAFFIC));
        frequencyTraffic.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_NETWORK_TRAFFIC, (String) newValue);
                frequencyTraffic.setSummary((String) newValue + " seconds");
                if (network_traffic.isChecked()) {
                    Aware.startTraffic(awareContext);
                }
                return true;
            }
        });

        final CheckBoxPreference network = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_NETWORK_EVENTS);
        network.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_NETWORK_EVENTS).equals("true"));
        network.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_NETWORK_EVENTS, network.isChecked());
                if (network.isChecked()) {
                    Aware.startNetwork(awareContext);
                } else {
                    Aware.stopNetwork(awareContext);
                }
                return true;
            }
        });
    }

    /**
     * Screen module settings UI
     */
    private void screen() {
        final CheckBoxPreference screen = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_SCREEN);
        screen.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_SCREEN).equals("true"));
        screen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_SCREEN, screen.isChecked());
                if (screen.isChecked()) {
                    Aware.startScreen(awareContext);
                } else {
                    Aware.stopScreen(awareContext);
                }
                return true;
            }
        });
    }

    /**
     * WiFi module settings UI
     */
    private void wifi() {
        final PreferenceScreen wifis = (PreferenceScreen) findPreference("wifi");
        if (is_watch) {
            wifis.setEnabled(false);
            return;
        }

        final CheckBoxPreference wifi = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_WIFI);
        wifi.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_WIFI).equals("true"));
        wifi.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_WIFI, wifi.isChecked());
                if (wifi.isChecked()) {
                    Aware.startWiFi(awareContext);
                } else {
                    Aware.stopWiFi(awareContext);
                }
                return true;
            }
        });

        final EditTextPreference wifiInterval = (EditTextPreference) findPreference(Aware_Preferences.FREQUENCY_WIFI);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_WIFI).length() > 0) {
            wifiInterval.setSummary(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_WIFI) + " seconds");
        }
        wifiInterval.setText(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_WIFI));
        wifiInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_WIFI, (String) newValue);
                wifiInterval.setSummary((String) newValue + " seconds");
                Aware.startWiFi(awareContext);
                return true;
            }
        });
    }

    /**
     * Processor module settings UI
     */
    private void processor() {
        final CheckBoxPreference processor = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_PROCESSOR);
        processor.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_PROCESSOR).equals("true"));
        processor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_PROCESSOR, processor.isChecked());
                if (processor.isChecked()) {
                    Aware.startProcessor(awareContext);
                } else {
                    Aware.stopProcessor(awareContext);
                }
                return true;
            }
        });

        final EditTextPreference frequencyProcessor = (EditTextPreference) findPreference(Aware_Preferences.FREQUENCY_PROCESSOR);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_PROCESSOR).length() > 0) {
            frequencyProcessor.setSummary(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_PROCESSOR) + " seconds");
        }
        frequencyProcessor.setText(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_PROCESSOR));
        frequencyProcessor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_PROCESSOR, (String) newValue);
                frequencyProcessor.setSummary((String) newValue + " seconds");
                Aware.startProcessor(awareContext);
                return true;
            }
        });
    }

    /**
     * Timezone module settings UI
     */
    private void timeZone() {
        final PreferenceScreen timezones = (PreferenceScreen) findPreference("timezone");
        if (is_watch) {
            timezones.setEnabled(false);
            return;
        }

        final CheckBoxPreference timeZone = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_TIMEZONE);
        timeZone.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_TIMEZONE).equals("true"));
        timeZone.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_TIMEZONE, timeZone.isChecked());
                if (timeZone.isChecked()) {
                    Aware.startTimeZone(awareContext);
                } else {
                    Aware.stopTimeZone(awareContext);
                }
                return true;
            }
        });

        final EditTextPreference frequencyTimeZone = (EditTextPreference) findPreference(Aware_Preferences.FREQUENCY_TIMEZONE);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_TIMEZONE).length() > 0) {
            frequencyTimeZone.setSummary(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_TIMEZONE) + " seconds");
        }
        frequencyTimeZone.setText(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_TIMEZONE));
        frequencyTimeZone.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_TIMEZONE, (String) newValue);
                frequencyTimeZone.setSummary((String) newValue + " seconds");
                Aware.startTimeZone(awareContext);
                return true;
            }
        });
    }

    /**
     * Light module settings UI
     */
    private void light() {

        final PreferenceScreen light_pref = (PreferenceScreen) findPreference("light");
        Sensor temp = mSensorMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (temp != null) {
            light_pref.setSummary(light_pref.getSummary().toString().replace("*", " - Power: " + temp.getPower() + " mA"));
        } else {
            light_pref.setSummary(light_pref.getSummary().toString().replace("*", ""));
            light_pref.setEnabled(false);
            return;
        }

        final CheckBoxPreference light = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_LIGHT);
        light.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_LIGHT).equals("true"));
        light.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mSensorMgr.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    light.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_LIGHT, false);
                    return false;
                }

                Aware.setSetting(awareContext, Aware_Preferences.STATUS_LIGHT, light.isChecked());
                if (light.isChecked()) {
                    Aware.startLight(awareContext);
                } else {
                    Aware.stopLight(awareContext);
                }
                return true;
            }
        });

        final ListPreference frequency_light = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_LIGHT);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LIGHT).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LIGHT);
            frequency_light.setSummary(freq);
        }
        frequency_light.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_LIGHT));
        frequency_light.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_LIGHT, (String) newValue);
                frequency_light.setSummary((String) newValue);
                Aware.startLight(awareContext);
                return true;
            }
        });
    }

    /**
     * Magnetometer module settings UI
     */
    private void magnetometer() {
        final PreferenceScreen magno_pref = (PreferenceScreen) findPreference("magnetometer");
        Sensor temp = mSensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (temp != null) {
            magno_pref.setSummary(magno_pref.getSummary().toString().replace("*", " - Power: " + temp.getPower() + " mA"));
        } else {
            magno_pref.setSummary(magno_pref.getSummary().toString().replace("*", ""));
            magno_pref.setEnabled(false);
            return;
        }

        final CheckBoxPreference magnetometer = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_MAGNETOMETER);
        magnetometer.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_MAGNETOMETER).equals("true"));
        magnetometer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (mSensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    magnetometer.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_MAGNETOMETER, false);
                    return false;
                }

                Aware.setSetting(awareContext, Aware_Preferences.STATUS_MAGNETOMETER, magnetometer.isChecked());
                if (magnetometer.isChecked()) {
                    Aware.startMagnetometer(awareContext);
                } else {
                    Aware.stopMagnetometer(awareContext);
                }
                return true;
            }
        });

        final ListPreference frequency_magnetometer = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_MAGNETOMETER);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_MAGNETOMETER).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_MAGNETOMETER);
            frequency_magnetometer.setSummary(freq);
        }
        frequency_magnetometer.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_MAGNETOMETER));
        frequency_magnetometer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_MAGNETOMETER, (String) newValue);
                frequency_magnetometer.setSummary((String) newValue);
                Aware.startMagnetometer(awareContext);
                return true;
            }
        });
    }

    /**
     * Atmospheric Pressure module settings UI
     */
    private void barometer() {
        final PreferenceScreen baro_pref = (PreferenceScreen) findPreference("barometer");
        Sensor temp = mSensorMgr.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (temp != null) {
            baro_pref.setSummary(baro_pref.getSummary().toString().replace("*", " - Power: " + temp.getPower() + " mA"));
        } else {
            baro_pref.setSummary(baro_pref.getSummary().toString().replace("*", ""));
            baro_pref.setEnabled(false);
            return;
        }

        final CheckBoxPreference pressure = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_BAROMETER);
        pressure.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_BAROMETER).equals("true"));
        pressure.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (mSensorMgr.getDefaultSensor(Sensor.TYPE_PRESSURE) == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    pressure.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_BAROMETER, false);
                    return false;
                }

                Aware.setSetting(awareContext, Aware_Preferences.STATUS_BAROMETER, pressure.isChecked());
                if (pressure.isChecked()) {
                    Aware.startBarometer(awareContext);
                } else {
                    Aware.stopBarometer(awareContext);
                }
                return true;
            }
        });

        final ListPreference frequency_pressure = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_BAROMETER);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_BAROMETER).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_BAROMETER);
            frequency_pressure.setSummary(freq);
        }
        frequency_pressure.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_BAROMETER));
        frequency_pressure.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_BAROMETER, (String) newValue);
                frequency_pressure.setSummary((String) newValue);
                Aware.startBarometer(awareContext);
                return true;
            }
        });
    }

    /**
     * Proximity module settings UI
     */
    private void proximity() {

        final PreferenceScreen proxi_pref = (PreferenceScreen) findPreference("proximity");
        Sensor temp = mSensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (temp != null) {
            proxi_pref.setSummary(proxi_pref.getSummary().toString().replace("*", " - Power: " + temp.getPower() + " mA"));
        } else {
            proxi_pref.setSummary(proxi_pref.getSummary().toString().replace("*", ""));
            proxi_pref.setEnabled(false);
            return;
        }

        final CheckBoxPreference proximity = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_PROXIMITY);
        proximity.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_PROXIMITY).equals("true"));
        proximity.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (mSensorMgr.getDefaultSensor(Sensor.TYPE_PROXIMITY) == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    proximity.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_PROXIMITY, false);
                    return false;
                }

                Aware.setSetting(awareContext, Aware_Preferences.STATUS_PROXIMITY, proximity.isChecked());
                if (proximity.isChecked()) {
                    Aware.startProximity(awareContext);
                } else {
                    Aware.stopProximity(awareContext);
                }
                return true;
            }
        });

        final ListPreference frequency_proximity = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_PROXIMITY);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_PROXIMITY).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_PROXIMITY);
            frequency_proximity.setSummary(freq);
        }
        frequency_proximity.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_PROXIMITY));
        frequency_proximity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_PROXIMITY, (String) newValue);
                frequency_proximity.setSummary((String) newValue);
                Aware.startProximity(awareContext);
                return true;
            }
        });
    }

    /**
     * Rotation module settings UI
     */
    private void rotation() {

        final PreferenceScreen rotation_pref = (PreferenceScreen) findPreference("rotation");
        Sensor temp = mSensorMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (temp != null) {
            rotation_pref.setSummary(rotation_pref.getSummary().toString().replace("*", " - Power: " + temp.getPower() + " mA"));
        } else {
            rotation_pref.setSummary(rotation_pref.getSummary().toString().replace("*", ""));
            rotation_pref.setEnabled(false);
            return;
        }

        final CheckBoxPreference rotation = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_ROTATION);
        rotation.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_ROTATION).equals("true"));
        rotation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mSensorMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                    rotation.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_ROTATION, false);
                    return false;
                }

                Aware.setSetting(awareContext, Aware_Preferences.STATUS_ROTATION, rotation.isChecked());
                if (rotation.isChecked()) {
                    Aware.startRotation(awareContext);
                } else {
                    Aware.stopRotation(awareContext);
                }
                return true;
            }
        });

        final ListPreference frequency_rotation = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_ROTATION);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_ROTATION).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_ROTATION);
            frequency_rotation.setSummary(freq);
        }
        frequency_rotation.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_ROTATION));
        frequency_rotation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_ROTATION, (String) newValue);
                frequency_rotation.setSummary((String) newValue);
                Aware.startRotation(awareContext);
                return true;
            }
        });
    }

    /**
     * Telephony module settings UI
     */
    private void telephony() {
        final PreferenceScreen telephonies = (PreferenceScreen) findPreference("telephony");
        if (is_watch) {
            telephonies.setEnabled(false);
            return;
        }
        final CheckBoxPreference telephony = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_TELEPHONY);
        telephony.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_TELEPHONY).equals("true"));
        telephony.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.STATUS_TELEPHONY, telephony.isChecked());
                if (telephony.isChecked()) {
                    Aware.startTelephony(awareContext);
                } else {
                    Aware.stopTelephony(awareContext);
                }
                return true;
            }
        });
    }

    /**
     * Logging module settings UI components
     */
    public void logging() {
        webservices();
        mqtt();
    }

    /**
     * Webservices module settings UI
     */
    private void webservices() {
        final CheckBoxPreference webservice = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_WEBSERVICE);
        webservice.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_WEBSERVICE).equals("true"));
        webservice.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (Aware.getSetting(awareContext, Aware_Preferences.WEBSERVICE_SERVER).length() == 0) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_PARAMETERS);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_WEBSERVICE, false);
                    webservice.setChecked(false);
                    return false;
                } else {
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_WEBSERVICE, webservice.isChecked());
                    if (webservice.isChecked() && Aware.getSetting(awareContext, Aware_Preferences.WEBSERVICE_SERVER).length() > 0) {
                        Aware.joinStudy(awareContext, Aware.getSetting(awareContext, Aware_Preferences.WEBSERVICE_SERVER));
                    }
                    return true;
                }
            }
        });

        final EditTextPreference webservice_server = (EditTextPreference) findPreference(Aware_Preferences.WEBSERVICE_SERVER);
        webservice_server.setText(Aware.getSetting(awareContext, Aware_Preferences.WEBSERVICE_SERVER));
        if (Aware.getSetting(awareContext, Aware_Preferences.WEBSERVICE_SERVER).length() > 0) {
            webservice_server.setSummary("Server: " + Aware.getSetting(awareContext, Aware_Preferences.WEBSERVICE_SERVER));
        }
        webservice_server.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.WEBSERVICE_SERVER, (String) newValue);
                webservice_server.setSummary("Server: " + (String) newValue);
                return true;
            }
        });

        final CheckBoxPreference webservice_wifi_only = (CheckBoxPreference) findPreference(Aware_Preferences.WEBSERVICE_WIFI_ONLY);
        webservice_wifi_only.setChecked(Aware.getSetting(awareContext, Aware_Preferences.WEBSERVICE_WIFI_ONLY).equals("true"));
        webservice_wifi_only.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.WEBSERVICE_WIFI_ONLY, webservice_wifi_only.isChecked());
                return true;
            }
        });

        final EditTextPreference frequency_webservice = (EditTextPreference) findPreference(Aware_Preferences.FREQUENCY_WEBSERVICE);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_WEBSERVICE).length() > 0) {
            frequency_webservice.setSummary(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_WEBSERVICE) + " minutes");
        }
        frequency_webservice.setText(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_WEBSERVICE));
        frequency_webservice.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_WEBSERVICE, (String) newValue);
                frequency_webservice.setSummary((String) newValue + " minutes");
                return true;
            }
        });

        final ListPreference clean_old_data = (ListPreference) findPreference(Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA);
        if (Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA).length() > 0) {
            String freq = Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA);
            if (freq.equals("0")) {
                clean_old_data.setSummary("Never");
            } else if (freq.equals("1")) {
                clean_old_data.setSummary("Weekly");
            } else if (freq.equals("2")) {
                clean_old_data.setSummary("Monthly");
            }
        }
        clean_old_data.setDefaultValue(Aware.getSetting(awareContext, Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA));
        clean_old_data.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA, (String) newValue);
                if (((String) newValue).equals("0")) {
                    clean_old_data.setSummary("Never");
                } else if (((String) newValue).equals("1")) {
                    clean_old_data.setSummary("Weekly");
                } else if (((String) newValue).equals("2")) {
                    clean_old_data.setSummary("Monthly");
                }
                return true;
            }
        });
    }

    /**
     * MQTT module settings UI
     */
    private void mqtt() {
        final CheckBoxPreference mqtt = (CheckBoxPreference) findPreference(Aware_Preferences.STATUS_MQTT);
        mqtt.setChecked(Aware.getSetting(awareContext, Aware_Preferences.STATUS_MQTT).equals("true"));
        mqtt.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Aware.getSetting(awareContext, Aware_Preferences.MQTT_SERVER).length() == 0) {
                    clientUI.showDialog(DIALOG_ERROR_MISSING_PARAMETERS);
                    mqtt.setChecked(false);
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_MQTT, false);
                    return false;
                } else {
                    Aware.setSetting(awareContext, Aware_Preferences.STATUS_MQTT, mqtt.isChecked());
                    if (mqtt.isChecked()) {
                        Aware.startMQTT(awareContext);
                    } else {
                        Aware.stopMQTT(awareContext);
                    }
                    return true;
                }
            }
        });

        final EditTextPreference mqttServer = (EditTextPreference) findPreference(Aware_Preferences.MQTT_SERVER);
        mqttServer.setText(Aware.getSetting(awareContext, Aware_Preferences.MQTT_SERVER));
        if (Aware.getSetting(awareContext, Aware_Preferences.MQTT_SERVER).length() > 0) {
            mqttServer.setSummary("Server: " + Aware.getSetting(awareContext, Aware_Preferences.MQTT_SERVER));
        }
        mqttServer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.MQTT_SERVER, (String) newValue);
                mqttServer.setSummary("Server: " + (String) newValue);
                return true;
            }
        });

        final EditTextPreference mqttPort = (EditTextPreference) findPreference(Aware_Preferences.MQTT_PORT);
        if (Aware.getSetting(awareContext, Aware_Preferences.MQTT_PORT).length() > 0) {
            mqttPort.setSummary(Aware.getSetting(awareContext, Aware_Preferences.MQTT_PORT));
        }
        mqttPort.setText(Aware.getSetting(awareContext, Aware_Preferences.MQTT_PORT));
        mqttPort.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.MQTT_PORT, (String) newValue);
                return true;
            }
        });

        final EditTextPreference mqttUsername = (EditTextPreference) findPreference(Aware_Preferences.MQTT_USERNAME);
        if (Aware.getSetting(awareContext, Aware_Preferences.MQTT_USERNAME).length() > 0) {
            mqttUsername.setSummary(Aware.getSetting(awareContext, Aware_Preferences.MQTT_USERNAME));
        }
        mqttUsername.setText(Aware.getSetting(awareContext, Aware_Preferences.MQTT_USERNAME));
        mqttUsername.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.MQTT_USERNAME, (String) newValue);
                return true;
            }
        });

        final EditTextPreference mqttPassword = (EditTextPreference) findPreference(Aware_Preferences.MQTT_PASSWORD);
        mqttPassword.setText(Aware.getSetting(awareContext, Aware_Preferences.MQTT_PASSWORD));
        mqttPassword.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.MQTT_PASSWORD, (String) newValue);
                return true;
            }
        });

        final EditTextPreference mqttKeepAlive = (EditTextPreference) findPreference(Aware_Preferences.MQTT_KEEP_ALIVE);
        if (Aware.getSetting(awareContext, Aware_Preferences.MQTT_KEEP_ALIVE).length() > 0) {
            mqttKeepAlive.setSummary(Aware.getSetting(awareContext, Aware_Preferences.MQTT_KEEP_ALIVE) + " seconds");
        }
        mqttKeepAlive.setText(Aware.getSetting(awareContext, Aware_Preferences.MQTT_KEEP_ALIVE));
        mqttKeepAlive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.MQTT_KEEP_ALIVE, (String) newValue);
                mqttKeepAlive.setSummary((String) newValue + " seconds");
                return true;
            }
        });

        final EditTextPreference mqttQoS = (EditTextPreference) findPreference(Aware_Preferences.MQTT_QOS);
        mqttQoS.setText(Aware.getSetting(awareContext, Aware_Preferences.MQTT_QOS));
        mqttQoS.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.MQTT_QOS, (String) newValue);
                return true;
            }
        });

        final EditTextPreference mqttProtocol = (EditTextPreference) findPreference(Aware_Preferences.MQTT_PROTOCOL);
        mqttProtocol.setText(Aware.getSetting(awareContext, Aware_Preferences.MQTT_PROTOCOL));
        mqttProtocol.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.MQTT_PROTOCOL, (String) newValue);
                return true;
            }
        });
    }

    /**
     * Developer UI options
     * - Debug flag
     * - Debug tag
     * - AWARE updates
     * - Device ID
     */
    public void developerOptions() {
        final CheckBoxPreference debug_flag = (CheckBoxPreference) findPreference(Aware_Preferences.DEBUG_FLAG);
        debug_flag.setChecked(Aware.getSetting(awareContext, Aware_Preferences.DEBUG_FLAG).equals("true"));
        debug_flag.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.DEBUG = debug_flag.isChecked();
                Aware.setSetting(awareContext, Aware_Preferences.DEBUG_FLAG, debug_flag.isChecked());
                return true;
            }
        });

        final EditTextPreference debug_tag = (EditTextPreference) findPreference(Aware_Preferences.DEBUG_TAG);
        debug_tag.setText(Aware.getSetting(awareContext, Aware_Preferences.DEBUG_TAG));
        debug_tag.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.TAG = (String) newValue;
                Aware.setSetting(awareContext, Aware_Preferences.DEBUG_TAG, (String) newValue);
                return true;
            }
        });

        final CheckBoxPreference auto_update = (CheckBoxPreference) findPreference(Aware_Preferences.AWARE_AUTO_UPDATE);
        auto_update.setChecked(Aware.getSetting(awareContext, Aware_Preferences.AWARE_AUTO_UPDATE).equals("true"));

        PackageInfo awareInfo = null;
        try {
            awareInfo = awareContext.getPackageManager().getPackageInfo(awareContext.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        auto_update.setSummary("Current version is " + ((awareInfo != null) ? awareInfo.versionCode : "???"));
        auto_update.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.AWARE_AUTO_UPDATE, auto_update.isChecked());
                if (auto_update.isChecked()) {
                    sendBroadcast(new Intent(Aware.ACTION_AWARE_CHECK_UPDATE));
                }
                return true;
            }
        });

        final CheckBoxPreference debug_db_slow = (CheckBoxPreference) findPreference(Aware_Preferences.DEBUG_DB_SLOW);
        debug_db_slow.setChecked(Aware.getSetting(awareContext, Aware_Preferences.DEBUG_DB_SLOW).equals("true"));
        debug_db_slow.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Aware.setSetting(awareContext, Aware_Preferences.DEBUG_DB_SLOW, debug_db_slow.isChecked());
                return true;
            }
        });

        final EditTextPreference device_id = (EditTextPreference) findPreference(Aware_Preferences.DEVICE_ID);
        device_id.setSummary("UUID: " + Aware.getSetting(awareContext, Aware_Preferences.DEVICE_ID));
        device_id.setText(Aware.getSetting(awareContext, Aware_Preferences.DEVICE_ID));
        device_id.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.DEVICE_ID, (String) newValue);
                device_id.setSummary("UUID: " + Aware.getSetting(awareContext, Aware_Preferences.DEVICE_ID));
                return true;
            }
        });

        final EditTextPreference group_id = (EditTextPreference) findPreference(Aware_Preferences.GROUP_ID);
        group_id.setSummary("Group: " + Aware.getSetting(awareContext, Aware_Preferences.GROUP_ID));
        group_id.setText(Aware.getSetting(awareContext, Aware_Preferences.GROUP_ID));
        group_id.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Aware.setSetting(awareContext, Aware_Preferences.GROUP_ID, (String) newValue);
                group_id.setSummary("Group: " + Aware.getSetting(awareContext, Aware_Preferences.GROUP_ID));
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if ((Aware.getSetting(getApplicationContext(), Aware_Preferences.STATUS_APPLICATIONS).equals("true") || Aware.getSetting(getApplicationContext(), Aware_Preferences.STATUS_KEYBOARD).equals("true")) && !Applications.isAccessibilityServiceActive(getApplicationContext())) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
            mBuilder.setSmallIcon(R.drawable.ic_stat_aware_accessibility);
            mBuilder.setContentTitle("AWARE configuration");
            mBuilder.setContentText(getResources().getString(R.string.aware_activate_accessibility));
            mBuilder.setDefaults(Notification.DEFAULT_ALL);
            mBuilder.setAutoCancel(true);

            Intent accessibilitySettings = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            accessibilitySettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent clickIntent = PendingIntent.getActivity(getApplicationContext(), 0, accessibilitySettings, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(clickIntent);
            NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notManager.notify(Applications.ACCESSIBILITY_NOTIFICATION_ID, mBuilder.build());
        }

        if (Aware.getSetting(getApplicationContext(), "study_id").length() > 0) {
            new Aware_Activity.Async_StudyData().execute(Aware.getSetting(this, Aware_Preferences.WEBSERVICE_SERVER));
        }
    }

    /**
     * Sets first all the settings to the client.
     * If there are plugins, apply the same settings to them.
     * This allows us to add plugins to studies from the dashboard.
     *
     * @param context
     * @param configs
     */
    protected static void applySettings(Context context, JSONArray configs) {

        boolean is_developer = Aware.getSetting(context, Aware_Preferences.DEBUG_FLAG).equals("true");

        //First reset the client to default settings...
        Aware.reset(context);

        if (is_developer) Aware.setSetting(context, Aware_Preferences.DEBUG_FLAG, true);

        //Now apply the new settings
        JSONArray plugins = new JSONArray();
        JSONArray sensors = new JSONArray();

        for (int i = 0; i < configs.length(); i++) {
            try {
                JSONObject element = configs.getJSONObject(i);
                if (element.has("plugins")) {
                    plugins = element.getJSONArray("plugins");
                }
                if (element.has("sensors")) {
                    sensors = element.getJSONArray("sensors");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //Set the sensors' settings first
        for (int i = 0; i < sensors.length(); i++) {
            try {
                JSONObject sensor_config = sensors.getJSONObject(i);
                Aware.setSetting(context, sensor_config.getString("setting"), sensor_config.get("value"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //Set the plugins' settings now
        ArrayList<String> active_plugins = new ArrayList<>();
        for (int i = 0; i < plugins.length(); i++) {
            try {
                JSONObject plugin_config = plugins.getJSONObject(i);

                String package_name = plugin_config.getString("plugin");
                active_plugins.add(package_name);

                JSONArray plugin_settings = plugin_config.getJSONArray("settings");
                for (int j = 0; j < plugin_settings.length(); j++) {
                    JSONObject plugin_setting = plugin_settings.getJSONObject(j);
                    Aware.setSetting(context, plugin_setting.getString("setting"), plugin_setting.get("value"), package_name);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //Now check plugins
        new CheckPlugins(context).execute(active_plugins);

        //Send data to server
        Intent sync = new Intent(Aware.ACTION_AWARE_SYNC_DATA);
        context.sendBroadcast(sync);

        Intent applyNew = new Intent(Aware.ACTION_AWARE_REFRESH);
        context.sendBroadcast(applyNew);
    }

    public static class CheckPlugins extends AsyncTask<ArrayList<String>, Void, Void> {
        private Context context;

        public CheckPlugins(Context c) {
            this.context = c;
        }

        @Override
        protected Void doInBackground(ArrayList<String>... params) {

            String study_url = Aware.getSetting(context, Aware_Preferences.WEBSERVICE_SERVER);
            String study_host = study_url.substring(0, study_url.indexOf("/index.php"));
            String protocol = study_url.substring(0, study_url.indexOf(":"));

            for (final String package_name : params[0]) {

                String http_request;
                if (protocol.equals("https")) {
                    try {
                        http_request = new Https(context, SSLManager.getHTTPS(context, study_url)).dataGET(study_host + "/index.php/plugins/get_plugin/" + package_name, true);
                    } catch (FileNotFoundException e) {
                        http_request = null;
                    }
                } else {
                    http_request = new Http(context).dataGET(study_host + "/index.php/plugins/get_plugin/" + package_name, true);
                }

                if (http_request != null) {
                    try {
                        if (!http_request.equals("[]")) {
                            JSONObject json_package = new JSONObject(http_request);
                            if (json_package.getInt("version") > Plugins_Manager.getVersion(context, package_name)) {
                                Aware.downloadPlugin(context, package_name, true); //update the existing plugin
                            } else {
                                PackageInfo installed = Plugins_Manager.isInstalled(context, package_name);
                                if (installed != null) {
                                    Aware.startPlugin(context, package_name); //start plugin
                                } else {

                                    //We don't have the plugin installed or bundled. Ask to install?
                                    if (Aware.DEBUG)
                                        Log.d(Aware.TAG, package_name + " is not installed yet!");

                                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);
                                    builder.setTitle("AWARE")
                                            .setMessage("Install necessary plugin(s)?")
                                            .setPositiveButton("Install", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Aware.downloadPlugin(context, package_name, false);
                                                }
                                            })
                                            .setNegativeButton("Cancel", null).show();
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }
}