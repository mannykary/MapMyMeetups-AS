package com.mannykary.mapmymeetups;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.Filter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private ArrayList<Place> resultList;

    private static final String LOG_TAG = "MapMyMeetups";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";
    private static final String PLACES_API_KEY = APIKeys.GOOGLE_PLACES;

    public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public String getItem(int index) {
        return resultList.get(index).description;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                // TODO Improve the delay for autocomplete.
                if (constraint != null && constraint.length() > 5 && constraint.length() % 3 == 0) {
                    // Retrieve the autocomplete results.
                    resultList = autocomplete(constraint.toString());

                    // Assign the data to the FilterResults
                    filterResults.values = resultList;
                    filterResults.count = resultList.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }};
        return filter;
    }
    
    public static ArrayList<Place> autocomplete(String input) {
        ArrayList<Place> resultList = null;
        String jq = null;
        StringBuilder query = null;

        try {
            query = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            query.append("?key=" + PLACES_API_KEY);
            query.append("&input=" + URLEncoder.encode(input, "utf8"));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            e.printStackTrace();
        }


        try {
            jq = new JSONReaderTask().execute(query.toString()).get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Log.i("json", jq);

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jq);
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");


            // Extract the Place descriptions from the results
            resultList = new ArrayList<Place>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                String description = predsJsonArray.getJSONObject(i).getString("description");
                String placeId = predsJsonArray.getJSONObject(i).getString("place_id");
                resultList.add(new Place(description, placeId));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }
    
}