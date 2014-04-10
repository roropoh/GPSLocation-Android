package com.example.tcptesting.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

/*------------------------------------------------------------------------------------------------------------------
-- SOURCE FILE: MainActivity.java
--
-- PROGRAM: Location Finder
--
-- CLASSES: MainActivity
--              protected void onCreate(Bundle savedInstanceState)
--              public boolean onCreateOptionsMenu(Menu menu)
--              public boolean onOptionsItemSelected(MenuItem item)
--          ServerActivity
--              ServerThread
--              public void onCreate(Bundle savedInstanceState)
--              private void initilizeMap()
--              protected void onStop()
--          ClientActivity
--              ClientThread
--              public void onCreate(Bundle savedInstanceState)
--
--
-- DATE: February 28 2014
--
-- REVISIONS: March 02 2014 - added port option to MainActivity
--
-- DESIGNER:   Robin Hsieh & Damien Sathanielle
--
-- PROGRAMMER: Robin Hsieh & Damien Sathanielle
--
-- NOTES: Main activity where the user can select to act as a client or server. Here the user will
--        specify a port to communicate on.
----------------------------------------------------------------------------------------------------------------------*/
public class MainActivity extends Activity {

    Button clientButton;
    Button serverButton;
    EditText portNumberMainActivity;
    int     serverPortMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverButton = (Button) findViewById(R.id.bServer);
        clientButton = (Button) findViewById(R.id.bClient);

        serverButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                portNumberMainActivity = (EditText) findViewById(R.id.portNumActivityMain);

                if (portNumberMainActivity.getText().toString().matches("")) {
                    Toast.makeText(MainActivity.this, "Please enter a port number for server", Toast.LENGTH_LONG).show();
                } else {
                    Log.d("Port Number inside MainActivity", portNumberMainActivity.getText().toString());

                    serverPortMainActivity = Integer.parseInt(portNumberMainActivity.getText().toString());

                    Intent myIntent = new Intent(MainActivity.this, ServerActivity.class);
                    myIntent.putExtra("intPortNumber", serverPortMainActivity);
                    startActivity(myIntent);
                }
            }
        });

        clientButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent myIntent = new Intent(MainActivity.this, ClientActivity.class);
                startActivity(myIntent);
            }
        });

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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
