package com.example.elevate;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.elevate.maps.MapFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnNavigationItemSelectedListener(bottomNavMethod);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new MapFragment());
    }

    private BottomNavigationView.OnNavigationItemSelectedListener bottomNavMethod = item -> {
        Fragment fragment = null;

        switch (item.getItemId()) {
            case R.id.home:
                fragment = new HomeFragment();
                break;
            case R.id.maps:
                fragment = new MapFragment();
                break;
        }
        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
        return true;
    };
}
