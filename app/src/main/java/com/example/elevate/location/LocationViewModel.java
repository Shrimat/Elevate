package com.example.elevate.location;

import android.location.Location;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LocationViewModel extends ViewModel {

    private MutableLiveData<Location> currentLocation;

    public LocationViewModel() {
        this.currentLocation = new MutableLiveData<>();
    }

    public MutableLiveData<Location> getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation.setValue(currentLocation);
    }
}
