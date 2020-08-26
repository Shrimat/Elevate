package com.example.elevate.location;

import android.location.Location;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LocationViewModel extends ViewModel implements LocationManager.locationSuccessListener {

    private MutableLiveData<Location> currentLocation;

    public LocationViewModel() {
        this.currentLocation = new MutableLiveData<>();
    }

    @Override
    public void onLocationReceived() {
        currentLocation.setValue(LocationManager.getInstance().getMyCurrentLocation());
        if (currentLocation == null){
            //Toast.makeText(getActivity(), "unable to get location", Toast.LENGTH_SHORT).show();
        }
    }

    public MutableLiveData<Location> getCurrentLocation() {
        return currentLocation;
    }
}
