package com.tads.eaj.joffr.analises;


import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    WifiManager wifi;

    static String MQTTHOST = "tcp://192.168.0.16:1883";
    static String USERNAME = "teste";
    static String SENHA = "teste";
    String topico = "Temperatura";
    boolean conectado = false;
    int count = 0;

    MqttAndroidClient client;
    MqttConnectOptions options;

    Vibrator vibrator;

    GraphView grafi;
    TextView tv;
    View tela;

    //DataPoint[] pontos = new DataPoint[]{new DataPoint(0, 0)};
    LineGraphSeries<DataPoint> series;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("batata", "onCreate");
        tv = (TextView) findViewById(R.id.tv);
        tela = findViewById(R.id.tela);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        grafi = (GraphView) findViewById(R.id.graf);

        //series = new LineGraphSeries<>(pontos);
        series = new LineGraphSeries<>();
        series.setDrawBackground(true);
        series.setDrawDataPoints(true);
        grafi.addSeries(series);

        grafi.getViewport().setScrollable(true);
        // set manual X bounds
        grafi.getViewport().setXAxisBoundsManual(true);
        grafi.getViewport().setMinX(1);
        grafi.getViewport().setMaxX(10);

        // set manual Y bounds
        grafi.getViewport().setYAxisBoundsManual(true);
        grafi.getViewport().setMinY(24);
        grafi.getViewport().setMaxY(31);

        //listener do ponto
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(MainActivity.this, "Você clicou no ponto: " + dataPoint, Toast.LENGTH_SHORT).show();
            }
        });

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Pub();
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

        //TESTA SE O DISPOSITIVO ESTA CONECTADO AO WIFI, SE NAO ESTIVER ELE NAO TENTARA CONECTAR AO BROKER
        if (wifi.isWifiEnabled()) {
//            Toast.makeText(this, R.string.wifiConect, Toast.LENGTH_SHORT).show();
            ConectaMQTT();
        } else {
//            Toast.makeText(this, "o wifi ta desligado", Toast.LENGTH_SHORT).show();
            Snackbar.make(tela, R.string.wifidesc, Snackbar.LENGTH_SHORT).show();
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Snackbar.make(tela, "Conexão com o Broker perdida", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String temp = new String(message.getPayload());
                float y = Float.valueOf(temp);//Integer.parseInt(temp);
                tv.setText(temp);
                count++;
                series.appendData(new DataPoint(count, y), true, 40);
                if (y < 26) {
                    series.setColor(Color.rgb(0, 188, 212));
                    series.setBackgroundColor(Color.argb(50, 79, 195, 247));
                    setTheme(R.style.TemaAzul);
                } else if (y > 31) {
                    series.setColor(Color.rgb(244, 67, 54));
                    series.setBackgroundColor(Color.argb(50, 229, 115, 115));
                    setTheme(R.style.TemaVermelho);
                } else {
                    series.setColor(Color.rgb(76, 175, 80));
                    series.setBackgroundColor(Color.argb(50, 129, 199, 132));
                }
                //vibrator.vibrate(500);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

    }

    private void ConectaMQTT() {
        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //conectou
//                    Log.d("teste", "onSucess");
//                    Toast.makeText(MainActivity.this, "Conectou", Toast.LENGTH_SHORT).show();
                    Snackbar.make(tela, R.string.BrokerConect, Snackbar.LENGTH_SHORT).show();

                    setSubcription();
                    conectado = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //algo deu errado
//                    Log.d("teste", "onFailure");
//                    Log.d("teste", exception.toString());
//                    Toast.makeText(MainActivity.this, "Não Conectou", Toast.LENGTH_SHORT).show();
                    Snackbar.make(tela, R.string.BrokerErr, Snackbar.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void Pub() {
        String topic = "teste";
        String message = "SCORT THE PAYLOAD!";

        try {
            client.publish(topic, message.getBytes(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSubcription() {
        try {
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
        if (id == R.id.action_Reconect) {
            //AQUI TENTA RECONECTAR, CASO ESTEJA DESCONECTADO
            if (!conectado) {
                if (wifi.isWifiEnabled()) {
                    ConectaMQTT();
                } else {
                    Snackbar.make(tela, "O wifi está desligado", Snackbar.LENGTH_SHORT).show();
                    return true;
                }
            } else {
                Snackbar.make(tela, "Você já esta conectado", Snackbar.LENGTH_SHORT).show();
            }

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
