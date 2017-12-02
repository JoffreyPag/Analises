package com.tads.eaj.joffr.analises;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
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

    static String MQTTHOST = "tcp://192.168.50.1:1883";
    static String USERNAME = "JoffrMQTT";
    static String SENHA = "mosquito";
    String topico = "Temperatura", topicoU = "Umidade"; //topicos usados nessa aplicação
    boolean conectado = false; //flag para conexao com o broker
    int xT = 0, xU = 0;

    MqttAndroidClient client;
    MqttConnectOptions options;

//    Vibrator vibrator;

    GraphView grafi, grafi2;
    TextView tv, tvh;
    View tela;

    LineGraphSeries<DataPoint> series, series2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.tvt);
        tvh = findViewById(R.id.tvH);
        tela = findViewById(R.id.tela);

        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

//        Vibrator vibrator
//        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        SetGraficos();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //cria o cliente mqtt e tenta conectar
        CriaClienteMQTT();

    }

//====================== METODOS QUE TRABALHAM O MQTT ==============================
    private void CriaClienteMQTT() {
        //TRABALHANDO O MQTT CLIENT

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, clientId);

        options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(SENHA.toCharArray());

        //TESTA SE O DISPOSITIVO ESTA CONECTADO AO WIFI, SE NAO ESTIVER ELE NAO TENTARA CONECTAR AO BROKER
        if (wifi.isWifiEnabled()) {
            ConectaMQTT();
        } else {
            Snackbar.make(tela, R.string.wifidesc, Snackbar.LENGTH_SHORT).show();
        }

        client.setCallback(new MqttCallback() {
            //aqui trata eventos do mqtt (perda de conexao, chegada de mensagens e envio de mensagens
            @Override
            public void connectionLost(Throwable cause) {
                //caso a conexao caia
                Snackbar.make(tela, "Conexão com o Broker perdida", Snackbar.LENGTH_SHORT).show();
                conectado = false;
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //executa sempre que uma nova mensagem chega do broker que esta conectado
                String temp;
                if (topic.equals(topico)) {
                    temp = new String(message.getPayload());
                    float y = Float.valueOf(temp);//Integer.parseInt(temp);
                    tv.setText(temp);
                    xT++;
                    //para cada novo dado sera jogado no eixo y e o contador x so estara incrementado
                    //para cada nova menssagem
                    series.appendData(new DataPoint(xT, y), true, 40);
                    //apenas uma brincadeiras com cores nos graficos
                    if (y < 26) {
                        series.setColor(Color.rgb(0, 188, 212));
                        series.setBackgroundColor(Color.argb(50, 79, 195, 247));
                    } else if (y > 31) {
                        series.setColor(Color.rgb(244, 67, 54));
                        series.setBackgroundColor(Color.argb(50, 229, 115, 115));
                    } else {
                        series.setColor(Color.rgb(76, 175, 80));
                        series.setBackgroundColor(Color.argb(50, 129, 199, 132));
                    }
                } else if (topic.equals(topicoU)) {
                    temp = new String(message.getPayload());
                    float y = Float.valueOf(temp);
                    tvh.setText(temp);
                    xU++;
                    //aqui tem a mesma situação do de cima porem eh para um topico diferente
                    series2.appendData(new DataPoint(xU, y), true, 40);
                    if (y > 80) {
                        series2.setColor(Color.rgb(0, 188, 212));
                        series2.setBackgroundColor(Color.argb(50, 79, 195, 247));
                    } else if (y < 30) {
                        series2.setColor(Color.rgb(255, 193, 7));
                        series2.setColor(Color.argb(50, 255, 213, 79));
                    } else {
                        series2.setColor(Color.rgb(76, 175, 80));
                        series2.setBackgroundColor(Color.argb(50, 129, 199, 132));
                    }
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

    //se algum dia eu quiser publicar algo....
   /* public void Pub() {
        String topic = "teste";
        String message = "SCORT THE PAYLOAD!";

        try {
            client.publish(topic, message.getBytes(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }*/

    private void setSubcription() {
        try {
            client.subscribe(topico, 0);
            client.subscribe(topicoU, 0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

//===============================================================================

    public void SetGraficos() {
        //GRAFICO 1
        grafi = (GraphView) findViewById(R.id.graf);
        grafi2 = (GraphView) findViewById(R.id.graf2);
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
        //GRAFICO 2
        series2 = new LineGraphSeries<>();
        series2.setDrawBackground(true);
        series2.setDrawDataPoints(true);
        grafi2.addSeries(series2);
        grafi2.getViewport().setScrollable(true);
        // set manual X bounds
        grafi2.getViewport().setXAxisBoundsManual(true);
        grafi2.getViewport().setMinX(10);
        grafi2.getViewport().setMaxX(50);
        // set manual Y bounds
        grafi2.getViewport().setYAxisBoundsManual(true);
        grafi2.getViewport().setMinY(40);
        grafi2.getViewport().setMaxY(90);
        series2.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(MainActivity.this, "Você clicou no ponto: " + dataPoint, Toast.LENGTH_SHORT).show();
            }
        });
    }

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
        }else if (id == R.id.action_reset){
            Reset();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        /*if (id == R.id.nav_camera) {
            // Handle the camera action
        } else*/
        if (id == R.id.nav_info) {
            startActivity(new Intent(this, Sobre.class));

        } else if (id == R.id.nav_config) {
            startActivityForResult(new Intent(this, Configuracao.class), 11);

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 11) {
                Bundle b = data.getExtras();
                MQTTHOST = b.getString("newHost");
                USERNAME = b.getString("user");
                SENHA = b.getString("senha");

                //ja pega a nova configuração e ja tenta conectar
                CriaClienteMQTT();

                Snackbar.make(tela, "Configurações implementas com sucesso", Snackbar.LENGTH_SHORT).show();
            }
        } else if (resultCode == RESULT_CANCELED) {
            Snackbar.make(tela, "Cancelado", Snackbar.LENGTH_SHORT).show();
        }
    }

    public void Reset() {
        MQTTHOST = "tcp://192.168.50.1:1883";
        USERNAME = "JoffrMQTT";
        SENHA = "mosquito";
        Snackbar.make(tela, "Configurações alteradas para o de instalação",
                Snackbar.LENGTH_SHORT).show();
    }
}
