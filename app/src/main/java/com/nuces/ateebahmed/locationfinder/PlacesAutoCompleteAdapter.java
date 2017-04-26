package com.nuces.ateebahmed.locationfinder;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.DataBufferUtils;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.location.places.AutocompleteFilter.TYPE_FILTER_GEOCODE;

/**
 * Created by progamer on 25/04/17.
 */

public class PlacesAutoCompleteAdapter extends ArrayAdapter implements Filterable {

    private ArrayList<AutocompletePrediction> places;
    private GoogleApiClient googleApiClient;

    public PlacesAutoCompleteAdapter(@NonNull Context context, GoogleApiClient client) {
        super(context, android.R.layout.simple_expandable_list_item_2, android.R.id.text1);
        googleApiClient = client;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults results = new FilterResults();
                ArrayList<AutocompletePrediction> data = new ArrayList<>();
                if (charSequence != null) {
                    // use this data in getPlaceById to get coordinates
                    data = getLocations(charSequence);
                }
                // for getting LatLng
                    /*Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId)
                            .setResultCallback(new ResultCallback<PlaceBuffer>() {
                                @Override
                                public void onResult(PlaceBuffer places) {
                                    if (places.getStatus().isSuccess() && places.getCount() > 0) {
                                        final Place myPlace = places.get(0);
                                        Log.i(TAG, "Place found: " + myPlace.getName());
                                    } else {
                                        Log.e(TAG, "Place not found");
                                    }
                                    places.release();
                                }
                            });*/
                results.values = data;
                if (data != null)
                    results.count = data.size();
                else results.count = 0;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                if (filterResults != null && filterResults.count > 0) {
                    places = (ArrayList<AutocompletePrediction>) filterResults.values;
                    notifyDataSetChanged();
                } else notifyDataSetInvalidated();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                if (resultValue instanceof AutocompletePrediction)
                    return ((AutocompletePrediction) resultValue).getFullText(null);
                return super.convertResultToString(resultValue);
            }
        };
    }

    @Nullable
    @Override
    public AutocompletePrediction getItem(int position) {
        return places.get(position);
    }

    @Override
    public int getCount() {
        return places.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View row = super.getView(position, convertView, parent);

        AutocompletePrediction item = getItem(position);
        TextView t1 = (TextView) row.findViewById(android.R.id.text1);
        t1.setText(item.getPrimaryText(new StyleSpan(Typeface.BOLD)));
        return row;
    }

    private ArrayList<AutocompletePrediction> getLocations(CharSequence query) {
        if (!googleApiClient.isConnected())
            return null;
        AutocompletePredictionBuffer results = Places.GeoDataApi
                .getAutocompletePredictions(googleApiClient, "Karachi" + query.toString(),
                        LocationComponentsSingleton.BOUNDS, getCityFilter())
                .await(60, TimeUnit.SECONDS);
        if (!results.getStatus().isSuccess()) {
            results.release();
            return null;
        }
        return DataBufferUtils.freezeAndClose(results);
    }

    private AutocompleteFilter getCityFilter() {
        return new AutocompleteFilter.Builder().setTypeFilter(TYPE_FILTER_GEOCODE).build();
    }
}
