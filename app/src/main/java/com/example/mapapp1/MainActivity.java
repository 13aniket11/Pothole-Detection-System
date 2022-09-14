package com.example.mapapp1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener   {
    private Button start, stop;
    private TextView status;
    private int flg=0;
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private FirebaseFirestore db;
    private LocationRequest locationRequest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);
        db = FirebaseFirestore.getInstance();
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener((SensorEventListener) MainActivity.this,gyroscopeSensor,sensorManager.SENSOR_DELAY_UI);

        if (gyroscopeSensor == null) {
            Toast.makeText(this, "device has no gyroscope", Toast.LENGTH_SHORT).show();
            finish();
        }
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flg=1;
                status.setText("Connected");
                status.setTextColor(Color.parseColor("#008037"));
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flg=0;
                status.setText("Disconnected");
                status.setTextColor(Color.parseColor("#FF1616"));
            }
        });
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
//        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER && (event.values[0] > 20f || event.values[0] < -20f || event.values[1] > 20f || event.values[1] < -20f || event.values[2] > 20f || event.values[2] < -20f)) {
//            flg=2;
//            status.setText(event.values[0] + "\n" + event.values[1] + "\n" + event.values[2]);
//        }
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE && flg==1 && (event.values[0] > 5f || event.values[0] < -5f || event.values[1] > 5f || event.values[1] < -5f || event.values[2] > 5f || event.values[2] < -5f)){
//            status.setText(event.values[0] + "\n" + event.values[1] + "\n" + event.values[2]);
//            flg=1;
            getCurrentLocation();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

            @Override
            public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);

                if (requestCode == 1){
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED){

                        if (isGPSEnabled()) {

                            getCurrentLocation();

                        }else {

                            turnOnGPS();
                        }
                    }
                }


            }

            @Override
            protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                super.onActivityResult(requestCode, resultCode, data);

                if (requestCode == 2) {
                    if (resultCode == Activity.RESULT_OK) {

                        getCurrentLocation();
                    }
                }
            }
            private void getCurrentLocation() {


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        if (isGPSEnabled()) {

                            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                                        @Override
                                        public void onLocationResult(@NonNull LocationResult locationResult) {
                                            super.onLocationResult(locationResult);

                                            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                                    .removeLocationUpdates(this);

                                            if (locationResult != null && locationResult.getLocations().size() >0){

                                                int index = locationResult.getLocations().size() - 1;
                                                double latitude = locationResult.getLocations().get(index).getLatitude();
                                                double longitude = locationResult.getLocations().get(index).getLongitude();

//                                                status.setText("Latitude: "+ latitude + "\n" + "Longitude: "+ longitude);
                                                Map<String, Object> user = new HashMap<>();
                                                user.put("Latitude", latitude);
                                                user.put("Longitude", longitude);
                                                db.collection("users")
                                                        .add(user)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Toast.makeText(MainActivity.this, "Location Stored Successfully !", Toast.LENGTH_SHORT).show();
                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Toast.makeText(MainActivity.this, "Error - " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        });

                                            }
                                        }
                                    }, Looper.getMainLooper());

                        } else {
                            turnOnGPS();
                        }

                    } else {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    }
                }
            }
            private void turnOnGPS() {



                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest);
                builder.setAlwaysShow(true);

                Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                        .checkLocationSettings(builder.build());

                result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<LocationSettingsResponse> task) {

                        try {
                            LocationSettingsResponse response = task.getResult(ApiException.class);
                            Toast.makeText(MainActivity.this, "GPS is already tured on", Toast.LENGTH_SHORT).show();

                        } catch (ApiException e) {

                            switch (e.getStatusCode()) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                                    try {
                                        ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                        resolvableApiException.startResolutionForResult(MainActivity.this, 2);
                                    } catch (IntentSender.SendIntentException ex) {
                                        ex.printStackTrace();
                                    }
                                    break;

                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    //Device does not have location
                                    break;
                            }
                        }
                    }
                });

            }
            private boolean isGPSEnabled() {
                LocationManager locationManager = null;
                boolean isEnabled = false;

                if (locationManager == null) {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                }

                isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                return isEnabled;

            }
}