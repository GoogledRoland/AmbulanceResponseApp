package com.example.responseaapp;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Button toMap;

    String ambulanceIdentifyer = "Ambulance001";
    String emergencyIdentifyer = "";
    DatabaseReference ambulanceInfo;
    DatabaseReference isAvailable;
    DatabaseReference emergencyLocation;



    String mblNumVar, smsMsgVar;
    String sender;
    Double Lat; Double Lng;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ambulanceInfo = FirebaseDatabase.getInstance().getReference().child("AmbulanceInfo").child(ambulanceIdentifyer);
        ambulanceInfo.child("online").setValue(true);
        emergencyLocation = FirebaseDatabase.getInstance().getReference().child("EmergencyLocation");



        startService(new Intent(MainActivity.this, onAppKilled.class));

        SmsReceiver.bindListener(new SmsListener() {
            @Override
            public void messageReceived(String messageText) {

                Boolean isItRequest = messageText.contains("EMERGENCY");
                if (isItRequest){
                    String splitIt = messageText;
                    String result = splitIt.substring(splitIt.indexOf("(")+1,splitIt.indexOf(")"));
                    String[] separate = result.split(",");
                    Lat = Double.parseDouble(separate[0]);
                    Lng = Double.parseDouble(separate[1]);
                    sender = splitIt.substring(splitIt.indexOf("|")+1);


                }else{
                    // do nothing
                }


            }
        });




        isAvailable = FirebaseDatabase.getInstance().getReference().child("AmbulanceInfo").child(ambulanceIdentifyer).child("EmergencyId");
        isAvailable.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    // send a message that this ambulance is unavailable
                    smsMsgVar = "Sorry this Ambulance is Unavailable";
                    sendSmsMsg(sender, smsMsgVar);
                }else{
                    // get the id and coordinates
                    smsMsgVar = "Please Wait, We're on our Way";
                    sendSmsMsg(sender, smsMsgVar);
                    DatabaseReference elRef = FirebaseDatabase.getInstance().getReference().child("EmergencyLocation");
                    GeoFire geoFire = new GeoFire(elRef);
                    geoFire.setLocation(sender, new GeoLocation(Lat, Lng), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.ambulancesiren);
        ambulanceInfo = FirebaseDatabase.getInstance().getReference().child("AmbulanceInfo").child(ambulanceIdentifyer).child("EmergencyId");
        ambulanceInfo.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Intent intent1 = new Intent(MainActivity.this, MapsActivity.class);
                    startActivity(intent1);
                    mediaPlayer.start();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });




        toMap = findViewById(R.id.bToMap);
        toMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });










    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ambulanceInfo = FirebaseDatabase.getInstance().getReference().child("AmbulanceInfo");
        ambulanceInfo.child("online").setValue(false);
    }



    void sendSmsMsg(String mblNumVar, String smsMsgVar) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsMgrVar = SmsManager.getDefault();
                smsMgrVar.sendTextMessage(mblNumVar, null, smsMsgVar, null, null);
                Toast.makeText(getApplicationContext(), "Message Sent",
                        Toast.LENGTH_LONG).show();
            }
            catch (Exception ErrVar) {
                Toast.makeText(getApplicationContext(),ErrVar.getMessage().toString(), Toast.LENGTH_LONG).show();
                ErrVar.printStackTrace();
            }
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 10);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 10 : {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    try {
                        SmsManager smsMgrVar = SmsManager.getDefault();
                        smsMgrVar.sendTextMessage(mblNumVar, null, smsMsgVar, null, null);
                        Toast.makeText(getApplicationContext(), "Message Sent",
                                Toast.LENGTH_LONG).show();
                    }
                    catch (Exception ErrVar) {
                        Toast.makeText(getApplicationContext(),ErrVar.getMessage().toString(), Toast.LENGTH_LONG).show();
                        ErrVar.printStackTrace();
                    }
                }else{
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
