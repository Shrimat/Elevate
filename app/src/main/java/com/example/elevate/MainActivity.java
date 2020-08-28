package com.example.elevate;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.elevate.location.LocationManager;
import com.example.elevate.location.LocationViewModel;
import com.example.elevate.maps.MapFragment;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements LocationManager.locationSuccessListener{

    private BottomNavigationView bottomNavigationView;
    private LocationViewModel locationViewModel;
    private MapFragment mapFragment;

    private static final int ZOOM = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LocationManager.createInstance(LocationServices.getFusedLocationProviderClient(this));
        LocationManager.getInstance().setLocation(this);
        this.bottomNavigationView = findViewById(R.id.bottomNav);
        this.locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        this.mapFragment = new MapFragment();
        bottomNavigationView.setOnNavigationItemSelectedListener(bottomNavMethod);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, mapFragment);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener bottomNavMethod = item -> {
        Fragment fragment = null;

        switch (item.getItemId()) {
            case R.id.home:
                fragment = new HomeFragment();
                break;
            case R.id.maps:
                fragment = mapFragment;
                break;
        }
        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
        return true;
    };

    @Override
    public void onLocationReceived() {
        locationViewModel.setCurrentLocation(LocationManager.getInstance().getMyCurrentLocation());
        if (locationViewModel.getCurrentLocation().getValue() == null){
            Toast.makeText(this, "unable to get location", Toast.LENGTH_SHORT).show();
        }
    }

    public LocationViewModel getLocationViewModel() {
        return locationViewModel;
    }
}
