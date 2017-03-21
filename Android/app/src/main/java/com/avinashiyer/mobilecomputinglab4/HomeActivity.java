package com.avinashiyer.mobilecomputinglab4;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements BeaconConsumer,RangeNotifier {
    private static final String TAG = "HomeActivity";
    private static final String url = "http://192.168.0.23:8080/";
    String[] uArray = {"0x02676f6f676c6507","0x027961686f6f07","0x0266616365626f6f6b07"};
    List<String> uuidList = new ArrayList(Arrays.asList(uArray));
    private BeaconManager mBeaconManager;
    private ProgressDialog progressDialog=null;
    @Override
    public void onResume() {
        super.onResume();
        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect the main Eddystone-UID frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-21v"));
        mBeaconManager.bind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.setRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        //Toast.makeText(getApplicationContext(),"insidebvbvbvb",Toast.LENGTH_SHORT).show();
        for (Beacon beacon: beacons) {

            String url1 = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
            Log.d("UUID","UUID IS: "+beacon.getId1());
            //Toast.makeText(HomeActivity.this,url1+" ",Toast.LENGTH_SHORT).show();
            //uuidList.contains(beacon.getId1().toString()) &&
            if(uuidList.contains(beacon.getId1().toString()) && beacon.getBeaconTypeCode() == 0x10){
                int beaconIndex = uuidList.indexOf(beacon.getId1().toString()) + 1;
                Toast.makeText(getApplicationContext(),"My beacon:  "+beacon.getServiceUuid()+" ",Toast.LENGTH_SHORT).show();
                String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                Log.d("BHAUKAAL", "I see a beacon transmitting a url: " + url +
                        " approximately " + beacon.getDistance() + " meters away.");
                if(progressDialog!=null){
                    progressDialog.dismiss();
                    progressDialog=null;
                }

                sendData(beaconIndex,beacon.getId1().toString());
//                Intent i = new Intent(HomeActivity.this,HomeActivity.class);
//                startActivity(i);
//                finish();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mBeaconManager.unbind(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        progressDialog = new ProgressDialog(HomeActivity.this);
        progressDialog.setIndeterminate(false);
        progressDialog.setMessage("Searching for beacons...");
        progressDialog.show();
    }

    private void sendStringData(final int id, final String beaconId){

        //192.168.0.23:8080/?beaconIndex=2&
        String uri = url+"a?beaconIndex="+String.valueOf(id)+"&beaconId="+beaconId;
        StringRequest postRequest = new StringRequest(Request.Method.POST, uri,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error+"");
                    }
                }
        );
        MyApplication.getInstance().addToRequestQueue(postRequest,TAG);
    }
    private void sendData(final int id, final String beaconId){
        final JSONObject jsonBodyObj = new JSONObject();
        try{
            jsonBodyObj.put("beaconIndex", String.valueOf(id));
            jsonBodyObj.put("beaconId", beaconId);

        }catch (JSONException e){
            e.printStackTrace();
        }
        //final String requestBody = jsonBodyObj.toString();
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());

            }
        }) {
            @Override
            public byte[] getBody() {
                try {
                    return jsonBodyObj == null ? null : jsonBodyObj.toString().getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                            jsonBodyObj, "utf-8");
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }

//            @Override
//            protected Map<String, String> getParams() {
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("beaconIndex", String.valueOf(id));
//                params.put("beaconId", beaconId);
//                //params.put("password", "password123");
//
//                return params;
//            }

        };

// Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(jsonObjReq, TAG);
    }

}
