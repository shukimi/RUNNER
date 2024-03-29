package com.example.runner;

import android.content.Intent;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.runner.databinding.ActivityHomePageBinding;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HomePage extends AppCompatActivity {

    ActivityHomePageBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;
    private List<Calendar> calendars;
    private SupportMapFragment supportMapFragment;
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_home_page);
        binding = ActivityHomePageBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        //FIREBASE - GET CURRENT USER ID +GET PATH OF DB NAMED "USERS"
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        //LAST LOGIN WIDGET
        databaseReference.child(firebaseUser.getUid()).child("logOut").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                String logOut=String.valueOf(task.getResult().getValue());
                if (!task.isSuccessful()) {
                    Log.e("shuki", "Error getting data", task.getException());
                }
                else {
                    if (logOut.equals("1"))
                    {
                        binding.lastLogin.setText("First Login Welcome");
                    }
                    else {
                        binding.lastLogin.setText("Last Login : " +  logOut);
                    }
                }
            }
        });


        //CALENDAR WIDGET - CREATE CALENDAR LIST
        calendars = new ArrayList<>();
        //HIGHLIGHT RUNNING DATES IN CALENDAR
        db.collection(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //GET FIRESTORE DOCUMENT TITLE
                                String dateString = document.getId();
                                //HIGHLIGHT DATES CALENDAR
                                DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                                Calendar cal = Calendar.getInstance();
                                try {
                                    cal.setTime(df.parse(dateString));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                calendars.add(cal);
                               binding.calendarView.setHighlightedDays(calendars);
                            }
                        } else {
                            Log.w("logw", "Error getting documents.", task.getException());
                        }
                    }
                });

        //CARD WIDGET - GET USER LAST SELF RUN + ASSIGN IN CARDVIEW
        db.collection(firebaseUser.getUid()).orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            //IF DEFAULT USER HAS NO RUNS SHOW
                            if (task.getResult().isEmpty())
                            {
                                binding.distance.setText("Start Your first run today!");
                            }
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String t = getTimeStamp((Timestamp) document.getData().get("timestamp"));
                                String d = document.getData().get("distance").toString();
                                String c = document.getData().get("chronometer").toString();
                                binding.timestamp.setText("Date : " + t);
                                binding.distance.setText("Distance : " + d + "km");
                                binding.chronometer.setText("StopWatch (min) : " + c);
                            }
                        }
                        else {
                            Log.w("logw", "Error getting documents.", task.getException());
                        }
                    }
                });

        //SETTING TOP NAV BAR
        topNavBar();

        //SETTING BOTTOM NAV BAR
        bottomNavBar();

    }

    // method that write back Timestamp to string
    public String getTimeStamp(Timestamp timestamp) {
        Date date = new Date(2000, 11, 21);
        try {
            date = timestamp.toDate();
        } catch (Exception e) {

        }
        long timestampl = date.getTime();
        java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String dateStr = simpleDateFormat.format(timestampl);
        return dateStr;
    }

    //SETTING BOTTOM NAV BAR
    private void bottomNavBar() {
        binding.bottomNavBar.setSelectedItemId(R.id.home_page); // to keep the icon on
        binding.bottomNavBar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home_page:
                        // start new activity....
                        //overridePendingTransition(0, 0);
                        break;
                    case R.id.club:
                        Intent intentC = new Intent(HomePage.this, Club.class);
                        startActivity(intentC);
                        overridePendingTransition(0, 0);
                        break;
                    case R.id.run:
                        Intent intentR = new Intent(HomePage.this, Run.class);
                        startActivity(intentR);
                        overridePendingTransition(0, 0);
                        break;
                    case R.id.activity:
                        Intent intentS = new Intent(HomePage.this, Activity.class);
                        startActivity(intentS);
                        overridePendingTransition(0, 0);
                        break;
                    case R.id.profile:
                        Intent intent = new Intent(HomePage.this, Profile.class);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                        break;
                }
                return true;
            }
        });
    }

    //SETTING BOTTOM NAV BAR
    private void topNavBar() {
        //SETTING TOP NAV BAR
        binding.topAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.logOut:
                        String logOut = HomePage.getCurrentDateTime();
                        databaseReference = FirebaseDatabase.getInstance().getReference("users");
                        databaseReference.child(firebaseUser.getUid()).child("logOut").setValue(logOut);
                        Intent intent = new Intent(HomePage.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        FirebaseAuth.getInstance().signOut();
                        startActivity(intent);
                        break;
                }
                return true;
            }
        });
    }

    // for smooth transitions between activity & for the bottom-nav button to be pressed
    @Override
    protected void onRestart() {
        super.onRestart();
        binding.bottomNavBar.setSelectedItemId(R.id.home_page);
        overridePendingTransition(0, 0);
    }

    //GET PARAMETER FOR LAST LOGIN
    public static String getCurrentDateTime() {
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String datetime = ft.format(dNow);
        return datetime;
    }


}
