package com.example.runner;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.runner.data.Splits;
import com.example.runner.databinding.ActivityEndRunSummaryBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndRunSummary extends AppCompatActivity {

    //binding
    private ActivityEndRunSummaryBinding binding;
    //Views
    private SupportMapFragment supportMapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap map;
    private Polyline polyline;
    private PolylineOptions polylineOptions;
    private Bundle extras;
    private String chronometer, distance, avgPace;
    private TextView distanceTv, timetv;
    private String timesOfDay;
    //Variables:
    private List<LatLng> points;
    private ArrayList<Splits> splitsArrayList;
    //Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore firebaseFirestore;
    private DocumentReference documentReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_end_run_summary);
        binding = ActivityEndRunSummaryBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // Initialize:
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map_end);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        polylineOptions = new PolylineOptions();
        extras = getIntent().getExtras();

        // init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();
        documentReference = firebaseFirestore.collection(firebaseUser.getUid()).document(getCurrentDateTime());


        if (extras != null) {
            chronometer = extras.getString("chronometer");
            distance = extras.getString("distance");
            polylineOptions = (PolylineOptions) extras.get("polylines");
            points = polylineOptions.getPoints();
            splitsArrayList = (ArrayList<Splits>) extras.get("splits");
        }
        Log.d("current", "onCreate: "+splitsArrayList.toString());
        binding.distanceTv.setText(distance);
        binding.timeTv.setText("Time: "+chronometer);
        drawTrack();

        setSummaryTextView();
        avgPace = calculateAverageOfTime(splitsArrayList);
        //Log.d("avgPace", "onCreate: "+avgPace);

        // Create a new run object and store it in firebase
        Map<String, Object> run = new HashMap<>();
        run.put("distance", distance);
        run.put("points", points);
        run.put("timestamp", getCurrentDateTime());
        run.put("chronometer", chronometer);
        run.put("splits", splitsArrayList);
        run.put("timesOfDay", timesOfDay);
        run.put("avgPace", avgPace);

        saveToFireStore(run);

        //testRead();


    }

        public static String calculateAverageOfTime(ArrayList<Splits> splitsArrayList) {
            long seconds = 0;
            for(Splits split : splitsArrayList) {
                String[] mmss = split.getTime().split(":");
                seconds += Integer.valueOf(mmss[0]) * 60;
                seconds += Integer.valueOf(mmss[1]);
            }
            seconds /= splitsArrayList.size();
            long mm = (seconds / 60) % 60;
            long ss = seconds % 60;

            return String.format("%02d:%02d",mm,ss);

    }

    private void setSummaryTextView() {
        StringBuilder firstLine = new StringBuilder();
        StringBuilder secondLine = new StringBuilder();

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        String dayString = "";
        switch (day) {
            case Calendar.SUNDAY:
                dayString = "Sunday";
                break;
            case Calendar.MONDAY:
                dayString = "Monday";
                break;
            case Calendar.TUESDAY:
                dayString = "Tuesday";
                break;
            case Calendar.WEDNESDAY:
                dayString = "Wednesday";
                break;
            case Calendar.THURSDAY:
                dayString = "Thursday";
                break;
            case Calendar.FRIDAY:
                dayString = "Friday";
                break;
            case Calendar.SATURDAY:
                dayString = "Saturday";
                break;
        }
        String timesOfDayTittle = "";
        // the first 2 numbers of the time - for example 08:50:32, it will take the 08 and call it morning
        int first2 = Integer.parseInt(getCurrentTime().substring(0,2));

        if(first2 > 0 && first2 < 5){
            timesOfDayTittle = "Midnight";
        }else if(first2 > 5 && first2 < 11){
            timesOfDayTittle = "Morning";
        }else if(first2 > 11 && first2 < 18){
            timesOfDayTittle = "Afternoon";
        }else{
            timesOfDayTittle = "Evening";
        }

        // set up the text
        firstLine.append(dayString + " "+getCurrentTime().substring(0,5));
        binding.firstLineTv.setText(firstLine);
        secondLine.append(dayString + " "+timesOfDayTittle+" Run");
        binding.secondLineTv.setText(secondLine);
        // for saving the timesOfDay in the fire store
        timesOfDay = secondLine.toString();
    }

//    private void testRead() {
//        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
//            @Override
//            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
//                if (error != null) {
//                    Toast.makeText(EndRunSummary.this, "Error!", Toast.LENGTH_SHORT).show();
//                    Log.d("EndRunSummary", error.toString());
//                    return;
//                }
//
//                if (value.exists()) {
//                    //Log.d("EndRunSummary1", value.getString("date"));
//                    //Log.d("EndRunSummary2", value.toString());
//                }
//            }
//        });
//
//    }

    private void saveToFireStore(Map<String, Object> run) {
        documentReference.set(run).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(EndRunSummary.this, "run saved", Toast.LENGTH_SHORT).show();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EndRunSummary.this, "run not saved", Toast.LENGTH_SHORT).show();
            }
        });
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
                            //MarkerOptions options = new MarkerOptions().position(latLng).title("You Are Here");
                            googleMap.setMyLocationEnabled(true);
                            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                            googleMap.getUiSettings().setRotateGesturesEnabled(false);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                            //googleMap.addMarker(options);
                        }
                    });
                }
            }
        });
    }

    private static String getCurrentDateTime() {
        // creating an id by time and date to the "SavedRuns" children
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        //SimpleDateFormat ft = new SimpleDateFormat("EEEE, d MMMM yyyy HH:mm:ss");
        String datetime = ft.format(dNow);
        return datetime;
    }

    private static String getCurrentTime() {
        // creating an id by time and date to the "SavedRuns" children
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss");
        //SimpleDateFormat ft = new SimpleDateFormat("EEEE, d MMMM yyyy HH:mm:ss");
        String datetime = ft.format(dNow);
        return datetime;
    }

    private static String getCurrentDate() {
        // creating an id by time and date to the "SavedRuns" children
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy");
        //SimpleDateFormat ft = new SimpleDateFormat("EEEE, d MMMM yyyy HH:mm:ss");
        String datetime = ft.format(dNow);
        return datetime;
    }
}