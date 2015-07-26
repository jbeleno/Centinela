package com.jbeleno.centinela;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class Configurar extends ActionBarActivity {
    private String rango, notificaciones;
    private String URL_NEW_SETTINGS, URL_SETTINGS;
    private RequestQueue queue;
    private NetSingleton MyVolley;
    private SeekBar sk;
    private Switch notification;
    private TextView Rangotxt;

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurar);

        //Se inicializan los datos de las URLs
        URL_SETTINGS = "http://54.174.132.246/centinela/usuarios/ver_settings";
        URL_NEW_SETTINGS = "http://54.174.132.246/centinela/usuarios/actualizar_settings";

        //Se declara el contexto de la aplicación
        context = getApplicationContext();

        //Se declara de manera general un objeto para hacer las consultas GET/POST
        MyVolley = MyVolley.getInstance(this.getApplicationContext());
        queue = MyVolley.getRequestQueue();



        initSeekBar();
        initButton();
        cargarSettings();
    }

    private void cargarSettings(){
        //Se recupera el id del usuario
        final SharedPreferences prefs = getGcmPreferences();
        String Id = prefs.getString("id", "");

        //Se agregan datos como parametros
        Map<String, String> parametros = new HashMap<>();
        parametros.put("usuario",Id);

        CustomRequest myReq = new CustomRequest(Request.Method.POST,
                URL_SETTINGS,
                parametros,
                SuccessSettings(),
                ErrorSettings());
        queue.add(myReq);
    }

    private void initSeekBar(){
        sk =(SeekBar) findViewById(R.id.rangoBar);
        Rangotxt = (TextView) findViewById(R.id.rango);
        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                // TODO Auto-generated method stub
                Rangotxt.setText(context.getString(R.string.rango)+Float.toString(progress)+" Km");
                rango = Float.toString(progress);
            }
        });
    }

    private void initButton(){
        Button btnSettings = (Button) findViewById(R.id.btnConfigurar);
        notification = (Switch) findViewById(R.id.notificacion);
        btnSettings.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Se obtiene el valor de notificaciones
                notificaciones = (notification.isChecked()) ? "SI":"NO";

                //Se recupera el id del usuario
                final SharedPreferences prefs = getGcmPreferences();
                String Id = prefs.getString("id", "");

                //Se agregan datos como parametros
                Map<String, String> parametros = new HashMap<>();
                parametros.put("usuario",Id);
                parametros.put("notificaciones",notificaciones);
                parametros.put("rango", rango);

                Log.i("Centinela", parametros.toString());
                CustomRequest myReq = new CustomRequest(Request.Method.POST,
                        URL_NEW_SETTINGS,
                        parametros,
                        SuccessNewSettings(),
                        ErrorSettings());
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

    //Se guarda en el servidor las configuraciones registradas
    private Response.Listener<JSONObject> SuccessNewSettings() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String Status = response.getString("Status");
                    if(Status.equals("OK")) {
                        Toast.makeText(getApplicationContext(), R.string.goodSettings, Toast.LENGTH_LONG).show();
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

    //Se muestran las configuraciones del usuario
    private Response.Listener<JSONObject> SuccessSettings() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String Status = response.getString("Status");
                    if(Status.equals("OK")) {
                        JSONObject settings = response.getJSONObject("Settings");
                        String range = settings.getString("rango");
                        String texto = context.getString(R.string.rango)+range+" Km";
                        Log.i("Rango", texto);
                        Rangotxt.setText(texto);
                        String push = settings.getString("notificaciones");
                        if(push.equals("SI")){
                            notification.setChecked(true);
                        }else{
                            notification.setChecked(false);
                        }
                        sk.setProgress((int)Float.parseFloat(range));
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
    private Response.ErrorListener ErrorSettings() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_configurar, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
