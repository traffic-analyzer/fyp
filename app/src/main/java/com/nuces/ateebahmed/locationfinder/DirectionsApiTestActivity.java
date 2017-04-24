package com.nuces.ateebahmed.locationfinder;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.GeocodedWaypoint;
import com.google.maps.model.GeocodingResult;

import java.io.IOException;

public class DirectionsApiTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directions_api_test);


        GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBfqbjPyZZacjq5XuJVUw_3RgXbehyJK0c");
        try {
            //Geocoding example
            /*GeocodingResult[] results = GeocodingApi.reverseGeocode(context, new com.google.maps.model.LatLng(24.747001, 66.640322)).await();
            for (GeocodingResult r: results)
                Log.i("DirectionsApiTest", r.formattedAddress + " " + results.length);*/
            DirectionsResult result = DirectionsApi.getDirections(context, "Karachi, PK", "Hyderabad, PK").await();
            /*for (GeocodedWaypoint g: result.geocodedWaypoints)
                Log.i("DirectionsApiTest", g.placeId);*/
            for (DirectionsRoute r: result.routes)
                for (int i = 0; i < r.overviewPolyline.decodePath().size(); i++)
                Log.i("DirectionsApiTest", r.overviewPolyline.decodePath().get(i++).toString());
        } catch (ApiException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}