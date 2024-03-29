package com.example.runner;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.runner.data.Splits;
import com.example.runner.databinding.ActivityNewRunBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NewRun extends AppCompatActivity implements View.OnClickListener {
    //binding
    private ActivityNewRunBinding binding;
    // my edit
    private ImageButton startPauseButton;
    //fragment vars
    private GoogleMap map;
    private Polyline polyline;
    private PolylineOptions polylineOptions;
    // current location
    private SupportMapFragment supportMapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private Looper looper;
    // Vars
    //Chronometer vars
    private boolean stopStart;
    private long pauseOffset;

    private float distanceFar;
    private float lat1, lng1, lat2, lng2;
    private int length;

    //splits
    // stroe the splits objects
    private ArrayList<Splits> splitsArrayList;
    //global var to store the km
    private String km;
    // iteration for counting the km for split
    private int kmToran;

    //timer for splits
    private Timer timer;
    private TimerTask timerTask;
    private Double time = 0.0;
    private boolean timerStarted;
    private String currentSplit;
    private Integer flag=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //binding
        binding = ActivityNewRunBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // Initialize:
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        polylineOptions = new PolylineOptions();
        //my edit
        startPauseButton = findViewById(R.id.imageButtonStartPause);
        pauseOffset = 0;
        distanceFar = 0;
        resetChronometer();
        getCurrentLocation();
        //splits
        splitsArrayList = new ArrayList<>();
        kmToran = 1;

        //timer
        timer = new Timer();
        currentSplit = "";
        timerStarted = false;



//        focus.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                drawTrack();
//                Toast.makeText(NewRun.this,"focus?",Toast.LENGTH_SHORT).show();
//            }
//        });

        // create a location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                final LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                polylineOptions.add(latLng);
                supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        getFarAway(polylineOptions);
                        double track = distanceFar / 1000;
                        calcSplits(track);
                        km = String.valueOf(track);
                        try {
                            int dot = km.indexOf(".");
                            km = km.substring(0, dot + 3);
                        } catch (Exception e) {
                            System.out.println(km);
                        }
                        if (km.equals("0.0")) {
                            km = "0.00";
                        }
                        //binding.distanceText.setText("km: " + km);
                        binding.distanceText.setText(km);
                        drawTrack();
                    }
                });
            }
        };

        stopStart = true;
        requestLocation();
        // as the activity start both chronometer and the split timer will be activated
        startSplitTimer();
        startChronometer();

        //setting up stop button
        stopButton();

        topNavBar();

    }

    /*
             ----------------------------------   timer for splits methods -----------------------------------------------------


     */

    public void ResetSplitTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            time = 0.0;
            timerStarted = false;
        }
    }

    public void StartStopSplitTimer() {
        if (timerStarted == true) {
            timerStarted = false;
            startSplitTimer();
        } else if (timerStarted == false) {
            timerStarted = true;
            timerTask.cancel();
        }
    }

    private void startSplitTimer() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                time++;
                Log.d("splitt", "run: "+time);
//                runOnUiThread(new Runnable()
//                {
//                    @Override
//                    public void run()
//                    {
//
//                        Log.d("splitt", "run: "+time);
//                    }
//                });
//                Log.d("run?", "run: "+time);
            }

        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    private String getSplitTimerText() {
        int rounded = (int) Math.round(time);

        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        //int hours = ((rounded % 86400) / 3600);
        return formatTime(seconds, minutes);
    }

    private String formatTime(int seconds, int minutes) {
        return String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }

    // method for calculate the splits of each km
    private void calcSplits(double track) {
        if (track > kmToran) {
            currentSplit = getSplitTimerText();
            //Log.d("timer", "calcSplits: "+currentSplit);
            ResetSplitTimer();
            //create split obj
            Splits splits = new Splits(currentSplit, (int) track);
            splitsArrayList.add(splits);
            //Log.d("yossi", "calcSplits: "+splits.toString());
            kmToran++;
            currentSplit = "";
            startSplitTimer();
        }
    }

    /*
                ------------------------------------ end - timer for splits methods -------------------------------------------------------


     */


    private void stopButton() {
        binding.imageButtonStop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent intent = new Intent(NewRun.this, EndRunSummary.class);
                //passing all the values to the next activity for saving it in firebase
                intent.putExtra("chronometer", binding.timeChronometer.getText());
                intent.putExtra("distance", binding.distanceText.getText().toString());
                intent.putExtra("polylines", polylineOptions);
                intent.putExtra("splits", splitsArrayList);
                startActivity(intent);
                finish();
                // ending the split timer
                timerTask.cancel();
                return false;
            }
        });
    }

    @SuppressLint("MissingPermission")
    void requestLocation() {
        // get the client location every second
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, looper.myLooper());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imageButtonStartPause) {
            if(flag==0)
            {
                startPauseCycle();
                binding.topAppBar.setVisibility(View.GONE);
                flag=1;
            }
            else{
                startPauseCycle();
                binding.topAppBar.setVisibility(View.VISIBLE);
                flag=0;
            }
        } else if (v.getId() == R.id.imageButtonStop) {
            Toast.makeText(this, "Ending run with long click", Toast.LENGTH_SHORT).show();
        }
    }

    //SETTING TOP NAV BAR - CANCEL RUN BUTTON
    private void topNavBar() {
        //SETTING TOP NAV BAR
        binding.topAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.cancelNewRun:
                        startPauseCycle();
                        MaterialAlertDialogBuilder mainBuilder = new MaterialAlertDialogBuilder(NewRun.this);
                        mainBuilder.setTitle("")
                                .setMessage("Do you wish to cancel run (activity will not be saved)?")
                                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(NewRun.this, Run.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        startPauseCycle();
                                    }
                                }).create();
                        mainBuilder.show();
                        break;
                }
                return true;
            }
        });
    }

    //PAUSE RUN - CONTINUE RUN
    private void startPauseCycle() {
        if (!stopStart) {
            stopStart = true;
            startChronometer();
            requestLocation();
            StartStopSplitTimer();
            startPauseButton.setImageResource(R.drawable.ic_baseline_pause_24);
        } else {
            stopStart = false;
            pauseChronometer();
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            startPauseButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            StartStopSplitTimer();
        }
    }

    // cancel the split timer on back press
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        timerTask.cancel();
    }

    /*
         ----------------------------------   SETTING RUN PARAMETERS -----------------------------------------------------


 */

    // CHRONOMETER
    private void startChronometer() {
        binding.timeChronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
        binding.timeChronometer.start();
    }

    private void pauseChronometer() {
        binding.timeChronometer.stop();
        pauseOffset = SystemClock.elapsedRealtime() - binding.timeChronometer.getBase();
    }

    private void resetChronometer() {
        binding.timeChronometer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
    }

    private void getCurrentLocation() {
        // init the first location to avoid mistakes
        @SuppressLint("MissingPermission") Task<Location> task = fusedLocationProviderClient.getLastLocation();
    }

    //DISTANCE IN KM
    public float distance(float lat_a, float lng_a, float lat_b, float lng_b) {
        // calculate the distance between 2 points
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b - lat_a);
        double lngDiff = Math.toRadians(lng_b - lng_a);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;
        int meterConversion = 1609;
        return new Float(distance * meterConversion).floatValue();
    }

    //POLYLINE TRACK
    public void getFarAway(PolylineOptions road) {
        // calculating user's run by list of points
        List<LatLng> locations = road.getPoints();
        Object[] points = locations.toArray();
        length = points.length - 1;
        if (length > 0 && points[length - 1] instanceof LatLng && points[length] instanceof LatLng) {
            LatLng latlng1, latLng2;
            latlng1 = (LatLng) points[length - 1];
            latLng2 = (LatLng) points[length];
            lat1 = (float) latlng1.latitude;
            lng1 = (float) latlng1.longitude;
            lat2 = (float) latLng2.latitude;
            lng2 = (float) latLng2.longitude;
            distanceFar += distance(lat1, lng1, lat2, lng2);
        }
    }

    private void drawTrack() {
        // drawing the user track on the map using a list of coordinates
        @SuppressLint("MissingPermission") Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                if (location != null) {
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            map = googleMap;
                            googleMap.clear();
                            polyline = map.addPolyline(polylineOptions);
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.setMyLocationEnabled(true);
                            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                            googleMap.getUiSettings().setRotateGesturesEnabled(false);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                        }
                    });
                }
            }
        });
    }

    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem cancelNewRun = menu.findItem(R.id.cancelNewRun);
        if(true)
        {
            cancelNewRun.setVisible(false);
        }
        else
        {
            cancelNewRun.setVisible(true);
        }
        return true;
    }
}