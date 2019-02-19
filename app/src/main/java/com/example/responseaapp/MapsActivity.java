package com.example.responseaapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private static final String TAG = "tagIt";

    Button completeResponse;

    private SupportMapFragment mapFragment;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private SharedPreferences mPreference;
    private SharedPreferences.Editor mEditor;


    LatLng genLatLng;
    LatLng emergencyLocLatLng;

    String ambulanceIdentifyer = "Ambulance001";
    String emergencyId = "";

    private FusedLocationProviderClient mFusedLocationProviderClient;

    DatabaseReference myRef;
    DatabaseReference ambulanceInfo;
    DatabaseReference putToWorking;


    GeoFire geoFireWorking;
    GeoFire geoFireWaiting;


    /// DAMN LETS DO SAFE CODING

    private Marker mMarker;
    private Marker emergencyMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toast.makeText(this, "onCreate", Toast.LENGTH_SHORT).show();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        myRef = FirebaseDatabase.getInstance().getReference().child("AvailableAmbulance");
        putToWorking = FirebaseDatabase.getInstance().getReference().child("WorkingAmbulance");
        ambulanceInfo = FirebaseDatabase.getInstance().getReference().child("AmbulanceInfo").child(ambulanceIdentifyer).child("EmergencyId");


        geoFireWorking = new GeoFire(putToWorking);
        geoFireWaiting = new GeoFire(myRef);



        completeResponse = findViewById(R.id.bCompleteResponse);

        completeResponse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference rmvEmergency = FirebaseDatabase.getInstance().getReference().child("EmergencyLocation").child(emergencyId);
                rmvEmergency.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        emergencyMarker.remove();
                    }
                });
                ambulanceInfo.removeValue();

                geoFireWorking.removeLocation(ambulanceIdentifyer, new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        startActivity(getIntent());
                        finish();
                        overridePendingTransition(0, 0);
                    }
                });
            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // get latest location here.
        mLocationRequest = new LocationRequest();
        // update interval to 1000ms
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // get updated Location every second

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SDK Greater than 23 and has Permission", Toast.LENGTH_SHORT).show();
                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(this, "SDK Greater than 23 and doesnt have permission", Toast.LENGTH_SHORT).show();
                checkLocationPermission();
            }
        } else {
            Toast.makeText(this, "SDK Less than 23", Toast.LENGTH_SHORT).show();
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }


    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                // function called every seconds
                mLastLocation = location;
                genLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(genLatLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
                if (mMarker != null) {
                    mMarker.remove();
                }
                mMarker = mMap.addMarker(new MarkerOptions().position(genLatLng).title("My Location"));


                ambulanceInfo.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            emergencyId = "";

                        } else {
                            emergencyId = dataSnapshot.getValue().toString();
                            getEmergencyLocation(emergencyId);

                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                switch (emergencyId) {
                    case "":
                        geoFireWaiting.setLocation(ambulanceIdentifyer, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                            }
                        });
                        geoFireWorking.removeLocation(ambulanceIdentifyer, new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                            }
                        });

                        break;
                    default:

                        geoFireWorking.setLocation(ambulanceIdentifyer, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                putToWorking.child(ambulanceIdentifyer).child("EmergencyId").setValue(emergencyId);
                            }
                        });
                        geoFireWaiting.removeLocation(ambulanceIdentifyer, new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                            }
                        });

                        break;
                }

            }
        }
    };

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Give Permission")
                        .setMessage("Please Give Location Permission")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }
            }
        }
    }

    private void getEmergencyLocation(String key) {
        DatabaseReference dbReff = FirebaseDatabase.getInstance().getReference().child("EmergencyLocation").child(key).child("l");
        dbReff.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                double Lat = 0;
                double Lng = 0;
                if (dataSnapshot.exists()){
                    Lat = Double.parseDouble(dataSnapshot.child("0").getValue().toString());
                    Lng = Double.parseDouble(dataSnapshot.child("1").getValue().toString());

                    emergencyLocLatLng = new LatLng(Lat, Lng);
                    Log.d(TAG, "onDataChange: " + emergencyLocLatLng);
                    emergencyMarker = mMap.addMarker(new MarkerOptions().position(emergencyLocLatLng).title("Emergency Location"));

                    Location loc1 = new Location("");
                    loc1.setLatitude(genLatLng.latitude);
                    loc1.setLongitude(genLatLng.longitude);


                    Location loc2 = new Location("");
                    loc2.setLatitude(Lat);
                    loc2.setLongitude(Lng);

                    float distance = loc1.distanceTo(loc2);

                    if (distance < 10) {
                        completeResponse.setVisibility(View.VISIBLE);
                    }
                }else{
                    if (emergencyMarker != null){
                        emergencyMarker.remove();
                    }
                }



            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    private void disconnectAmbulance() {
        geoFireWaiting.removeLocation(ambulanceIdentifyer, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });
        checkLocationPermission();
        if (mFusedLocationProviderClient != null) {
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
            mMap.setMyLocationEnabled(false);
        }
    }


//
//
//    @Override
//    public void onLocationChanged(Location location) {
//        // function called every second
//        // if not available
//
//
//    }
//
//
//    @Override
//    public void onConnected(@Nullable Bundle bundle) {
//
//
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//
//    }
//
//    @Override
//    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
//
//    }


    // CREATED FUNCTIONS


}
