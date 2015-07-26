package com.jbeleno.centinela;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationServices;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Reportes extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String PROPERTY_REG_ID = "push_id";
    public static final String PROPERTY_ID = "id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_LATITUD = "latitud";
    private static final String PROPERTY_LONGITUD = "longitud";
    private static final String PROPERTY_PLATAFORMA = "plataforma";
    private static final String PROPERTY_MODELO = "modelo";
    private static final String PROPERTY_UUID = "uuid";
    private static final String PROPERTY_VERSION = "version";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    String SENDER_ID = "299878614688";

    static final String TAG = "Centinela-Reportes";

    GoogleCloudMessaging gcm;
    Context context;

    String regid;

    private ArrayList<JSONObject> reportes;
    private RequestQueue queue;
    private String modelo, plataforma, identificador, version, latitude, longitude;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private String URL_FEED, URL_REGISTRO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        //Se obtienen los datos del botón
        ImageButton btnReportar = (ImageButton) findViewById(R.id.btn_irRepotar);
        btnReportar.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(v.getContext(), Reportar.class);
                        intent.putExtra("Latitud", latitude);
                        intent.putExtra("Longitud", longitude);
                        startActivity(intent);
                    }
                }
        );

        //Se declara el contexto de la aplicación
        context = getApplicationContext();

        //Se inicializan los datos del equipo
        obtenerDatosDispositivo();

        //Se declara de manera general un objeto para hacer las consultas GET/POST
        NetSingleton myVolley = NetSingleton.getInstance(context);
        queue = myVolley.getRequestQueue();

        //Se declara las URLs
        URL_FEED = "http://54.174.132.246/centinela/publicaciones/ver";
        URL_REGISTRO = "http://54.174.132.246/centinela/usuarios/nuevo";

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        buildGoogleApiClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check device for Play Services APK.
        checkPlayServices();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences();
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.putString(PROPERTY_LATITUD, latitude);
        editor.putString(PROPERTY_LONGITUD, longitude);
        editor.putString(PROPERTY_PLATAFORMA, plataforma);
        editor.putString(PROPERTY_MODELO, modelo);
        editor.putString(PROPERTY_UUID, identificador);
        editor.putString(PROPERTY_VERSION, version);
        editor.apply();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences();
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.i(TAG, msg);
            }
        }.execute(null, null, null);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences() {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences("Usuario",
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        // Your implementation here.
        Map<String, String> parametros = new HashMap<>();
        parametros.put(PROPERTY_REG_ID, regid);
        parametros.put(PROPERTY_LATITUD, latitude);
        parametros.put(PROPERTY_LONGITUD, longitude);
        parametros.put(PROPERTY_PLATAFORMA, plataforma);
        parametros.put(PROPERTY_MODELO, modelo);
        parametros.put(PROPERTY_UUID, identificador);
        parametros.put(PROPERTY_VERSION, version);

        CustomRequest myReq = new CustomRequest(Request.Method.POST,
                URL_REGISTRO,
                parametros,
                SuccessRegistro(),
                ErrorRegistro());
        queue.add(myReq);
    }

    //Se crea una varible que inicializa la API de Google Services para usar la Geolocalización
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    //Se hallan los datos del dispositivo
    private void obtenerDatosDispositivo(){
        modelo = Build.MANUFACTURER+"-"+Build.MODEL;
        version = Build.VERSION.RELEASE;
        identificador = !Build.SERIAL.equals(Build.UNKNOWN) ? Build.SERIAL : Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        plataforma = "Android";
    }

    //Se llena el array con los datos que se extraen del servidor
    private void llenarLista(){
        //Inicializar el arrayList de reportes
        reportes = new ArrayList<>();

        Map<String, String> parametros = new HashMap<>();
        parametros.put("latitud",latitude);
        parametros.put("longitud", longitude);

        CustomRequest myReq = new CustomRequest(Request.Method.POST,
                URL_FEED,
                parametros,
                SuccessReport(),
                ErrorReport());
        queue.add(myReq);
    }

    //Se almacenan los datos registro del servidor en el array
    private Response.Listener<JSONObject> SuccessRegistro() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String Status = response.getString("Status");
                    if(Status.equals("OK")) {
                        final SharedPreferences prefs = getGcmPreferences();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(PROPERTY_ID, response.getString("ID"));
                        editor.apply();
                        Log.d(TAG, response.getString("ID"));
                    }else{
                        String Mensaje = response.getString("Mensaje");
                        Toast.makeText(getApplicationContext(), Mensaje, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), R.string.errorJSON, Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    //Se manejan los errores en la consulta regsitro al servidor
    private Response.ErrorListener ErrorRegistro() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
            Log.d(TAG, error.getMessage());
            }
        };
    }


    //Se almacenan los datos FEED del servidor en el array
    private Response.Listener<JSONObject> SuccessReport() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String Status = response.getString("Status");
                    if(Status.equals("OK")) {
                        JSONArray Datos = response.getJSONArray("Datos");
                        for (int i = 0; i < Datos.length(); i++) {
                            //Log.d("Debug-Centinela:", Datos.getJSONObject(i).toString());
                            reportes.add(Datos.getJSONObject(i));
                        }
                        llenarVista();
                    }else{
                        String Mensaje = response.getString("Mensaje");
                        Toast.makeText(getApplicationContext(), Mensaje, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), R.string.errorJSON, Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    //Se manejan los errores en la consulta FEED al servidor
    private Response.ErrorListener ErrorReport() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                Log.d(TAG, error.getMessage());
            }
        };
    }

    //Se dibuja la vista con los datos del array
    private void llenarVista(){
        MiAdaptador adaptador = new MiAdaptador();
        ListView lista = (ListView)findViewById(R.id.lista);
        adaptador.notifyDataSetChanged();
        lista.setAdapter(adaptador);
    }

    //Se recibe la latitud y longitud
    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            latitude = String.valueOf(mLastLocation.getLatitude());
            longitude = String.valueOf(mLastLocation.getLongitude());
            Log.d("Success: ", latitude+", "+longitude);
            llenarLista();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(),R.string.errorGPS ,Toast.LENGTH_LONG).show();
    }

    private class MiAdaptador extends ArrayAdapter{
        public MiAdaptador(){
            super(Reportes.this, R.layout.item_reporte, reportes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View item = convertView;
            //Asegurarnos que la vista existe
            if(item == null){
                item = getLayoutInflater().inflate(R.layout.item_reporte, parent, false);
            }


            //Se recuperan los datos del array
            String mensaje, distancia, tiempo, tipo, color;
            double latitud, longitud;
            try {
                tipo = reportes.get(position).getString("tipo");
                mensaje = reportes.get(position).getString("mensaje");
                distancia = reportes.get(position).getString("direccion");
                tiempo = reportes.get(position).getString("fecha");
                latitud = Double.parseDouble(reportes.get(position).getString("latitud"));
                longitud = Double.parseDouble(reportes.get(position).getString("longitud"));
            }catch (JSONException e){
                tipo = "Otro";
                mensaje = "Ups!, it seems there's a problem in our server, please try reload the view later.";
                distancia = "Here";
                tiempo = "1 minute";
                latitud = 7.77;
                longitud = -73.11;
            }

            //Se asigna el color del marker
            switch(tipo){
                case "Hurto":
                    color = "f44336";
                    break;
                case "Accidente":
                    color = "795548";
                    break;
                case "Homicidio":
                    color = "000000";
                    break;
                case "Suicidio":
                    color = "9c27b8";
                    break;
                case "Sospecha":
                    color = "2962ff";
                    break;
                case "Riña":
                    color = "4caf50";
                    break;
                default:
                    color = "bdbdbd";
                    break;
            }

            //Se llena cada item con sus datos
            TextView msg = (TextView) item.findViewById(R.id.item_mensaje);
            TextView tiempotxt = (TextView) item.findViewById(R.id.item_tiempo);
            TextView distanciatxt = (TextView) item.findViewById(R.id.item_distancia);
            msg.setText(mensaje);
            tiempotxt.setText(tiempo);
            distanciatxt.setText(distancia);

            //Se configuran los datos del mapa
            MapView mapa = (MapView) item.findViewById(R.id.item_mapa);
            mapa.setCenter(new LatLng(latitud, longitud));
            mapa.setZoom(17);

            //Se agrega un marcador donde sucedio el evento(crimen o sospecha)
            Marker evento = new Marker(mapa, mensaje, distancia+", "+tiempo, new LatLng(latitud, longitud));
            evento.setIcon(new Icon(item.getContext(), Icon.Size.MEDIUM, "", color));
            mapa.addMarker(evento);

            //Se llama al contenedor y se establece un tag para saber a que item pertenece
            LinearLayout contenedor = (LinearLayout) item.findViewById(R.id.item_content);
            contenedor.setTag(position);


            //Se establece un onClickListener para cuando se de click se va a otra vista con los detalles
            contenedor.setOnClickListener(new View.OnClickListener() {
                public void onClick(final View v) {

                }
            });

            return item;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_reportes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), Configurar.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
