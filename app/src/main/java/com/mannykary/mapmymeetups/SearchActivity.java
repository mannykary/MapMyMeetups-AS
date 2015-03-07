package com.mannykary.mapmymeetups;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;


public class SearchActivity extends ListActivity implements OnItemClickListener {

    private String[] categories;
    private HashMap<String, String> categoriesMap = new HashMap<String, String>();
    private EditText searchEditText;
    private AutoCompleteTextView locationEditText;
    
    private Spinner spinnerDate;
    private Spinner spinnerRadius;
    
    public String reverseGeocode(LatLng l) {
        String query = "https://maps.googleapis.com/maps/api/geocode/json?"
            + "latlng=" + l.latitude + "," + l.longitude
            + "&key=" + APIKeys.GOOGLE_PLACES;
        
        String response = null;

        try {
            response = new JSONReaderTask().execute(query).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        String address = null;
        try {
            JSONObject mainObj = new JSONObject(response);
            JSONArray results = mainObj.getJSONArray("results");
            address = results.getJSONObject(0).getString("formatted_address");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return address;
    }
    
    public LatLng geocode(String a) {
        String query = "https://maps.googleapis.com/maps/api/geocode/json?"
            + "address=" + Uri.encode(a)
            + "&key=" + APIKeys.GOOGLE_PLACES;

        String response = null;

        try {
            response = new JSONReaderTask().execute(query).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        Double lat = 0d, lng = 0d;
        try {
            JSONObject mainObj = new JSONObject(response);
            JSONArray results = mainObj.getJSONArray("results");
            JSONObject location = results.getJSONObject(0)
                                         .getJSONObject("geometry")
                                         .getJSONObject("location");
            lat = location.getDouble("lat");
            lng = location.getDouble("lng");
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new LatLng(lat, lng);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        LatLng latlng = (LatLng) getIntent().getExtras().get("location");
        final String address = reverseGeocode(latlng);

        locationEditText = (AutoCompleteTextView) findViewById(R.id.editText_location);
        locationEditText.setText(address);
        locationEditText.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));
        locationEditText.setOnItemClickListener(this);

        searchEditText = (EditText) findViewById(R.id.editText_search);
        searchEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView tv, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    Intent i = new Intent();
                    i.putExtra("searchQuery", tv.getText().toString());
                    LatLng location = geocode(locationEditText.getText().toString());
                    i.putExtra("location", location);
                    setResult(MainActivity.RESULT_OK_SEARCH, i);
                    i.putExtra("date", spinnerDate.getSelectedItem().toString());
                    i.putExtra("radius", getRadiusInInt(spinnerRadius.getSelectedItem().toString()));
                    finish();

                    return true;
                }
                return false;
            }
        });

        setUpDateSpinner();
        setUpRadiusSpinner();
        setUpCategoriesList();

    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        String str = (String) adapterView.getItemAtPosition(position);
        searchEditText.requestFocus();
        if (searchEditText.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        
        //LatLng latlng = geocode(str);
        //Toast.makeText(this, latlng.toString(), Toast.LENGTH_SHORT).show();
    }
    
    
    
    public void setUpCategoriesList() {
        try {
            categories = getCategories();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                categories));
        
    }
    
    public void setUpDateSpinner() {
        spinnerDate = (Spinner) findViewById(R.id.spinner_date);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.dates_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDate.setAdapter(adapter);
        
    }
    
    public void setUpRadiusSpinner() {
        spinnerRadius = (Spinner) findViewById(R.id.spinner_radius);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.distances_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRadius.setAdapter(adapter);
    }
    
    public int getRadiusInInt(String s) {
        return Integer.parseInt(s.split(" ")[0]);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        String selectedCategory = categories[position];
        Intent i = new Intent();
        i.putExtra("selectedCategory", categoriesMap.get(selectedCategory));
        LatLng location = geocode(locationEditText.getText().toString());
        i.putExtra("location", location);
        setResult(MainActivity.RESULT_OK_CATEGORY, i);
        i.putExtra("date", spinnerDate.getSelectedItem().toString());
        i.putExtra("radius", getRadiusInInt(spinnerRadius.getSelectedItem().toString()));
        finish();
    }

    public String[] getCategories() throws JSONException {

        String q = "https://api.meetup.com/2/categories"
                + "?key=" + APIKeys.MEETUP
                + "&sign=true"
                + "&format=json";

        String jq = null;

        try {
            jq = new JSONReaderTask().execute(q).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        JSONArray results = new JSONObject(jq).getJSONArray("results");

        int numCategories = results.length();
        String[] categories = new String[numCategories+1];
        
        categories[0] = "All categories";
        categoriesMap.put("All categories", "all");

        for (int i = 1; i < numCategories; i++){
            String categoryId = results.getJSONObject(i).getString("id");
            String categoryName = results.getJSONObject(i).getString("name");
            categories[i] = categoryName;
            categoriesMap.put(categoryName, categoryId);
        }

        return categories;
    }
}
