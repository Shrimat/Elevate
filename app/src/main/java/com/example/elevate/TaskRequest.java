package com.example.elevate;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

public class TaskRequest implements Callable<String> {

    private final String input;

    public TaskRequest(String input) {
        this.input = input;
    }

    public static String requestDirection(String request) throws IOException {
        String response = "";
        InputStream myStream = null;
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(request);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            myStream = httpURLConnection.getInputStream();
            inputStreamReader = new InputStreamReader(myStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder myBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                myBuilder.append(line);
            }
            response = myBuilder.toString();
            inputStreamReader.close();
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myStream != null) {
                myStream.close();
            }
            assert httpURLConnection != null;
            httpURLConnection.disconnect();
        }
        return response;
    }

    @Override
    public String call() {
        String response = "";
        try {
            response = requestDirection(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}
