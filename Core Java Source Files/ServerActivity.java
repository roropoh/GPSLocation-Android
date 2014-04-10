package com.example.tcptesting.app;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/*------------------------------------------------------------------------------------------------------------------
-- CLASS: ServerActivity
--
-- DATE: February 28 2014
--
-- REVISIONS: March 01 2014 - get and print clients location
--            March 02 2014 - implement google maps
--            March 03 2014 - format and display username and timestamp
--
-- DESIGNER:   Robin Hsieh & Damien Sathanielle
--
-- PROGRAMMER: Damien Sathanielle
--
-- NOTES: This class creates a server that will listen for clients. Once a client connects
--        it will listen for the location data from the client. It will then plot the clients
--        location on google maps and zoom the camera to the most recent location. A timestamp,
--        username, as well as location will be displayed.
----------------------------------------------------------------------------------------------------------------------*/


 public class ServerActivity extends MainActivity {

     GoogleMap googleMap;
     Socket client;

     private TextView serverStatus;
     private TextView serverStatus2;

     String serverip;
     int portNumber;

     private Handler handler = new Handler();

     private ServerSocket serverSocket;
     private static final LatLng VANCOUVER_LOCATION = new LatLng(49.2500, -123.1000);

     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_server);
         serverStatus = (TextView) findViewById(R.id.server_status);
         serverStatus2 = (TextView) findViewById(R.id.server_status2);

         Intent mIntent = getIntent();
         portNumber = mIntent.getIntExtra("intPortNumber", 0);
         Log.d("My port number inside ServerActivity", "" + portNumber);


         WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
         serverip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
         Log.d("serverip after getting", serverip);

         initilizeMap();

         Thread fst = new Thread(new ServerThread());
         fst.start();
     }

     /* initialize google map */
     private void initilizeMap() {
         if (googleMap == null) {
             googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

             /* check if map is created successfully or not */
             if (googleMap == null) {
                 Toast.makeText(getApplicationContext(), "Sorry! unable to create maps",
                         Toast.LENGTH_SHORT).show();
             }

             googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(VANCOUVER_LOCATION, 10));
         }
     }

     public class ServerThread implements Runnable {
         String line = null;

         public void run() {
             try {
                 if (serverip != null) {
                     handler.post(new Runnable() {
                         @Override
                         public void run() {
                             serverStatus.setText("Listening on IP: " + serverip);
                             serverStatus.append("\nPort:" + portNumber);
                         }
                     });

                     serverSocket = new ServerSocket(portNumber);
                     while (true) {
                         /* listen for clients */
                         client = serverSocket.accept();
                         handler.post(new Runnable() {
                             @Override
                             public void run() {
                                 if (client != null) {
                                     serverStatus2.setText("Connected.");
                                 }
                             }
                         });

                         try {
                             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                             while ((line = in.readLine()) != null) {
                                 Log.d("ServerActivity", line);
                                 handler.post(new Runnable() {
                                     @Override
                                     public void run() {
                                         List<String> clientData = Arrays.asList((line.split("\\s*,\\s*")));

                                         Double latitude = Double.parseDouble(clientData.get(0));
                                         Double longitude = Double.parseDouble(clientData.get(1));
                                         String time = clientData.get(2);
                                         String name = clientData.get(3);

                                         LatLng clientCoord = new LatLng(latitude, longitude);
                                         /* display data received from the client */
                                         serverStatus2.setText("Client: " + name + "\n[" + time + "]\nLatitude: " + latitude +
                                                 "\nLongitude: " + longitude);

					                     /* clear the map of previous markers */
                                         googleMap.clear();
                                         googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_dot))
                                                 .position(clientCoord).flat(true).title(name));

                                         /* position camera on new location */
                                         CameraPosition cameraPosition = CameraPosition.builder()
                                                                         .tilt(30)
                                                                         .target(clientCoord).zoom(15).build();

                                           /* animate the change in camera view over 2 seconds */
                                         googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),
                                                 2000, null);

                                     }
                                 });
                             }
                             serverSocket.close();
                             break;
                         } catch (Exception e) {
                             handler.post(new Runnable() {
                                 @Override
                                 public void run() {
                                     serverStatus.setText("Oops. Connection interrupted. Please reconnect your phones.");
                                 }
                             });
                             e.printStackTrace();
                         }
                     }
                 } else {
                     handler.post(new Runnable() {
                         @Override
                         public void run() {
                             serverStatus.setText("Couldn't detect internet connection.");
                         }
                     });
                 }
             } catch (final Exception e) {
                 handler.post(new Runnable() {
                     @Override
                     public void run() {
                         serverStatus.setText("Error" + e.getMessage());
                     }
                 });
                 e.printStackTrace();
             }
         }
     }

     @Override
     protected void onStop() {
         super.onStop();
         try {
             /* close socket upon exiting */
             serverSocket.close();
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
 }

