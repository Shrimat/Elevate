package com.example.elevate.directions;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectionsParser {
    /**
     * Returns a list of lists containing latitude and longitude from a JSONObject
     */
    public List<Map<String,Integer>> parse(JSONObject jObject) {

        List<Map<String,Integer>> routes = new ArrayList<>();
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;

        try {

            jRoutes = jObject.getJSONArray("routes");

            // Loop for all routes
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                Map<String,Integer> encodedPaths = new HashMap<>();

                //Loop for all legs
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    //Loop for all steps
                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        //Obtaining distance of segment to the nearest metre
                        Integer distance = (Integer) ((JSONObject) ((JSONObject) jSteps.get(k)).get("distance")).get("value");
                        encodedPaths.put(polyline, distance);
                    }
                    routes.add(encodedPaths);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return routes;
    }

}
