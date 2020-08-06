package com.example.elevate.elevation;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class ElevationTaskParser implements Callable<Map<LatLng, Double>> {

    private final String input;

    public ElevationTaskParser(String input) {
        this.input = input;
    }

    @Override
    public Map<LatLng, Double> call() {
        JSONObject myObject;
        Map<LatLng, Double> positionToElevation = new LinkedHashMap<>();
        try {
            myObject = new JSONObject(input);
            ElevationParser parser = new ElevationParser();
            positionToElevation = parser.parse(myObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return positionToElevation;
    }
}
