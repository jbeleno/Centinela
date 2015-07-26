package com.jbeleno.centinela;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Juan Sebastian on 31/03/2015.
 */
public class Reportar extends ActionBarActivity{
    private double longitud, latitud, latitude, longitude;
    private String URL_NUEVO_EVENTO;
    private RequestQueue queue;
    private NetSingleton MyVolley;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportar);
        Intent intent = getIntent();
        latitud = Double.parseDouble(intent.getStringExtra("Latitud"));
        longitud = Double.parseDouble(intent.getStringExtra("Longitud"));

        //Se declara de manera general un objeto para hacer las consultas GET/POST
        MyVolley = MyVolley.getInstance(this.getApplicationContext());
        queue = MyVolley.getRequestQueue();

        //Se declara el contexto de la aplicación
        context = getApplicationContext();

        URL_NUEVO_EVENTO = "http://54.174.132.246/centinela/publicaciones/nuevo";
        initSpinner();
        initMapa();
        initButton();
    }

    private void initButton(){
        Button btnEvento = (Button) findViewById(R.id.btnReportar);
        btnEvento.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Se inicializan los controles desde la interfaz gráfica
                Spinner tipo = (Spinner) findViewById(R.id.report_tipo);
                EditText evento = (EditText) findViewById(R.id.report_texto);

                //Se recupera el id del usuario
                final SharedPreferences prefs = getGcmPreferences();
                String Id = prefs.getString("id", "");

                //Se agregan datos como parametros
                Map<String, String> parametros = new HashMap<>();
                parametros.put("usuario",Id);
                parametros.put("latitud",Double.toString(latitude));
                parametros.put("longitud", Double.toString(longitude));
                parametros.put("tipo", tipo.getSelectedItem().toString());
                parametros.put("evento", evento.getText().toString());

                CustomRequest myReq = new CustomRequest(Request.Method.POST,
                        URL_NUEVO_EVENTO,
                        parametros,
                        SuccessEvento(),
                        ErrorEvento());
                queue.add(myReq);
            }
        });
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

    //Se guarda en el servidor el eveto registrado
    private Response.Listener<JSONObject> SuccessEvento() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String Status = response.getString("Status");
                    if(Status.equals("OK")) {
                        Toast.makeText(getApplicationContext(), R.string.todoBien, Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(), Reportes.class);
                        startActivity(intent);
                    }else{
                        String Mensaje = response.getString("Mensaje");
                        Toast.makeText(getApplicationContext(), Mensaje, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), R.string.errorJSON,
                            Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    //Se manejan los errores en la consulta al servidor
    private Response.ErrorListener ErrorEvento() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
            }
        };
    }

    private void initSpinner(){
        Spinner spinner = (Spinner) findViewById(R.id.report_tipo);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.tipos, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
    }

    private void initMapa(){
        //Se configuran los datos del mapa
        MapView mapa = (MapView) findViewById(R.id.report_mapa);
        mapa.setCenter(new LatLng(latitud, longitud));
        mapa.setZoom(14);
        mapa.setMapViewListener(new MapViewListener() {
            @Override
            public void onShowMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onHideMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onTapMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onLongPressMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onTapMap(MapView mapView, ILatLng iLatLng) {
                //Se limpia el mapa de markers
                mapView.clear();
                //Se agrega un marcador donde sucedio el evento(crimen o sospecha)
                Marker evento = new Marker(mapView, "", "", new LatLng(iLatLng.getLatitude(), iLatLng.getLongitude()));
                evento.setIcon(new Icon(mapView.getContext(), Icon.Size.MEDIUM, "", "2196f3"));
                mapView.addMarker(evento);

                //Se asignan las coordenadas a las variables privadas
                latitude = iLatLng.getLatitude();
                longitude = iLatLng.getLongitude();
            }

            @Override
            public void onLongPressMap(MapView mapView, ILatLng iLatLng) {

            }
        });
    }

    //Se guarda el evento
    private void guardar_evento(){

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_reportar, menu);
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
