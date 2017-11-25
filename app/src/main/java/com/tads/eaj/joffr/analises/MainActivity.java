package com.tads.eaj.joffr.analises;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.MainThread;
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
import android.widget.QuickContactBadge;
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

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    GraphView grafi;

    DataPoint[] pontos = new DataPoint[]{new DataPoint(0, 0)};
    DataPoint[] auxpontos;
    LineGraphSeries<DataPoint> series;

    //Declaração para as mensagens de depuração
    private static final String TAG = MainActivity.class.getSimpleName();

    //Declarações e inicializações para o MQTT
    private String clientId = MqttClient.generateClientId();
    private MqttAndroidClient client;
    private boolean assinou = false;

    //Declarações e inicializações para as notificações
    private NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "MqttAlert");
    private TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    private NotificationManager mNotificationManager;
    private int cont = 0;

    int count = 1;

    private MqttCallback ClientCallBack = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            //chamado sempre que a conexão com broker for perdida
            Log.d("batata", "peda da conexção...Reconectando");
            connectMQTT();
            assinou = false;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            //indica a chegada de uma menssagem de algum topico inscrito
            String msg = new String(message.getPayload());
            Log.d(TAG, topic + ": " + msg);
            if (topic.equals("/Temperatura")) {//APRESENTADA GRAFICAMENTE
                int temp = Integer.parseInt(msg);
                count++;
                //aux pega os valores guardados para recriar o velho com um novo tamanho
                auxpontos = new DataPoint[pontos.length];
                auxpontos = pontos.clone();
                pontos = new DataPoint[auxpontos.length + 1];
                for (int i = 0; i < auxpontos.length; i++) {
                    pontos[i] = auxpontos[i];
                }
                pontos[pontos.length - 1] = new DataPoint(count, temp);
                series.resetData(pontos);
            } else {
                Log.d("batata", "NOTHING HAPPEN");
                //mensagem publicada
                int mId = 99;
                mBuilder.setSmallIcon(R.drawable.ic_menu_send)
                        .setContentTitle("Mensagem recebida")
                        .setContentText(topic + ": " + message.toString())
                        .setVibrate(new long[]{150, 300, 150, 600}) // Para Vibrar
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                // Para apitar
                // Executa a notificação
                mNotificationManager.notify(mId, mBuilder.build());
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "Entregue!");
        }
    };

    private IMqttActionListener MqttCallBackApp = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            //Acionado quando a conexão com o server for estabelecida, e as assinaturas de topicos forem efetivadas
            Log.d(TAG, "on Sucess");
            if (!assinou) {
                subscribeMQTT();
                assinou = true;
            }
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            //problemas na assinatura
            Log.d("batata", "onFailure");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("batata", "onCreate");
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
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initMQTT();
        connectMQTT();
        startNotifications();
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

    //inicialização do cliente MQTT
    private void initMQTT() {
        client = new MqttAndroidClient(this.getApplicationContext(), "192.168.0.16", clientId);
        client.setCallback(ClientCallBack);
    }

    //inciialização do MQTT e conexão inicial
    private void connectMQTT() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName("teste");
            options.setPassword("teste".toCharArray());
            IMqttToken token = client.connect(options);
            token.setActionCallback(MqttCallBackApp);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Cria as classes necessárias para notificações: Intent, PendingIntent e acessa o mNotificationManager
    private void startNotifications(){
        Intent resultIntent = new Intent(this, MainActivity.class);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    }

    //Assina as menssagens MQTT desejadas
    public void subscribeMQTT() {
        int qos = 1;
        try {
            if (!client.isConnected()) {
                connectMQTT();
            }
            IMqttToken subTokenU = client.subscribe("/Temperatura", qos);
            subTokenU.setActionCallback(MqttCallBackApp);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //TODO:FAZER DEPOIS
    //Trata o clique do botão, publicando as mensagens
    public void onClickPub(View view) {
    }//Evento disparado no vlique botao
}
