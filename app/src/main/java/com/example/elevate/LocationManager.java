package com.example.elevate;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Objects;

public class LocationManager {
    private static LocationManager requestManager;
    private FusedLocationProviderClient mLocationProviderClient;
    private Location mLocation;
    private Location mMyCurrentLocation;
    private locationSuccessListener mListener;

    public Location getLocation() {
        return mLocation;
    }

    public void setLocation(Location location) {
        mLocation = location;
    }

    private LocationManager(FusedLocationProviderClient fusedLocationProviderClient) {
        this.mLocationProviderClient = fusedLocationProviderClient;
    }

    public static LocationManager createInstance(FusedLocationProviderClient fusedLocationProviderClient) {
        if (requestManager != null) {
            return requestManager;
        } else {
            return requestManager = new LocationManager(fusedLocationProviderClient);
        }
    }

    public static LocationManager getInstance() {
        return requestManager;
    }

    public void setLocation(Activity activity) {
        mListener = (locationSuccessListener) activity;
        LocationCallback callback = new LocationCallback();
        LocationRequest locationRequest = new LocationRequest();
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(activity), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
        Task<Void> r = mLocationProviderClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper());
        r.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (ContextCompat.checkSelfPermission(Objects.requireNonNull(activity), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            0);
                }
                mLocationProviderClient.getLastLocation().addOnSuccessListener(activity, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        mMyCurrentLocation = location;
                        mLocation = location;
                        if (location != null) {
                            mListener.onLocationReceived();
                            mLocationProviderClient.removeLocationUpdates(callback);
                        }
                    }
                });
            }
        });


    }

    public Location getMyCurrentLocation() {
        return mMyCurrentLocation;
    }


    public interface locationSuccessListener {
        void onLocationReceived();
    }
}
