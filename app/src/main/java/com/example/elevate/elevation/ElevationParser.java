package com.example.elevate.elevation;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class ElevationParser {

    public Map<LatLng, Double> parse(JSONObject jObject) {

        Map<LatLng, Double> positionToElevation = new LinkedHashMap<>();
        JSONArray jResults;

        try {
            jResults = jObject.getJSONArray("results");

            for (int i = 0; i < jResults.length(); i++) {
                double lat = (Double) ((JSONObject) ((JSONObject) jResults.get(i)).get("location")).get("lat");
                double lng = (Double) ((JSONObject) ((JSONObject) jResults.get(i)).get("location")).get("lng");
                LatLng currentPosition = new LatLng(lat, lng);
                double elevation = (Double) ((JSONObject) jResults.get(i)).get("elevation");
                positionToElevation.put(currentPosition, elevation);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return positionToElevation;
    }
}
