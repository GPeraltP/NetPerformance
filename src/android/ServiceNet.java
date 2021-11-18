package cordova.plugin.netperformance;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import static android.location.LocationManager.GPS_PROVIDER;

public class ServiceNet extends Service {

    private Timer time;
    private String downloadSpeed = "";
    private String uploadSpeed = "";
    private SpeedDownloadTestTask speedDownload = new SpeedDownloadTestTask();
    private SpeedUploadTestTask speedUpload = new SpeedUploadTestTask();
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        speedDownload.execute();
        speedUpload.execute();
        time = new Timer();
        time.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                JSONObject r = new JSONObject();
                try {
                    r.put("simOperatorName", getSimOperatorName());
                    r.put("simOperator", getSimOperator());
                    r.put("networkOperatorName", getNetworkOperatorName());
                    r.put("networkOperator", getNetworkOperator());
                    r.put("networkCountryIso", getNetworkCountryIso());
                    r.put("deviceSoftwareVersion", getDeviceSoftwareVersion());
                    r.put("phoneType", getPhoneType());
                    r.put("isNetworkRoaming", isNetworkRoaming());
                    r.put("simState", getSimState());
                    r.put("networkType", getNetworkType());
                    r.put("callState", getCallState());
                    r.put("dataState", getDataState());
                    r.put("groupIdLevel", getGroupIdLevel1());
                    r.put("simCountryIso", getSimCountryIso());
                    r.put("voiceMailAlphaTag", getVoiceMailAlphaTag());
                    r.put("voiceMailNumber", getVoiceMailNumber());
                    r.put("hasIccCard", hasIccCard());
                    r.put("dataActivity", getDataActivity());
                    r.put("signalQuality", getSignalQuality());
//                r.put("latitude", getLatitude());
//                r.put("longitude", getLongitude());
                    r.put("location", getLocation());
//                r.put("imsi", getMSISDN()); Por OS
//                r.put("model", getModel());

                    r.put("downloadSpeed", downloadSpeed);
                    r.put("uploadSpeed", uploadSpeed);
                    r.put("bands", getNetworkBands());
                    r.put("eNodeB", getENodeB());
                    r.put("cellId", getCellId());

                    //String data = formatJsonToCsv(r);
                    //uploadWithTransferUtility(data);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.i("tag", "Get speeds test every 5 seconds :: "+r.toString());
            }
        },3000,5000);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        speedDownload.cancel(true);
        speedUpload.cancel(true);
        time.cancel();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i("TAG", "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i("TAG", "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d("TAG", "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i("TAG", "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d("TAG", "gps provider does not exist " + ex.getMessage());
        }
    }

    public JSONObject getNetworkBands() {
        JSONObject bands = new JSONObject();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return bands;
                }
                List<CellInfo> info = tm.getAllCellInfo();
                for (CellInfo i :
                        info) {
                    if (i instanceof CellInfoLte) {
                        bands.put("Earfnc", ((CellInfoLte) i).getCellIdentity().getEarfcn());
                    }
                    if (i instanceof CellInfoGsm) {
                        bands.put("Arfcn", ((CellInfoGsm) i).getCellIdentity().getArfcn());
                    }
                    if (i instanceof CellInfoWcdma) {
                        bands.put("Uarfcn", ((CellInfoWcdma) i).getCellIdentity().getUarfcn());
                    }
                }
            }
        }catch (JSONException e){
            Log.e("NetPerformance", "getNetworkBands: ", e);
            return bands;
        }

        return bands;
    }

    public String getSimOperatorName() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getSimOperatorName();
    }

    public String getSimOperator() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getSimOperator();
    }

    public String getNetworkOperatorName() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getNetworkOperatorName();
    }

    public String getNetworkOperator() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getNetworkOperator();
    }

    public String getNetworkCountryIso() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getNetworkCountryIso();
    }

    public String getDeviceSoftwareVersion() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        return tm.getDeviceSoftwareVersion();
    }

    public String getPhoneType() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int phoneType = tm.getPhoneType();
        String returnValue = "";
        switch (phoneType) {
            case (TelephonyManager.PHONE_TYPE_CDMA):
                returnValue = "CDMA";
                break;
            case (TelephonyManager.PHONE_TYPE_GSM):
                returnValue = "GSM";
                break;
            case (TelephonyManager.PHONE_TYPE_NONE):
                returnValue = "NONE";
                break;
            case (TelephonyManager.PHONE_TYPE_SIP):
                returnValue = "SIP";
                break;
        }

        return returnValue;
    }

    public String isNetworkRoaming() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        boolean isRoaming = tm.isNetworkRoaming();
        String returnValue;
        if (isRoaming) {
            returnValue = "YES";
        } else {
            returnValue = "NO";
        }
        return returnValue;
    }

    public String getSimState() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int simState = tm.getSimState();
        String returnValue = "";
        switch (simState) {
            case (TelephonyManager.SIM_STATE_ABSENT):
                returnValue = "ABSENT";
                break;
            case (TelephonyManager.SIM_STATE_NETWORK_LOCKED):
                returnValue = "NETWORK_LOCKED";
                break;
            case (TelephonyManager.SIM_STATE_PIN_REQUIRED):
                returnValue = "PIN_REQUIRED";
                break;
            case (TelephonyManager.SIM_STATE_PUK_REQUIRED):
                returnValue = "PUK_REQUIRED";
                break;
            case (TelephonyManager.SIM_STATE_READY):
                returnValue = "READY";
                break;
            case (TelephonyManager.SIM_STATE_UNKNOWN):
                returnValue = "UNKNOWN";
                break;
        }
        return returnValue;
    }

    public String getNetworkType() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = 0;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            networkType = tm.getDataNetworkType();
        } else {
            networkType = tm.getNetworkType();
        }
        String returnValue = "";
        switch (networkType) {
            case (TelephonyManager.NETWORK_TYPE_1xRTT):
                returnValue = "1xRTT";
                break;
            case (TelephonyManager.NETWORK_TYPE_CDMA):
                returnValue = "CDMA";
                break;
            case (TelephonyManager.NETWORK_TYPE_EDGE):
                returnValue = "EDGE";
                break;
            case (TelephonyManager.NETWORK_TYPE_EVDO_0):
                returnValue = "EVDO_0";
                break;
            case (TelephonyManager.NETWORK_TYPE_LTE):
                returnValue = "LTE";
                break;
            case (TelephonyManager.NETWORK_TYPE_GPRS):
                returnValue = "GPRS";
                break;
            case (TelephonyManager.NETWORK_TYPE_UNKNOWN):
                returnValue = "UNKNOWN";
                break;
        }
        return returnValue;
    }

    public String getCallState() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int callState = tm.getCallState();
        String returnValue = "";
        switch (callState) {
            case (TelephonyManager.CALL_STATE_RINGING):
                returnValue = "RINGING";
                break;
            case (TelephonyManager.CALL_STATE_OFFHOOK):
                returnValue = "OFFHOOK";
                break;
            case (TelephonyManager.CALL_STATE_IDLE):
                returnValue = "IDLE";
                break;
        }
        return returnValue;
    }

    public String getDataState() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int dataState = tm.getDataState();
        String returnValue = "";
        switch (dataState) {
            case (TelephonyManager.DATA_DISCONNECTED):
                returnValue = "DISCONNECTED";
                break;
            case (TelephonyManager.DATA_CONNECTING):
                returnValue = "CONNECTING";
                break;
            case (TelephonyManager.DATA_CONNECTED):
                returnValue = "CONNECTED";
                break;
            case (TelephonyManager.DATA_SUSPENDED):
                returnValue = "SUSPENDED";
                break;
        }
        return returnValue;
    }

    public String getGroupIdLevel1() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        return tm.getGroupIdLevel1();
    }

    public String getSimCountryIso() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getSimCountryIso();
    }

    public String getVoiceMailAlphaTag() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        return tm.getVoiceMailAlphaTag();
    }

    public String getVoiceMailNumber() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        return tm.getVoiceMailNumber();
    }

    public String hasIccCard() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        boolean hasIccCard = tm.hasIccCard();
        String returnValue;
        if (hasIccCard) {
            returnValue = "TRUE";
        } else {
            returnValue = "FALSE";
        }
        return returnValue;
    }

    public String getDataActivity() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int dataActivity = tm.getDataActivity();
        String returnValue = "";
        switch (dataActivity) {
            case (TelephonyManager.DATA_ACTIVITY_NONE):
                returnValue = "NONE";
                break;
            case (TelephonyManager.DATA_ACTIVITY_IN):
                returnValue = "IN";
                break;
            case (TelephonyManager.DATA_ACTIVITY_OUT):
                returnValue = "OUT";
                break;
            case (TelephonyManager.DATA_ACTIVITY_INOUT):
                returnValue = "INOUT";
                break;
            case (TelephonyManager.DATA_ACTIVITY_DORMANT):
                returnValue = "DORMANT";
                break;
        }
        return returnValue;
    }

    public JSONObject getLocation() throws JSONException {
        JSONObject jLocation = new JSONObject();
      //  LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            jLocation.put("latitude", "");
            jLocation.put("longitude", "");

            return jLocation;
        }

        Location location;
        location = mLocationManager.getLastKnownLocation(GPS_PROVIDER);

        try {
            jLocation.put("latitude", Double.toString(location.getLatitude()));
            jLocation.put("longitude", Double.toString(location.getLongitude()));

        } catch (NullPointerException e) {
            jLocation.put("latitude", "");
            jLocation.put("longitude", "");
        }

        return jLocation;
    }

    public String getENodeB() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        List<CellInfo> info = tm.getAllCellInfo();
        for (CellInfo i:info) {
            if (i instanceof CellInfoLte){
                int longCid = ((CellInfoLte) i).getCellIdentity().getCi();

                String longCidHex = DecToHex(longCid);
                String eNBHex = longCidHex.substring(0, longCidHex.length()-2);

                int eNB = HexToDec(eNBHex);

                return Integer.toString(eNB);
            }
        }

        return "";
    }

    public String getCellId() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        List<CellInfo> info = tm.getAllCellInfo();
        for (CellInfo i:info) {
            if (i instanceof CellInfoLte){
                int longCid = ((CellInfoLte) i).getCellIdentity().getCi();

                String longCidHex = DecToHex(longCid);
                String cellIdHex = longCidHex.substring(longCidHex.length()-2);

                int cellId = HexToDec(cellIdHex);

                return Integer.toString(cellId);
            }
        }

        return "";
    }

    // Decimal -> hexadecimal
    public String DecToHex(int dec){
        return String.format("%x", dec);
    }

    // hex -> decimal
    public int HexToDec(String hex){
        return Integer.parseInt(hex, 16);
    }

    public JSONObject getSignalQuality() {
        JSONObject jsonQuality = new JSONObject();
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return new JSONObject();
        }
        List<CellInfo> cellInfoList = tm.getAllCellInfo();
        int qualitySignal;
        for (CellInfo cellInfo : cellInfoList) {
            if (cellInfo instanceof CellInfoLte) {
                try {
                    jsonQuality.put("Dbm",String.valueOf(((CellInfoLte)cellInfo).getCellSignalStrength().getDbm()));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        jsonQuality.put("Rsrp",String.valueOf(((CellInfoLte)cellInfo).getCellSignalStrength().getRsrp()));
                        jsonQuality.put("Rsrq",String.valueOf(((CellInfoLte)cellInfo).getCellSignalStrength().getRsrq()));
                        jsonQuality.put("Cqi",String.valueOf(((CellInfoLte)cellInfo).getCellSignalStrength().getCqi()));
                        jsonQuality.put("Rssnr",String.valueOf(((CellInfoLte)cellInfo).getCellSignalStrength().getRssnr()));
                    }else{
                        jsonQuality.put("Rsrp","");
                        jsonQuality.put("Rsrq","");
                        jsonQuality.put("Cqi","");
                        jsonQuality.put("Rssnr","");
                    }
                    qualitySignal = ((CellInfoLte) cellInfo).getCellSignalStrength().getLevel();
                    if (qualitySignal == 1) {
                        jsonQuality.put("QualitySignal", "Pobre -" + qualitySignal);
                    } else if (qualitySignal == 2) {
                        jsonQuality.put("QualitySignal", "Moderado -" + qualitySignal);
                    } else if (qualitySignal == 3) {
                        jsonQuality.put("QualitySignal", "Bueno -" + qualitySignal);
                    } else if (qualitySignal == 4) {
                        jsonQuality.put("QualitySignal", "Estupendo -" + qualitySignal);
                    } else if (qualitySignal == 0) {
                        jsonQuality.put("QualitySignal", "Nulo -" + qualitySignal);
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }

            }else if(cellInfo instanceof CellInfoGsm){
                qualitySignal = ((CellInfoGsm) cellInfo).getCellSignalStrength().getLevel();
                try {
                    jsonQuality.put("Dbm",String.valueOf(((CellInfoGsm)cellInfo).getCellSignalStrength().getDbm()));
                    jsonQuality.put("Rsrp","");
                    jsonQuality.put("Rsrq","");
                    jsonQuality.put("Cqi","");
                    jsonQuality.put("Rssnr","");
                    if (qualitySignal == 1) {
                        jsonQuality.put("QualitySignal", "Pobre -" + qualitySignal);
                    } else if (qualitySignal == 2) {
                        jsonQuality.put("QualitySignal", "Moderado -" + qualitySignal);
                    } else if (qualitySignal == 3) {
                        jsonQuality.put("QualitySignal", "Bueno -" + qualitySignal);
                    } else if (qualitySignal == 4) {
                        jsonQuality.put("QualitySignal", "Estupendo -" + qualitySignal);
                    } else if (qualitySignal == 0) {
                        jsonQuality.put("QualitySignal", "Nulo -" + qualitySignal);
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }

            }else if(cellInfo instanceof CellInfoWcdma){
                try {
                    jsonQuality.put("Dbm",String.valueOf(((CellInfoWcdma)cellInfo).getCellSignalStrength().getDbm()));
                    jsonQuality.put("Rsrp","");
                    jsonQuality.put("Rsrq","");
                    jsonQuality.put("Cqi","");
                    jsonQuality.put("Rssnr","");
                    jsonQuality.put("QualitySignal", "");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return jsonQuality;
    }

    public class SpeedDownloadTestTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            SpeedTestSocket speedTestSocket = new SpeedTestSocket();

            // add a listener to wait for speedtest completion and progress
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                @Override
                public void onCompletion(SpeedTestReport report) {
                    // called when download/upload is finished
//                    Log.v("speedtest", "[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
//                    Log.v("speedtest", "[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());

                    downloadSpeed = report.getTransferRateBit().toString() + " bps";
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    // called when a download/upload error occur
                }

                @Override
                public void onProgress(float percent, SpeedTestReport report) {

                    downloadSpeed = report.getTransferRateBit().toString() + " bps";

                    // called to notify download/upload progress
//                    Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
//                    Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
//                    Log.v("speedtest", "[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
                }
            });

            speedTestSocket.startDownload("http://ipv4.ikoula.testdebit.info/1M.iso");

            return null;
        }
    }

    public class SpeedUploadTestTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            SpeedTestSocket speedTestSocket = new SpeedTestSocket();

            // add a listener to wait for speedtest completion and progress
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                @Override
                public void onCompletion(SpeedTestReport report) {
                    // called when download/upload is finished
//                    Log.v("speedtest", "[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
//                    Log.v("speedtest", "[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());

                    uploadSpeed = report.getTransferRateBit().toString() + " bps";
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    // called when a download/upload error occur
                }

                @Override
                public void onProgress(float percent, SpeedTestReport report) {
                    // called to notify download/upload progress
                    uploadSpeed = report.getTransferRateBit().toString() + " bps";

//                    Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
//                    Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
//                    Log.v("speedtest", "[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
                }
            });

            speedTestSocket.startUpload("http://ipv4.ikoula.testdebit.info/", 1000000);

            return null;
        }
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }
    }

}
