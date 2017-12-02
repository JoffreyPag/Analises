package com.tads.eaj.joffr.analises;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Configuracao extends AppCompatActivity {

    EditText end1, end2, end3, end4, tPort, tNome, tSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracao);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarconf);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        end1 = findViewById(R.id.end1);
        end2 = findViewById(R.id.end2);
        end3 = findViewById(R.id.end3);
        end4 = findViewById(R.id.end4);
        tPort = findViewById(R.id.textPorta);
        tNome = findViewById(R.id.textNome);
        tSenha = findViewById(R.id.textSenha);

    }

    public void Salvar(View view) {
        String e1 = end1.getText().toString(), e2 = end2.getText().toString(),
                e3 = end3.getText().toString(), e4 = end4.getText().toString();

        if (e1.equalsIgnoreCase("") || e2.equalsIgnoreCase("") ||
                e3.equalsIgnoreCase("") || e4.equalsIgnoreCase("")) {
            Toast.makeText(this, "Há campos vazios, preencha corretamente", Toast.LENGTH_SHORT).show();
        } else {
            //String MQTTHOST = "tcp://192.168.50.1:1883"
            String nwMHOST = "tcp://" + e1 + "." + e2 + "." + e3 + "." + e4 + ":" + tPort.getText();
            Bundle b = new Bundle();
            b.putString("newHost", nwMHOST);
            b.putString("user", tNome.getText().toString());
            b.putString("senha", tSenha.getText().toString());
            Intent i = new Intent();
            i.putExtras(b);
            setResult(RESULT_OK, i);
            finish();
        }
    }


}
