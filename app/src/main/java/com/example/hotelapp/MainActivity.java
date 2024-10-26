package com.example.hotelapp;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hotelapp.fragments.LandmarkFragment;
import com.example.hotelapp.fragments.MapsFragment;
import com.example.hotelapp.fragments.SearchFragment;
import com.example.hotelapp.fragments.TrackerFragment;
import com.example.hotelapp.fragments.WeatherFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        databaseReference = FirebaseDatabase.getInstance().getReference("userInputs");

        // Load default fragment
        loadFragment(new SearchFragment());
        readInputsFromFirebase();

        // Set up BottomNavigationView listener
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.nav_search) {
                    selectedFragment = new SearchFragment(); // Create your SearchFragment
                } else if (item.getItemId() == R.id.nav_maps) {
                    selectedFragment = new MapsFragment(); // Create your MapsFragment
                } else if (item.getItemId() == R.id.nav_tracker) {
                    selectedFragment = new TrackerFragment(); // Create your TrackerFragment
                } else if (item.getItemId() == R.id.nav_weather) {
                    selectedFragment = new WeatherFragment(); // Create your WeatherFragment
                }
                else if (item.getItemId() == R.id.nav_login) {
                    selectedFragment = new LandmarkFragment(); // Create your WeatherFragment
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }

                return true;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        // Replace the current fragment with the selected one
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    private void readInputsFromFirebase() {
        // Example user ID or get the user-specific reference from your logic
        String userId = "example_user_id";
        DatabaseReference userReference = databaseReference.child(userId);

        userReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StringBuilder inputs = new StringBuilder();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String input = snapshot.getValue(String.class);
                    inputs.append(input).append("\n");
                }
                // Example: Use inputs or process as needed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error
            }
        });
    }
}
