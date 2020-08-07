package com.example.elevate;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.elevate.directions.DirectionsTaskParser;
import com.example.elevate.elevation.ElevationTaskParser;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Cap;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private GeofencingClient geofencingClient;
    private FusedLocationProviderClient fusedLocationClient;
    private GeofenceHelper geofenceHelper;
    private static final String TAG = "MapsActivity";

    private static final int ZOOM = 13;
    private static final int GEOFENCE_RADIUS = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 10001;
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String ELEVATION_URL = "https://maps.googleapis.com/maps/api/elevation/";
    private static final String JSON_OUTPUT_FORMAT = "json";
    private static final String API_KEY = BuildConfig.CONSUMER_KEY;
    private static final int SAMPLING_DISTANCE = 50; //The distance between two consecutive points in metres
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
        this.geofencingClient = LocationServices.getGeofencingClient(this);
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        this.geofenceHelper = new GeofenceHelper(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        //Allows for zoom activity on map
        map.getUiSettings().setZoomControlsEnabled(true);
        enableMyLocation();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(getCurrentLocation(), ZOOM));
        addMarkers();
    }

    private void addMarkers() {
        map.setOnMapLongClickListener(latLng -> {
            map.clear();
            destination = latLng;
            MarkerOptions myMarker = new MarkerOptions();
            myMarker.position(destination);

            myMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

            map.addMarker(myMarker);
            polylineToMarker();
            addCircle(destination, GEOFENCE_RADIUS);
            addGeofence(destination, GEOFENCE_RADIUS);
//            if (Build.VERSION.SDK_INT >= 29) {
//                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
//                        == PackageManager.PERMISSION_GRANTED) {
//                    addCircle(destination, GEOFENCE_RADIUS);
//                    addGeofence(destination, GEOFENCE_RADIUS);
//                } else {
//                    addCircle(destination, GEOFENCE_RADIUS);
//                    addGeofence(destination, GEOFENCE_RADIUS);
//                }
//            }
        });
    }

    private void polylineToMarker() {
        if (destination != null) {
            String directionURL = getDirectionURL(getCurrentLocation(), destination);
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
        String samples = "samples=" + (distance / SAMPLING_DISTANCE + 2);
        String key = "key=" + API_KEY;
        return ELEVATION_URL
                + JSON_OUTPUT_FORMAT + "?"
                + pathString + "&"
                + samples + "&"
                + key;
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
                            .color(getColour(data4.get(prev), data4.get(curr)))
                            .startCap(CAP_TYPE)
                            .endCap(CAP_TYPE)
                            .geodesic(true);

                    prev = curr;
                    map.addPolyline(polylineOptions);
                }

            });
        });
    }

    private int getColour(double start, double end) {
        double gradient = (end - start) / SAMPLING_DISTANCE;
        if (gradient < 0) {
            return Color.GREEN;
        } else if (0 <= gradient && gradient <= 0.05) {
            return Color.YELLOW;
        } else {
            return Color.RED;
        }

    }

    private void addGeofence(LatLng centre, float radius) {
        GeofencingRequest geofencingRequest = geofenceHelper
                .getGeofencingRequest(geofenceHelper
                        .getGeofence("ID", centre, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, geofenceHelper.getPendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Geofence Added");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure" + errorMessage);
                    }
                });
    }

    private void addCircle(LatLng centre, float radius) {
        map.addCircle(new CircleOptions()
                .center(centre)
                .radius(radius)
                .strokeColor(Color.argb(255,  255, 0, 0))
                .fillColor(Color.argb(64, 255, 0, 0))
                .strokeWidth(4));
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation();
                    break;
                }
        }
    }

    @SuppressWarnings("MissingPermission")
    private LatLng getCurrentLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        assert locationManager != null;
        android.location.Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        return new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
    }

}