package com.tads.eaj.joffr.analises;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class Configuracao extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracao);

    }

    public void Salvar(View view){
        setResult(RESULT_CANCELED);
        finish();
    }
}
