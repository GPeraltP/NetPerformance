package cordova.plugin.netperformance;

import cordova.plugin.netperformance.ServiceNet;
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
import android.content.Context;

import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.TelephonyManager;

import android.util.Log;

import android.os.Build;
import android.os.Bundle;

import android.widget.Toast;

import android.Manifest;

import android.app.Activity;
import android.app.ActivityManager;

/**
 * This class echoes a string called from JavaScript.
 */
public class NetPerformance extends CordovaPlugin {

    private static final String ACTION_START_PERFORM = "startNetPerformData";
    private static final String ACTION_STOP_PERFORM = "stopNetPerformData";
    private static final String ACTION_REQUEST_PERMISSION = "requestRequiredPermission";
    private static final String ACTION_ENABLE_GPS_DIALOG = "enableGPSDialog";

    private static final int PERMISSION_REQUEST_CODE = 100;

    private CallbackContext newCallbackContext = null;
    private Activity activity;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        activity = cordova.getActivity();

        if (ACTION_REQUEST_PERMISSION.equals(action)) {
            
            this.requestPermission(callbackContext);
            return true;
        }else if(ACTION_ENABLE_GPS_DIALOG.equals(action)){

            return true;
        }else if(ACTION_START_PERFORM.equals(action)){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.getApplicationContext().startService(new Intent(activity, ServiceNet.class));
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
