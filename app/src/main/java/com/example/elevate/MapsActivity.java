package com.example.elevate;

import android.Manifest;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;

import com.example.elevate.directions.DirectionsTaskParser;
import com.example.elevate.geofences.GeofenceHelper;
import com.example.elevate.location.LocationManager;
import com.example.elevate.maps.MapViewModel;
import com.example.elevate.maps.MapFragment;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationManager.locationSuccessListener {

    private GoogleMap map;
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private MapViewModel viewModel;
    private MutableLiveData<Location> currentLocation;
    private BottomNavigationView bottomNavigationView;
    private int currentID;
    private LatLng destination;


    private static final String TAG = "MapsActivity";
    private static final int ZOOM = 15;
    private static final int GEOFENCE_RADIUS = 150;
    private static final int SAMPLING_DISTANCE = 75; //The distance between two consecutive points in metres
    private static final int REQUEST_LOCATION_PERMISSION = 1000;
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String ELEVATION_URL = "https://maps.googleapis.com/maps/api/elevation/";
    private static final String JSON_OUTPUT_FORMAT = "json";
    private static final String API_KEY = BuildConfig.CONSUMER_KEY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.map);
//        assert mapFragment != null;
//        mapFragment.getMapAsync(this);

        this.bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnNavigationItemSelectedListener(bottomNavMethod);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new MapFragment());
//        this.geofencingClient = LocationServices.getGeofencingClient(this);
//        LocationManager.createInstance(LocationServices.getFusedLocationProviderClient(this));
//        LocationManager.getInstance().setLocation(this);
//        this.geofenceHelper = new GeofenceHelper(this);
//        this.viewModel = new InsertMapElementsViewModel();
//        this.currentID = 1;
//        this.currentLocation = new MutableLiveData<>();
//        GeofenceBroadcastReceiver receiver = new GeofenceBroadcastReceiver();
//        receiver.setMainActivityHandler(this);
//        IntentFilter fltr_smsreceived = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
//        registerReceiver(receiver,fltr_smsreceived);
//        viewModel.pointsToAlertAt.observe(this, latLngs -> {
//            for (LatLng position : latLngs) {
//                addCircle(position, GEOFENCE_RADIUS);
//                addGeofence(position, GEOFENCE_RADIUS, "ID"+currentID);
//                currentID++;
//            }
//        });
    }

    private BottomNavigationView.OnNavigationItemSelectedListener bottomNavMethod = new
            BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment fragment = null;

                    switch (item.getItemId()) {
                        case R.id.maps:
                            fragment = new MapFragment();
                    }
                    getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
                    return true;
                }
            };

    @Override
    public void onMapReady(GoogleMap googleMap) {
//        map = googleMap;
//        map.getUiSettings().setZoomControlsEnabled(true); //Allows for zoom activity on map
//        enableMyLocation();
//        currentLocation.observe(this, location ->
//                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
//                        location.getLongitude()), ZOOM)));
//        addMarkers();
    }

    private void addMarkers() {
        map.setOnMapLongClickListener(latLng -> {
            map.clear();
            geofencingClient.removeGeofences(geofenceHelper.getPendingIntent());
            destination = latLng;
            MarkerOptions myMarker = new MarkerOptions();
            myMarker.position(destination);

            myMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

            map.addMarker(myMarker);
            polylineToMarker();
        });
    }

    private void polylineToMarker() {
        if (destination != null) {
            currentLocation.observe(this, location -> {
                String directionURL = getDirectionURL(new LatLng(location.getLatitude(),
                        location.getLongitude()), destination);
                TaskRunner taskRunner1 = new TaskRunner();
                taskRunner1.executeAsync(new TaskRequest(directionURL), (data1) -> {
                    TaskRunner taskRunner2 = new TaskRunner();
                    taskRunner2.executeAsync(new DirectionsTaskParser(data1), (data2) -> {

                        for (Map<String, Integer> path : data2) {
                            for (String encodedPath : path.keySet()) {

                                elevationCalculation(encodedPath, path.get(encodedPath));
                            }
                        }

                    });
                });
            });
        }
    }

    private void elevationCalculation(String encodedPath, int distance) {
        String elevationURL = getElevationURL(encodedPath, distance);
        viewModel.runTask(elevationURL, map);
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

    private void addGeofence(LatLng centre, float radius, String ID) {
        Geofence geofence = geofenceHelper.getGeofence(ID, centre, radius,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //TODO: Add the permission check
            return;
        }

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "onSuccess: Geofence Added"))
                .addOnFailureListener(e -> {
                    String errorMessage = geofenceHelper.getErrorString(e);
                    Log.d(TAG, "onFailure" + errorMessage);
                });
    }

    private void addCircle(LatLng centre, float radius) {
        map.addCircle(new CircleOptions()
                .center(centre)
                .radius(radius)
                .strokeColor(Color.argb(255, 255, 0, 0))
                .fillColor(Color.argb(64, 255, 0, 0))
                .strokeWidth(4));
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            map.setMyLocationEnabled(true);
            // already permission granted
        }

    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    map.setMyLocationEnabled(true);
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    public void onLocationReceived() {
        currentLocation.setValue(LocationManager.getInstance().getMyCurrentLocation());
        if (currentLocation == null){
            Toast.makeText(this, "unable to get location", Toast.LENGTH_SHORT).show();
        }
    }
}