package com.gisbase.geomobile.plugin;

import com.gisbase.geomobile.FileProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

/**
 * This class echoes a string called from JavaScript.
 */
public class GeomobilePlugin extends CordovaPlugin {

    private static String TAG = GeomobilePlugin.class.getSimpleName();
    private static String NULL = "null";
    public static final String EVENTNAME_ERROR = "geomobile error.";
    public static final String GEOMOBILE_APP = "com.gisbase.geomobile";
    public static final String GEOMOBILE_READY = "com.gisbase.geomobile.ready";
    public static final String GEOMOBILE_LOAD = "com.gisbase.geomobile.load";
    public static final String GEOMOBILE_UPDATES = "com.gisbase.geomobile.updates";

    private File dataFile;

    final java.util.Map<String, BroadcastReceiver> receiverMap =
            new java.util.HashMap<String, BroadcastReceiver>(10);

    /**
     * @param eventName
     * @param data
     * @param <T>
     */
    protected <T> void fireEvent(final String eventName, final String data) {

        cordova.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String method = null;

                if (data == null) {
                    method = String.format("javascript:window.GeomobilePlugin.fireEvent( '%s', null );", eventName);
                } else {
                    method = String.format("javascript:window.GeomobilePlugin.fireEvent( '%s', %s );", eventName, data);
                }
                GeomobilePlugin.this.webView.loadUrl(method);
            }
        });
    }

    /**
     * @param receiver
     * @param filter
     */
    protected void registerReceiver(android.content.BroadcastReceiver receiver, android.content.IntentFilter filter) {
        this.webView.getContext().registerReceiver(receiver, filter);
        //LocalBroadcastManager.getInstance(super.webView.getContext()).registerReceiver(receiver, filter);
    }


    /**
     * @param receiver
     */
    protected void unregisterReceiver(android.content.BroadcastReceiver receiver) {
		Log.v(TAG, String.format("unregisterReceiver: %s", receiver.toString()));
        LocalBroadcastManager.getInstance(super.webView.getContext()).unregisterReceiver(receiver);
    }

    private File createFile(String name, String data){
        Context context = cordova.getActivity().getApplicationContext();
        Log.v(TAG, String.format("context: %s", context.toString()));

        File baseDir = context.getFilesDir();
        Log.v(TAG, String.format("base folder %s", baseDir.getAbsoluteFile()));
        File dataDir = new File(baseDir, "geomobile-data");
        Log.v(TAG, String.format("Created folder %s", dataDir.getAbsoluteFile()));
        File file = new File(dataDir, name+".json");
        Log.v(TAG, String.format("Created file %s", dataDir.getAbsoluteFile()));

        FileOutputStream FoutS = null;
        OutputStreamWriter outSW = null;

        try {
            dataDir.mkdirs();
            file.createNewFile();

            FoutS = new FileOutputStream(file);
            outSW = new OutputStreamWriter(FoutS);

            outSW.write(data);
            outSW.flush();

            Log.v(TAG, String.format("writed data: %s", data));

        } catch (Exception e) {
            file = null;
            e.printStackTrace();
        }
        finally {
            try {
                outSW.close();
                FoutS.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    private void sendBroadcastData(String app, String action, File dataFile){
        Log.v(TAG, String.format("app %s | action %s", app, action));

        Context context = cordova.getActivity().getApplicationContext();
        Log.v(TAG, String.format("context: %s", context.toString()));

        Log.v(TAG, String.format("authority: %s", context.getPackageName()+".transfer.fileprovider"));

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName()+".transfer.fileprovider", dataFile);

        Log.v(TAG, String.format("uri: %s", uri.toString()));

        final Intent intent = new Intent()
                .setAction(action)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setDataAndType(uri, "text/plain");

        context.grantUriPermission(app, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        this.webView.getContext().sendBroadcast(intent);
    }

    private void receiveFileData(Context context, final Intent intent, String eventName) {
        Uri uri = intent.getData();
        Log.v(TAG, String.format("context PackageName: %s", context.getPackageName()));
        Log.v(TAG, String.format("intent PackageName: %s", intent.getPackage()));
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fd = pfd.getFileDescriptor();
            InputStream iStream = context.getContentResolver().openInputStream(uri);
            Scanner s = new Scanner(iStream).useDelimiter("\\A");
            String data = s.hasNext() ? s.next() : "";
            Log.v(TAG, String.format("file data: %s", data));
            fireEvent(eventName, data);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MainActivity", "File not found.");
            return;
        }
    }

    private void openApp(String app){
        PackageManager manager = cordova.getActivity().getApplicationContext().getPackageManager();
        Intent launchIntent = manager.getLaunchIntentForPackage(app);
        Log.v(TAG, String.format("openning app: %s", app));
        cordova.getActivity().startActivity(launchIntent);
    }

    private void sendGeomobileData(){
		if(dataFile == null){
			Log.v(TAG, "temporary fix: dataFile is null");
			return;
		}
        sendBroadcastData(GEOMOBILE_APP, GEOMOBILE_LOAD, dataFile);
    }
	
	private boolean registerUpdatesListener() throws JSONException {
		if (!receiverMap.containsKey(GEOMOBILE_UPDATES)) {
            final BroadcastReceiver geomobileReadyReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, final Intent intent) {
                    goAsync();
                    Log.v(TAG, String.format("received event: %s", GEOMOBILE_UPDATES));
                    receiveFileData(context, intent, GEOMOBILE_UPDATES);
                }
            };

            try {
                registerReceiver(geomobileReadyReceiver, new IntentFilter(GEOMOBILE_UPDATES, "text/plain"));
                receiverMap.put(GEOMOBILE_UPDATES, geomobileReadyReceiver);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
	}

    private boolean open(JSONObject data, Integer flags, String category) throws JSONException {
        Context context = cordova.getActivity().getApplicationContext();
        data.put("appCallback", context.getPackageName());
        dataFile = createFile("openData", data.toString());
        if(dataFile == null){
            Log.e(TAG, "dataFile null");
            return false;
        }

        //registrar evento do GEOMOBILE_UPDATES
        if(registerUpdatesListener() == false){
			Log.e(TAG, "failed to register GEOMOBILE_UPDATES");
			return false;
		}

        //registrar evento do GEOMOBILE_READY
        if (!receiverMap.containsKey(GEOMOBILE_READY)) {
            final BroadcastReceiver geomobileReadyReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, final Intent intent) {
                    goAsync();
                    Log.v(TAG, String.format("received event: %s", GEOMOBILE_READY));
                    //receiveFile(context, intent, GEOMOBILE_READY);
                    sendGeomobileData();
					dataFile = null;
                    unregisterReceiver(receiverMap.get(GEOMOBILE_READY));
                    receiverMap.remove(receiverMap.get(GEOMOBILE_READY));
                }
            };

            try {
                registerReceiver(geomobileReadyReceiver, new IntentFilter(GEOMOBILE_READY, "text/plain"));
                receiverMap.put(GEOMOBILE_READY, geomobileReadyReceiver);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        //abrir o app
        openApp(GEOMOBILE_APP);

        return true;
    }
	
	private boolean receivedMessage(String message) throws JSONException {
		String receivedMessage = message + ".received";
		sendBroadcastData(GEOMOBILE_APP, receivedMessage, createFile("receivedMessage", "\""+receivedMessage+"\""));
		return true;
	}

    /**
     * @param action          The action to execute.
     * @param args            The exec() arguments.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return
     * @throws JSONException
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        boolean result = false;
        if (action.equals("openGeoMobile")) {
            final JSONObject data = args.getJSONObject(0);
            Log.v(TAG, String.format("openGeoMobile data: %s", data.toString()));

            /*final int flags = args.getInt(1);

            final String category = args.getString(2);*/

            result = open(data, null, null);

        } else if (action.equals("closeGeoMobile")) {
            clearReceivers();
            result = true;

        } else if (action.equals("registerListenerGeoMobile")) {
            result = registerUpdatesListener();

        } else if (action.equals("openWithoutDataGeoMobile")) {
            openApp(GEOMOBILE_APP);
            result = true;

        } else if (action.equals("receivedMessage")) {
			String message = args.getString(0);
            receivedMessage(message);
            result = true;

        }

        if(result) {
            callbackContext.success();
        } else{
            callbackContext.error(EVENTNAME_ERROR);
        }
        return result;
    }

    private void clearReceivers(){
        for (BroadcastReceiver r : receiverMap.values()) {
            unregisterReceiver(r);
        }

        receiverMap.clear();
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
        // deregister receiver
        clearReceivers();

        super.onDestroy();

    }

}