package com.jambit.conti.mirror;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;


public class ConfigActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);

        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfigActivity.this, MainActivity.class);
                EditText editText = (EditText) findViewById(R.id.ip);
                String ip = editText.getText().toString();
                intent.putExtra(MainActivity.IP_EXTRA, ip);
                editText = (EditText) findViewById(R.id.port);
                String port = editText.getText().toString();
                intent.putExtra(MainActivity.PORT_EXTRA, Integer.valueOf(port));
                startActivity(intent);
                finish();
            }
        });
    }
}
