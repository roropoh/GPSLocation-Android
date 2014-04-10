package com.example.tcptesting.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.Time;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/*------------------------------------------------------------------------------------------------------------------
-- CLASS: ClientActivity.java
--
-- DATE: February 28 2014
--
-- REVISIONS: March 02 2014 - add username and port option
--
-- DESIGNER: Robin Hsieh & Damien Sathanielle
--
-- PROGRAMMER: Robin Hsieh
--
-- RETURNS: -
--
-- NOTES: This class handles everything on the ClientActivity view. It will allow the user to connect
--        to a server. Sending through TCP, and also getting updates for GPS/Network location.
--
----------------------------------------------------------------------------------------------------------------------*/

public class ClientActivity extends MainActivity {
    Button ConnectButton;
    Button SendCoordinateButton;
    Button DisconnectButton;
    TextView mClientStatus;
    EditText mIPEditText;
    EditText mPortEditText;
    EditText usernameEditText;

    private Socket socket;

    public static double latitude;
    public static double longitude;

    String username;

    LocationManager locationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the view from activity_client.xml
        setContentView(R.layout.activity_client);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        ConnectButton = (Button) findViewById(R.id.bConnect);
        DisconnectButton = (Button) findViewById(R.id.bDisconnect);
        SendCoordinateButton = (Button) findViewById(R.id.bSendCoordinates);
        mClientStatus = (TextView) findViewById(R.id.clientStatusTextView);
        mIPEditText = (EditText) findViewById(R.id.ipText);
        mPortEditText = (EditText) findViewById(R.id.portText);
        usernameEditText = (EditText) findViewById(R.id.nameText);

        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                String possibleEmail = account.name;
                usernameEditText.setText(possibleEmail.substring(0, possibleEmail.indexOf("@")));
            }
        }

        mClientStatus.setMovementMethod(new ScrollingMovementMethod());

        SendCoordinateButton.setEnabled(false);
        DisconnectButton.setEnabled(false);

        ConnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                final String ipString = mIPEditText.getText().toString();
                final String portString = mPortEditText.getText().toString();
                final String usernameString = usernameEditText.getText().toString();

                if (usernameString.matches("")) {
                    Toast.makeText(ClientActivity.this, "Please enter a username", Toast.LENGTH_LONG).show();
                } else if (ipString.matches("")) {
                    Toast.makeText(ClientActivity.this, "Please enter an IP address", Toast.LENGTH_LONG).show();
                } else if (portString.matches("")) {
                    Toast.makeText(ClientActivity.this, "Please enter a port number", Toast.LENGTH_LONG).show();
                } else {
                    username = usernameEditText.getText().toString();
                    Log.d("username", username);
                    new Thread(new ClientThread(ipString, portString)).start();
                }
            }
        });

        DisconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                handlerToButtons.post(disconnectFromServer);
            }
        });


        SendCoordinateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);

                boolean gps_enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean network_enabled = service.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (!gps_enabled && !network_enabled) {
                    Toast.makeText(ClientActivity.this, "Please enable GPS to find your location.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }

                LocationListener mlocListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        location.getLatitude();
                        location.getLongitude();
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {
                    }

                    @Override
                    public void onProviderEnabled(String s) {
                    }

                    @Override
                    public void onProviderDisabled(String s) {
                    }
                };

                // Get the location manager
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mlocListener);

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    if (latitude > 0) {
                        Time now = new Time();
                        now.setToNow();


                        mClientStatus.setText("Sending from " + username + "\n");
                        mClientStatus.append("Time: " + now.format("%Y.%m.%d %H:%M:%S") + "\n");
                        mClientStatus.append("Latitude: " + latitude + "\n");
                        mClientStatus.append("Longitude: " + longitude + "\n");
                        String SendingCoordinates = (latitude + "," + longitude + "," + now.format("%Y.%m.%d %H:%M:%S") + "," + username);
                        Log.e("Currently Sending", SendingCoordinates);
                        try {
                            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter
                                    (socket.getOutputStream())), true);
                            out.println(SendingCoordinates);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        mClientStatus.setText("GPS in progress, please wait\n");
                        Log.d("Location Finding", "GPS in progress, please wait");
                    }
                } else {
                    mClientStatus.setText("GPS is not turned on...");
                }
            }
        });
    }

    class ClientThread implements Runnable {
        String mIp;
        String mPort;

        public ClientThread(String ip, String port) {
            mIp = ip;
            mPort = port;
        }

        @Override
        public void run() {
            try {
                Log.d("IP", mIp);
                Log.d("Port", mPort);

                Time now = new Time();
                now.setToNow();
                Log.d("Format Time", now.format("%Y.%m.%d %H:%M:%S"));

                InetAddress serverAddress = InetAddress.getByName(mIp);
                socket = new Socket(serverAddress, Integer.parseInt(mPort));
                handlerToButtons.post(connectSuccessful);
            } catch (SocketTimeoutException ste) {
                Toast.makeText(ClientActivity.this, "SocketTimeoutException occurred", Toast.LENGTH_LONG).show();
                ste.printStackTrace();
            } catch (UnknownHostException uhe) {
                Toast.makeText(ClientActivity.this, "UnknownHostExcept occurred, can't find server", Toast.LENGTH_LONG).show();
                uhe.printStackTrace();
            } catch (IOException e) {
                Toast.makeText(ClientActivity.this, "Error occurred, cannot connect to server", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

        }
    }

    final Handler handlerToButtons = new Handler();
    final Runnable connectSuccessful = new Runnable() {
        public void run() {
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(usernameEditText.getWindowToken(), 0);
            imm.hideSoftInputFromWindow(mIPEditText.getWindowToken(), 0);
            imm.hideSoftInputFromWindow(mPortEditText.getWindowToken(), 0);
            ConnectButton.setEnabled(false);
            DisconnectButton.setEnabled(true);
            SendCoordinateButton.setEnabled(true);
        }
    };

    final Runnable disconnectFromServer = new Runnable() {
        public void run() {
            ConnectButton.setEnabled(true);
            DisconnectButton.setEnabled(false);
            SendCoordinateButton.setEnabled(false);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
