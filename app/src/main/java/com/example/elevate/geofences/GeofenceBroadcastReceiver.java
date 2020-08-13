package com.example.elevate.geofences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import com.example.elevate.MapsActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = GeofenceBroadcastReceiver.class.getName();
    private MapsActivity main = null;

    public void setMainActivityHandler(MapsActivity main){
        this.main = main;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        TextToSpeech mTTS;
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
//        mTTS = new TextToSpeech(context, status -> {
//            if (status == TextToSpeech.SUCCESS) {
//                int result = mTTS.setLanguage(Locale.ENGLISH);
//                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                    Log.e("TTS", "Language not supported");
//                }
//            } else {
//                Log.e("TTS", "Initialisation failed");
//            }
//        });

        if (geofencingEvent.hasError()) {
            Log.d(TAG, "onReceive: Error receiving geofence event");
            return;
        }

        List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
        for (Geofence geofence : geofenceList) {
            Log.d(TAG, "onReceive: " + geofence.getRequestId());
        }
        int transitionType = geofencingEvent.getGeofenceTransition();

        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Toast.makeText(context, "GEOFENCE_TRANSITION_ENTER", Toast.LENGTH_SHORT).show();
//                mTTS.setPitch(1.0f);
//                mTTS.setSpeechRate(1.0f);
//                mTTS.speak("In 100 metres elevation reaches blah", TextToSpeech.QUEUE_FLUSH, null);
//                mTTS.stop();
//                mTTS.shutdown();
//                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//// Vibrate for 500 milliseconds
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
//                } else {
//                    //deprecated in API 26
//                    v.vibrate(500);
//                }

                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT :
                Toast.makeText(context, "GEOFENCE_TRANSITION_EXIT", Toast.LENGTH_SHORT).show();
                List<Geofence> triggeredGeofences = geofencingEvent.getTriggeringGeofences();
                List<String> toRemove = new ArrayList<>();
                for (Geofence geofence : triggeredGeofences) {
                    toRemove.add(geofence.getRequestId());
                }
                //main.transitionRemoveGeofence(toRemove.get(0));
                main.transitionRemoveGeofence("ID");
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL :
                Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_SHORT).show();
                break;
        }
    }




}
