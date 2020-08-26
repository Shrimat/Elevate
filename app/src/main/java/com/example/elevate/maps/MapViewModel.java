package com.example.elevate.maps;

import android.graphics.Color;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.elevate.TaskRequest;
import com.example.elevate.TaskRunner;
import com.example.elevate.elevation.ElevationTaskParser;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Cap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import java.util.ArrayList;
import java.util.List;

public class MapViewModel extends ViewModel {
    private static final double ELEVATION_ALERT_VALUE = 0.04;
    private static final int SAMPLING_DISTANCE = 75; //The distance between two consecutive points in metres
    private static final Cap CAP_TYPE = new RoundCap();
    private MutableLiveData<List<LatLng>> pointsToAlertAt;

    public MapViewModel() {
        this.pointsToAlertAt = new MutableLiveData<>();
        pointsToAlertAt.setValue(new ArrayList<>());
    }

    public void runTask(String elevationURL, GoogleMap map) {
        TaskRunner taskRunner3 = new TaskRunner();
        taskRunner3.executeAsync(new TaskRequest(elevationURL), (data3) -> {
            TaskRunner taskRunner4 = new TaskRunner();
            taskRunner4.executeAsync(new ElevationTaskParser(data3), (data4) -> {
                LatLng prev = null;
                List<LatLng> pointsToAlertList = new ArrayList<>();
                double gradient;
                boolean previousLess = true;
                for (LatLng curr : data4.keySet()) {
                    PolylineOptions polylineOptions = new PolylineOptions();
                    if (prev == null) {
                        prev = curr;
                        continue;
                    }
                    gradient = calculateGradient(data4.get(prev), data4.get(curr));
                    if (gradient > ELEVATION_ALERT_VALUE && previousLess) {
                        previousLess = false;
                        pointsToAlertList.add(prev);
                    } else if (gradient <= ELEVATION_ALERT_VALUE) {
                        previousLess = true;
                    }

                    polylineOptions.add(prev, curr)
                            .width(15)
                            .color(getColour(gradient))
                            .startCap(CAP_TYPE)
                            .endCap(CAP_TYPE)
                            .geodesic(true);

                    prev = curr;
                    map.addPolyline(polylineOptions);
                }
                pointsToAlertAt.postValue(pointsToAlertList);

            });
        });
    }

    private double calculateGradient(double start, double end) {
        return (end - start) / SAMPLING_DISTANCE;
    }

    private int getColour(double gradient) {
        if (gradient <= ELEVATION_ALERT_VALUE) {
            return Color.parseColor("#00BFFF");
        } else {
            return Color.parseColor("#DC143C");
        }

    }

    public MutableLiveData<List<LatLng>> getPointsToAlertAt() {
        return pointsToAlertAt;
    }
}
