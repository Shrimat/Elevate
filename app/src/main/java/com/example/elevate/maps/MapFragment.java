package com.example.elevate.maps;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.elevate.BuildConfig;
import com.example.elevate.R;
import com.example.elevate.TaskRequest;
import com.example.elevate.TaskRunner;
import com.example.elevate.directions.DirectionsTaskParser;
import com.example.elevate.geofences.GeofenceHelper;
import com.example.elevate.location.LocationViewModel;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private LocationViewModel locationViewModel;
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private MapViewModel mapViewModel;

    private static final int REQUEST_LOCATION_PERMISSION = 1000;
    private static final int ZOOM = 15;
    private static final int GEOFENCE_RADIUS = 150;
    private static final int SAMPLING_DISTANCE = 75; //The distance between two consecutive points in metres
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String ELEVATION_URL = "https://maps.googleapis.com/maps/api/elevation/";
    private static final String JSON_OUTPUT_FORMAT = "json";
    private static final String API_KEY = BuildConfig.CONSUMER_KEY;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_view, container, false);

        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        SupportMapFragment fragment = new SupportMapFragment();
        transaction.add(R.id.mapView, fragment);
        transaction.commit();

        fragment.getMapAsync(this);
        this.locationViewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
        this.mapViewModel = new MapViewModel();
        this.geofencingClient = LocationServices.getGeofencingClient(getActivity());
        this.geofenceHelper = new GeofenceHelper(getActivity());
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true); //Allows for zoom activity on map
        enableMyLocation();
        locationViewModel.getCurrentLocation().observe(this, location ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                        location.getLongitude()), ZOOM)));
        addMarkers();
    }

    private void addMarkers() {
        map.setOnMapLongClickListener(latLng -> {
            map.clear();
            geofencingClient.removeGeofences(geofenceHelper.getPendingIntent());
            MarkerOptions myMarker = new MarkerOptions();
            myMarker.position(latLng);

            myMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

            map.addMarker(myMarker);
            polylineToMarker(latLng);
        });
    }

    private void polylineToMarker(LatLng destination) {
        if (destination != null) {
            locationViewModel.getCurrentLocation().observe(this, location -> {
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
        mapViewModel.runTask(elevationURL, map);
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

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
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
                    Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }


}