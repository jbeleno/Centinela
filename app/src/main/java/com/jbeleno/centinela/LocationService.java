package com.jbeleno.centinela;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,LocationListener {
    private static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int UPDATE_INTERVAL_IN_SECONDS = 600;
    private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    private static final int FASTEST_INTERVAL_IN_SECONDS = 300;
    private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private String latitud, longitud;
    private RequestQueue queue;

    private static final String URL_UPDATE_LOCATION = "http://54.174.132.246/centinela/usuarios/actualizar";
    static final String TAG = "Centinela-Background";

    public LocationService() {

    }

    //Se crea una varible que inicializa la API de Google Services para usar la Geolocalización
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences() {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences("Usuario", Context.MODE_PRIVATE);
    }

    private void updateLocation(){
        //Se recupera el id del usuario
        final SharedPreferences prefs = getGcmPreferences();
        String Id = prefs.getString("id", "");

        //Se agregan datos como parametros
        Map<String, String> parametros = new HashMap<>();
        parametros.put("usuario",Id);
        parametros.put("latitud",latitud);
        parametros.put("longitud", longitud);

        CustomRequest myReq = new CustomRequest(Request.Method.POST,
                URL_UPDATE_LOCATION,
                parametros,
                SuccessLocation(),
                ErrorLocation());
        queue.add(myReq);
    }

    //Se almacenan los datos de localización del usuario en el servidor
    private Response.Listener<JSONObject> SuccessLocation() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "Latitud: "+latitud+", Longitud: "+longitud);
            }
        };
    }

    //Se manejan los errores en la consulta de actualización de datos
    private Response.ErrorListener ErrorLocation() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error de envio");
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Se inicia");

        buildGoogleApiClient();
        mGoogleApiClient.connect();
        createLocationRequest();

        //Se declara de manera general un objeto para hacer las consultas GET/POST
        NetSingleton myVolley = NetSingleton.getInstance(getApplicationContext());
        queue = myVolley.getRequestQueue();

        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            latitud = String.valueOf(mLastLocation.getLatitude());
            longitud = String.valueOf(mLastLocation.getLongitude());
            startLocationUpdates();
            Log.d(TAG, "Se conecta");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        latitud = String.valueOf(location.getLatitude());
        longitud = String.valueOf(location.getLongitude());
        updateLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Toast.makeText(getApplicationContext(),R.string.errorGPS ,Toast.LENGTH_LONG).show();
    }
}
