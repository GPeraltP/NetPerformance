package cordova.plugin.netperformance;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;

import android.os.Build;

import android.Manifest;

import android.app.Activity;
import android.util.AndroidRuntimeException;
import android.util.Log;

/**
 * This class echoes a string called from JavaScript.
 */
public class NetPerformance extends CordovaPlugin {

    private static final String ACTION_START_PERFORM = "startNetPerformData";
    private static final String ACTION_STOP_PERFORM = "stopNetPerformData";
    private static final String ACTION_REQUEST_PERMISSION = "requestRequiredPermission";
    private static final String ACTION_ENABLE_GPS_DIALOG = "enableGPSDialog";

    private static final String KEY_PHONE = "KEY_PHONE";
    private static final String KEY_IMEI = "KEY_IMEI";
    private static final String KEY_BRAND = "KEY_BRAND";
    private static final String KEY_MODEL = "KEY_MODEL";

    private static final int PERMISSION_REQUEST_CODE = 100;
    public static final int REQUEST_CHECK_SETTING = 1001; //Request code for GPS Dialog
    private LocationRequest locationRequest;
    private CallbackContext newCallbackContext = null;
    private Activity activity;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        activity = cordova.getActivity();
        JSONObject jsonArgs = new JSONObject();
        try{
            jsonArgs = args.getJSONObject(0);
        }catch (JSONException e){
            Log.i("NetPerformance", e.getMessage());
        }

        if (ACTION_REQUEST_PERMISSION.equals(action)) {
            
            this.requestPermission(callbackContext);
            return true;
        }else if(ACTION_ENABLE_GPS_DIALOG.equals(action)){
            newCallbackContext = callbackContext;
            cordova.setActivityResultCallback (this); //necessary to call onActivityResult
            this.enableGPS();
            return true;
        }else if(ACTION_START_PERFORM.equals(action)){
            JSONObject finalJsonArgs = jsonArgs;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String phone = "", imei = "", brand = "", model = "";

                    try {
                        phone = finalJsonArgs.getString(KEY_PHONE);
                        imei = finalJsonArgs.getString(KEY_IMEI);
                        brand = finalJsonArgs.getString(KEY_BRAND);
                        model = finalJsonArgs.getString(KEY_MODEL);
                    }catch (JSONException e){
                        Log.i("NetPerform",e.getMessage());
                    }
                    Intent i = new Intent(activity, ServiceNet.class);
                    i.putExtra(KEY_PHONE,phone);
                    i.putExtra(KEY_IMEI,imei);
                    i.putExtra(KEY_BRAND,brand);
                    i.putExtra(KEY_MODEL,model);
                    activity.getApplicationContext().startService(i);
                    callbackContext.success();
                }
            });
            return true;
        }else if(ACTION_STOP_PERFORM.equals(action)){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.getApplicationContext().stopService(new Intent(activity, ServiceNet.class));
                    callbackContext.success();
                }
            });
            return true;
        }
        return false;
    }

    private void requestPermission(CallbackContext callbackContext) {
        newCallbackContext = callbackContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_PHONE_NUMBERS,
                            Manifest.permission.READ_SMS,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE                            
                    };
            cordova.requestPermissions(
                    this,
                    PERMISSION_REQUEST_CODE,
                    permissions
            );
        } else {
            newCallbackContext.success(1);
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions,int[] grantResults) throws JSONException {
        if(requestCode == PERMISSION_REQUEST_CODE){
            for(int r:grantResults)
            {
                if(r == PackageManager.PERMISSION_DENIED)
                {
                    newCallbackContext.error(0);
                    return;
                }
            }
            newCallbackContext.success(1);
            newCallbackContext = null;
        }
        
    }

    private void enableGPS() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(cordova.getActivity().getApplicationContext()).checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete( Task<LocationSettingsResponse> task) {
                try {
                    //When GPS is Active send code 1
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,"1");
                    newCallbackContext.sendPluginResult(pluginResult);
                }catch (ApiException e){
                    switch (e.getStatusCode()){
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException)e;
                                resolvableApiException.startResolutionForResult(cordova.getActivity(),REQUEST_CHECK_SETTING);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }
                        break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }
                }

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Check Answer Google GPS Dialog
        PluginResult pluginResult;
        if (requestCode == REQUEST_CHECK_SETTING){
            switch (resultCode){

                case Activity.RESULT_OK:
                    pluginResult = new PluginResult(PluginResult.Status.OK,"1");
                    newCallbackContext.sendPluginResult(pluginResult);
                break;

                case Activity.RESULT_CANCELED:
                    pluginResult = new PluginResult(PluginResult.Status.ERROR,"0");
                    newCallbackContext.sendPluginResult(pluginResult);
                break;
            }
        }
    }
   /* private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(this.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    } */

    private void addProperty(JSONObject obj, String key, Object value) {
        try {
            if (value == null) {
                obj.put(key, JSONObject.NULL);
            } else {
                obj.put(key, value);
            }
        } catch (JSONException ignored) {
            //Believe exception only occurs when adding duplicate keys, so just ignore it
        }
    }

}
