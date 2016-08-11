package com.aware.utils;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.R;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Random;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by denzil on 17/01/15.
 */
public class DownloadPluginService extends IntentService {

    public DownloadPluginService() { super(Aware.TAG + " Plugin Downloader"); }

    @Override
    protected void onHandleIntent(Intent intent) {

        final NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String package_name = intent.getStringExtra("package_name");
        boolean is_update = intent.getBooleanExtra("is_update", false);

        if( Aware.DEBUG ) Log.d(Aware.TAG, "Trying to download: " + package_name);

        String study_url = Aware.getSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER);
        String study_host = study_url.substring(0, study_url.indexOf("/index.php"));
        String protocol = study_url.substring(0, study_url.indexOf(":"));

        String response;
        if( protocol.equals("https") ) {
            try {
                response = new Https(getApplicationContext(), SSLManager.getHTTPS(getApplicationContext(), study_url)).dataGET( study_host + "/index.php/plugins/get_plugin/" + package_name, true);
            } catch (FileNotFoundException e ) {
                response = null;
            }
        } else {
            response = new Http(getApplicationContext()).dataGET( study_host + "/index.php/plugins/get_plugin/" + package_name, true);
        }

        if( response != null ) {
            try {
                if(response.trim().equalsIgnoreCase("[]")) return;

                JSONObject json_package = new JSONObject(response);

                //Create the folder where all the plugins will be stored on external storage
                File folders = new File( getExternalFilesDir(null) + "/Documents/AWARE/", "plugins");
                folders.mkdirs();

                String package_url = study_host + json_package.getString("package_path") + json_package.getString("package_name");

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
                mBuilder.setSmallIcon(R.drawable.ic_action_aware_plugins);
                mBuilder.setContentTitle("AWARE Plugin");
                mBuilder.setContentText(((is_update) ? "Updating " : "Downloading ") + json_package.getString("title"));
                mBuilder.setProgress(0, 0, true);
                mBuilder.setAutoCancel(true);

                final int notID = new Random(System.currentTimeMillis()).nextInt();
                notManager.notify(notID, mBuilder.build());

                if( protocol.equals("https") ) { //Load SSL public certificate so we can talk with server

                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    InputStream caInput = SSLManager.getHTTPS(getApplicationContext(), study_url);
                    Certificate ca = cf.generateCertificate(caInput);
                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keyStore.load(null, null); //initialize as empty keystore
                    keyStore.setCertificateEntry("ca", ca); //add our certificate to keystore

                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init(keyStore); //add our keystore to the trusted keystores

                    //Initialize a SSL connection context
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

                    //Fix for known-bug on <= JellyBean (4.x)
                    System.setProperty("http.keepAlive", "false");

                    Ion.getDefault(getApplicationContext())
                            .getHttpClient()
                            .getSSLSocketMiddleware().setTrustManagers(trustManagerFactory.getTrustManagers());
                    Ion.getDefault(getApplicationContext())
                            .getHttpClient()
                            .getSSLSocketMiddleware().setSSLContext(sslContext);

                    Ion.with(getApplicationContext())
                            .load(package_url)
                            .write(new File(getExternalFilesDir(null)+"/Documents/AWARE/plugins/" + json_package.getString("package_name")))
                            .setCallback(new FutureCallback<File>() {
                                @Override
                                public void onCompleted(Exception e, File result) {
                                    if (result != null) {

                                        notManager.cancel(notID);

                                        Intent promptInstall = new Intent(Intent.ACTION_VIEW);
                                        promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        promptInstall.setDataAndType(Uri.fromFile(result), "application/vnd.android.package-archive");
                                        startActivity(promptInstall);
                                    }
                                }
                            });
                } else {
                    Ion.with(getApplicationContext())
                            .load(package_url)
                            .write(new File(getExternalFilesDir(null)+"/Documents/AWARE/plugins/" + json_package.getString("package_name")))
                            .setCallback(new FutureCallback<File>() {
                                @Override
                                public void onCompleted(Exception e, File result) {
                                    if( result != null ) {

                                        notManager.cancel(notID);

                                        Intent promptInstall = new Intent(Intent.ACTION_VIEW);
                                        promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        promptInstall.setDataAndType(Uri.fromFile(result), "application/vnd.android.package-archive");
                                        startActivity(promptInstall);
                                    }
                                }
                            });
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
