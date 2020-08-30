package com.example.elevate.maps;


import android.Manifest;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.elevate.BuildConfig;
import com.example.elevate.MainActivity;
import com.example.elevate.R;
import com.example.elevate.TaskRequest;
import com.example.elevate.TaskRunner;
import com.example.elevate.directions.DirectionsTaskParser;
import com.example.elevate.geofences.GeofenceHelper;
import com.example.elevate.location.LocationViewModel;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private View view;
    private MapView mapView;
    private LocationViewModel locationViewModel;
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private int currentID;
    private MapViewModel mapViewModel;

    private static final int REQUEST_LOCATION_PERMISSION = 1000;
    private static final int ZOOM = 15;
    private static final String TAG = "MapsFragment";
    private static final int GEOFENCE_RADIUS = 150;
    private static final int SAMPLING_DISTANCE = 75;
    private static final int MAP_PADDING = 100;
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String ELEVATION_URL = "https://maps.googleapis.com/maps/api/elevation/";
    private static final String JSON_OUTPUT_FORMAT = "json";
    private static final String API_KEY = BuildConfig.CONSUMER_KEY;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_map_view, container, false);

        FragmentManager manager = getParentFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        SupportMapFragment fragment = new SupportMapFragment();
        transaction.add(R.id.mapView, fragment);
        transaction.commit();

        fragment.getMapAsync(this);
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setCountries("GB");
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(place.getLatLng());
                locationViewModel.getCurrentLocation().observe(requireActivity(), location ->
                        builder.include(new LatLng(location.getLatitude(), location.getLongitude())));
                LatLngBounds bounds = builder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING);
                map.animateCamera(cu);
                addMarker(place.getLatLng());
                polylineToMarker(place.getLatLng());
            }


            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        this.locationViewModel = ((MainActivity) requireActivity()).getLocationViewModel();
        this.mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        this.geofencingClient = LocationServices.getGeofencingClient(requireActivity());
        this.geofenceHelper = new GeofenceHelper(getActivity());

        mapViewModel.getPointsToAlertAt().observe(requireActivity(), latLngs -> {
            for (LatLng position : latLngs) {
                addGeofence(position, GEOFENCE_RADIUS, "ID"+currentID);
                currentID++;
            }
        });
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(requireContext());
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        enableMyLocation();
        locationViewModel.getCurrentLocation().observe(this, location ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                        location.getLongitude()), ZOOM)));
        View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.setMargins(0, 0, 30, 30);
        longClick();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView  = (MapView) this.view.findViewById(R.id.mapView);
        if (mapView != null) {
            mapView.onCreate(null);
            mapView.onResume();
            mapView.getMapAsync(this);
        }
    }

    private void longClick() {
        map.setOnMapLongClickListener(latLng -> {
            map.clear();
            geofencingClient.removeGeofences(geofenceHelper.getPendingIntent());
            addMarker(latLng);
            polylineToMarker(latLng);
        });
    }

    private void addMarker(LatLng latLng) {
        MarkerOptions myMarker = new MarkerOptions();
        myMarker.position(latLng);

        myMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        map.addMarker(myMarker);
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

    //================================================================================
    // Location functions
    //================================================================================

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
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

    //================================================================================
    // Geofence functions
    //================================================================================

    private void addGeofence(LatLng centre, float radius, String ID) {
        Geofence geofence = geofenceHelper.getGeofence(ID, centre, radius,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}