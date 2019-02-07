package com.example.responseaapp;

import android.app.IntentService;
import android.content.Intent;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class background extends IntentService {

    DatabaseReference ambulanceInfo;
    String ambulanceIdentifyer = "Ambulance001";

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
//    public static final String ACTION_FOO = "com.example.responseaapp.action.FOO";
//    public static final String ACTION_BAZ = "com.example.responseaapp.action.BAZ";

    // TODO: Rename parameters
//    public static final String EXTRA_PARAM1 = "com.example.responseaapp.extra.PARAM1";
//    public static final String EXTRA_PARAM2 = "com.example.responseaapp.extra.PARAM2";

    public background() {
        super("background");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.ambulancesiren);
            ambulanceInfo = FirebaseDatabase.getInstance().getReference().child("AmbulanceInfo").child(ambulanceIdentifyer).child("EmergencyId");
            ambulanceInfo.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        Intent intent1 = new Intent(getBaseContext(), MapsActivity.class);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                        getApplication().startActivity(intent1);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


//            final String action = intent.getAction();
//            if (ACTION_FOO.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
//                handleActionFoo(param1, param2);
//            } else if (ACTION_BAZ.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
//                handleActionBaz(param1, param2);
//            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
