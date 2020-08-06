package com.example.elevate.directions;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class DirectionsTaskParser implements Callable<List<Map<String, Integer>>> {

    private final String input;

    public DirectionsTaskParser(String input) {
        this.input = input;
    }

    @Override
    public List<Map<String,Integer>> call() {
        JSONObject myObject;
        List<Map<String,Integer>> routes = null;
        try {
            myObject = new JSONObject(input);
            DirectionsParser parser = new DirectionsParser();
            routes = parser.parse(myObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return routes;
    }

}
