package com.nuces.ateebahmed.locationfinder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataBufferUtils;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.GeoDataApi;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.location.places.AutocompleteFilter.TYPE_FILTER_CITIES;
import static com.google.android.gms.location.places.AutocompleteFilter.TYPE_FILTER_GEOCODE;

public class SearchPlacesActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "SearchPlacesActivity";
    private AppCompatAutoCompleteTextView txtSrchPlc;
    private GoogleApiClient placesClient;
    private LatLngBounds bounds;
    private PlacesAutoCompleteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_places);

        placesClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        placesClient.connect();

        txtSrchPlc = (AppCompatAutoCompleteTextView) findViewById(R.id.txtSrchPlc);

        adapter = new PlacesAutoCompleteAdapter(this);

        txtSrchPlc.setAdapter(adapter);

        bounds = new LatLngBounds(new LatLng(24.747001, 66.640322),
                new LatLng(25.249834, 67.456057));
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        placesClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, connectionResult.getErrorMessage());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, requestCode + " " + resultCode + " " + data.toString());
    }

    private class PlacesAutoCompleteAdapter extends ArrayAdapter<AutocompletePrediction> implements
            Filterable {

        private ArrayList<AutocompletePrediction> places;

        public PlacesAutoCompleteAdapter(@NonNull Context context) {
            super(context, android.R.layout.simple_expandable_list_item_2, android.R.id.text1);
        }

        @Override
        public int getCount() {
            return places.size();
        }

        @Nullable
        @Override
        public AutocompletePrediction getItem(int position) {
            return places.get(position);
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

        private ArrayList<AutocompletePrediction> getLocations(CharSequence query) {
            if (!placesClient.isConnected())
                return null;
            AutocompletePredictionBuffer results = Places.GeoDataApi
                    .getAutocompletePredictions(placesClient, "Karachi" + query.toString(),
                            bounds, getCityFilter()).await(60, TimeUnit.SECONDS);
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
}
