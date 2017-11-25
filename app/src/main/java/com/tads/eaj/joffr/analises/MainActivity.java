package com.tads.eaj.joffr.analises;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    static String MQTTHOST = "tcp://192.168.0.16:1883";
    static String USERNAME = "teste";
    static String SENHA = "teste";
    String topico = "Temperatura";
    boolean conectado = false;

    MqttAndroidClient client;
    MqttConnectOptions options;

    GraphView grafi;
    Button bot;
    TextView tv;

    DataPoint[] pontos = new DataPoint[]{new DataPoint(0, 0)};
    LineGraphSeries<DataPoint> series;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("batata", "onCreate");
        bot = (Button) findViewById(R.id.button);
        tv = (TextView) findViewById(R.id.tv);
        grafi = (GraphView) findViewById(R.id.graf);

        series = new LineGraphSeries<>(pontos);
        grafi.addSeries(series);

        grafi.getViewport().setScalableY(true);
        // set manual X bounds
        grafi.getViewport().setXAxisBoundsManual(true);
        grafi.getViewport().setMinX(1);
        grafi.getViewport().setMaxX(5);

        // set manual Y bounds
        grafi.getViewport().setYAxisBoundsManual(true);
        grafi.getViewport().setMinY(0);
        grafi.getViewport().setMaxY(5);

        //listener do ponto
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(MainActivity.this, "Você clicou no ponto: " + dataPoint, Toast.LENGTH_SHORT).show();
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //AQUI TENTA RECONECTAR, CASO ESTEJA DESCONECTADO
                if (!conectado){
                    ConectaMQTT();
                }else{
                    Snackbar.make(view, "Você já esta conectado", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (conectado){
                    Snackbar.make(view, "Reconectou", Snackbar.LENGTH_SHORT).show();
                }else{
                    Snackbar.make(view, "Erro ao Reconectar", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        //TRABALHANDO O MQTT CLIENT
        //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, clientId);

        options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(SENHA.toCharArray());

//        try {
//            IMqttToken token = client.connect(options);
//            token.setActionCallback(new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    //conectou
//                    Log.d("teste", "onSucess");
//                    Toast.makeText(MainActivity.this, "Conectou", Toast.LENGTH_SHORT).show();
//                    setSubcription();
//                    conectado = true;
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    //algo deu errado
//                    Log.d("teste", "onFailure");
//                    Log.d("teste", exception.toString());
//                    Toast.makeText(MainActivity.this, "Não Conectou", Toast.LENGTH_SHORT).show();
//                }
//            });
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }

        ConectaMQTT();

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                tv.setText(new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

    }

    private void ConectaMQTT(){
        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //conectou
                    Log.d("teste", "onSucess");
                    Toast.makeText(MainActivity.this, "Conectou", Toast.LENGTH_SHORT).show();
                    setSubcription();
                    conectado = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //algo deu errado
                    Log.d("teste", "onFailure");
                    Log.d("teste", exception.toString());
                    Toast.makeText(MainActivity.this, "Não Conectou", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void Pub(View view){
        String topic = "teste";
        String message = "SCORT THE PAYLOAD!";

        try{
            client.publish(topic, message.getBytes(), 0, false);
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    private void setSubcription(){
        try{
            client.subscribe(topico, 0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
