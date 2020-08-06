package com.example.elevate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.elevate.directions.DirectionsTaskParser;
import com.example.elevate.elevation.ElevationTaskParser;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Cap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private static final int LOCATION_REQUEST = 500;
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String ELEVATION_URL = "https://maps.googleapis.com/maps/api/elevation/";
    private static final String JSON_OUTPUT_FORMAT = "json";
    private static final String API_KEY = BuildConfig.ApiKey;
    private static final int SAMPLING_DISTANCE = 20; //The distance between two consecutive points in metres
    private static final Cap CAP_TYPE = new RoundCap();
    private LatLng destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        //Allows for zoom activity on map
        map.getUiSettings().setZoomControlsEnabled(true);
        enablePermissions();
        addMarkers();
    }

    private String getDirectionURL(LatLng origin, LatLng dest) {
        String originString = "origin=" + origin.latitude + "," + origin.longitude;
        String destString = "destination=" + dest.latitude + "," + dest.longitude;
        String mode = "mode=bicycling";
        String key = "key=" + API_KEY;
        return DIRECTIONS_URL
                + JSON_OUTPUT_FORMAT + "?"
                + originString + "&"
                + destString + "&"
                + mode + "&"
                + key;
    }

    private String getElevationURL(String encodedPolyline, int distance) {
        String pathString = "path=enc:" + encodedPolyline;
        String samples = "samples=" + (distance/SAMPLING_DISTANCE + 2);
        String key = "key=" + API_KEY;
        return ELEVATION_URL
                + JSON_OUTPUT_FORMAT + "?"
                + pathString + "&"
                + samples + "&"
                + key;
    }


    private void addMarkers() {
        map.setOnMapLongClickListener(latLng -> {

            if (destination != null) {
                destination = null;
                map.clear();
            }
            destination = latLng;
            MarkerOptions myMarker = new MarkerOptions();
            myMarker.position(latLng);

            myMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

            map.addMarker(myMarker);

            polylineToMarker();
        });
    }

    @SuppressLint("MissingPermission")
    private void polylineToMarker() {
        if (destination != null) {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            String locationProvider = LocationManager.NETWORK_PROVIDER;
            android.location.Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            LatLng currentLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            String directionURL = getDirectionURL(currentLocation, destination);
            TaskRunner taskRunner1 = new TaskRunner();
            taskRunner1.executeAsync(new TaskRequest(directionURL), (data1) -> {
                TaskRunner taskRunner2 = new TaskRunner();
                taskRunner2.executeAsync(new DirectionsTaskParser(data1), (data2) -> {

                    for (Map<String,Integer> path : data2) {
                        for (String encodedPath : path.keySet()) {

                            elevationCalculation(encodedPath, path.get(encodedPath));
                        }
                    }
                });
            });
        }
    }

    private void elevationCalculation(String encodedPath, int distance) {
        String elevationURL = getElevationURL(encodedPath, distance);
        TaskRunner taskRunner3 = new TaskRunner();
        taskRunner3.executeAsync(new TaskRequest(elevationURL), (data3) -> {
            TaskRunner taskRunner4 = new TaskRunner();
            taskRunner4.executeAsync(new ElevationTaskParser(data3), (data4) -> {
                LatLng prev = null;
                for (LatLng curr : data4.keySet()) {
                    PolylineOptions polylineOptions = new PolylineOptions();
                    if (prev == null) {
                        prev = curr;
                        continue;
                    }
                    polylineOptions.add(prev, curr)
                            .width(15)
                            .color(getColour(data4.get(prev), data4.get(curr), SAMPLING_DISTANCE))
                            .startCap(CAP_TYPE)
                            .endCap(CAP_TYPE)
                            .geodesic(true);
                    prev = curr;
                    map.addPolyline(polylineOptions);
                }

            });
        });
    }

    private int getColour(double start, double end, double sectionDistance) {
        double gradient = (end - start) / sectionDistance;
        if (gradient < 0) {
            return Color.GREEN;
        } else if (0 <= gradient && gradient <= 10) {
            return Color.YELLOW;
        } else {
            return Color.RED;
        }

    }

    private void enablePermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        map.setMyLocationEnabled(true);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST) {
            if (grantResults.length > 0 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                map.setMyLocationEnabled(true);
            }
        }
    }



}