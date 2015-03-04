package com.mannykary.mapmymeetups;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity implements
	OnMapClickListener, OnMapLongClickListener {

	private GoogleMap mMap;
    private LatLng currentLocation = null;
	private String query;
    
    private static final int GET_SEARCH_REQUEST_CODE = 1;
    private static final int ONE_DAY_IN_MILLISECONDS = 60*60*24*1000;
    
    public static final int RESULT_OK_CATEGORY = 1;
    public static final int RESULT_OK_SEARCH = 2;

    private String selectedCategory = null;
    private String searchQuery = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setUpMapIfNeeded();
        
	}

	
//    @Override
//    protected void onStart() {
//        super.onStart();
//        // Connect the client.
//        mLocationClient.connect();
//    }
//    
//    @Override
//    protected void onStop() {
//        // Disconnecting the client invalidates it.
//        mLocationClient.disconnect();
//        super.onStop();
//    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:
                Intent i = new Intent(this, SearchActivity.class);
                i.putExtra("location", currentLocation);
                startActivityForResult(i, GET_SEARCH_REQUEST_CODE);
                return true;
            case R.id.action_settings:
                //openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_SEARCH_REQUEST_CODE) {
            switch(resultCode) {
                case RESULT_OK_CATEGORY:
                    if (data.hasExtra("selectedCategory")) {
                        selectedCategory = data.getStringExtra("selectedCategory");
                        searchQuery = null;
                    }
                    if (data.hasExtra("location")) {
                        currentLocation = (LatLng) data.getExtras().get("location");
                    }
                    break;
                case RESULT_OK_SEARCH:
                    if (data.hasExtra("searchQuery")) {
                        searchQuery = Uri.encode(data.getStringExtra("searchQuery"));
                        selectedCategory = null;
                    }
                    if (data.hasExtra("location")) {
                        currentLocation = (LatLng) data.getExtras().get("location");
                    }
                    break;
            }
            if (resultCode == RESULT_CANCELED) {
                Log.i("MainActivity", "no extra returned.");
            }
            addMarkers(currentLocation);
        }
    }
	
	@Override
	public void onResume() {
		super.onResume();
		// Check if device has the Google Play Services APK
		GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		setUpMapIfNeeded();
	}
	
	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the map
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		}
        setUpMap();
	}
    
    public Location getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        return locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
    }
    
    public void setUpMap() {
        // Check if we were successful in obtaining the map
        if (mMap != null) {
            // The map is verified. It is now safe to manipulate the map.
            mMap.setMyLocationEnabled(true);
            mMap.setOnMapClickListener(this);
            mMap.setOnMapLongClickListener(this);

            Location location = getCurrentLocation();

            if (currentLocation != null && 
                currentLocation.longitude != location.getLongitude() &&
                currentLocation.latitude != location.getLatitude()) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 11));
                addMarkers(currentLocation);
            } else if (location != null) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 11));
                addMarkers(currentLocation);
            }
        }
        
    }

	@Override
	public void onMapLongClick(LatLng point) {
        addMarkers(point);
	}
    
    public ArrayList<Event> getEvents(LatLng myLocation) {
        Log.i("MapMyMeetups", "onMapLongClick" + myLocation.toString());
        ArrayList<Event> events = null;

        //https://api.meetup.com/2/open_events?&sign=true&lon=-79.39143087&lat=43.70965413&radius=10&page=20&key=154cb1a653a76231d344073e2d7f16&format=json
        
        long currentTime = System.currentTimeMillis();
        int radius = 10;
        int page = 100;
        
        query = "https://api.meetup.com/2/open_events?&sign=true"
                + "&lon=" + myLocation.longitude
                + "&lat=" + myLocation.latitude
                + "&time=" + System.currentTimeMillis()
                + "," + (System.currentTimeMillis() + ONE_DAY_IN_MILLISECONDS)
                + "&radius=" + radius
                + "&page=" + page
                + "&key=" + APIKeys.MEETUP
                + "&format=json";

        if (selectedCategory != null) {
            query += "&category=" + selectedCategory;
        } else if (searchQuery != null) {
            query += "&text=" + searchQuery;
        }

        Log.i("MapMyMeetups", "query: " + query);

        String jq = null;

        try {
            jq = new JSONReaderTask().execute(query).get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Log.i("MapMyMeetups", "json: " + jq);

        try {
            events = getMeetups(jq);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return events;
    }

    public void addMarkers(LatLng myLocation) {
        mMap.clear();

        ArrayList<Event> events = getEvents(myLocation);
        
        HashMap<String, Event> markerMap = new HashMap<String, Event>();

        for (Event e : events) {
            
            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(e.latLng)
                    .title(e.name)
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .alpha(0.7f));
            
            markerMap.put(m.getId(), e);
        }
        
        final HashMap<String, Event> markerMapFinal = markerMap;
        
        mMap.setOnInfoWindowClickListener(
                new GoogleMap.OnInfoWindowClickListener(){
                    public void onInfoWindowClick(Marker marker){
                        Uri url = Uri.parse(markerMapFinal.get(marker.getId()).url);
                        Intent intent = new Intent(Intent.ACTION_VIEW, url);
                        startActivity(intent);
                    }
                }
        );

        //mMap.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater()));
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View v = getLayoutInflater().inflate(R.layout.popup, null);

                TextView groupName = (TextView) v.findViewById(R.id.event_group);
                TextView title = (TextView) v.findViewById(R.id.event_title);
                TextView date = (TextView) v.findViewById(R.id.event_date);

                SimpleDateFormat sdfDate = new SimpleDateFormat("EEEE, MMM d, yyyy");
                SimpleDateFormat sdfTime = new SimpleDateFormat("h:mm a");
                
                Event e = markerMapFinal.get(marker.getId());
                
                groupName.setText(e.groupName);
                title.setText(e.name);
                date.setText(sdfTime.format(e.date) + " on " + sdfDate.format(e.date));
                return v;
            }
        });
        
    }
    
	@Override
	public void onMapClick(LatLng point) {
		Log.i("MapMyMeetups", "onMapClick" + point.toString());
		
	}
	
	public ArrayList<Event> getMeetups(String q) throws JSONException {
		
		ArrayList<Event> events = new ArrayList<Event>();
		
		JSONObject objMain = new JSONObject(q);
		JSONArray results = objMain.getJSONArray("results");
		
		int numEvents = results.length();
		
		for (int i = 0; i < numEvents; i++) {
            Event e = new Event();
            Double lat, lon;
            
			if (results.getJSONObject(i).has("venue")) {
                lat = results.getJSONObject(i).getJSONObject("venue").getDouble("lat");
                lon = results.getJSONObject(i).getJSONObject("venue").getDouble("lon");
			} else {
                lat = results.getJSONObject(i).getJSONObject("group").getDouble("group_lat");
                lon = results.getJSONObject(i).getJSONObject("group").getDouble("group_lon");
            }
            e.latLng = new LatLng(lat, lon);
            e.date = new Date(Long.parseLong(results.getJSONObject(i).getString("time")));
            e.url = results.getJSONObject(i).getString("event_url");

            if (results.getJSONObject(i).has("description")) {
                e.desc = results.getJSONObject(i).getString("description");
            } else {
                e.desc = "";
            }
            
            e.name = results.getJSONObject(i).getString("name");
            e.groupName = results.getJSONObject(i).getJSONObject("group").getString("name");
            e.id = results.getJSONObject(i).getString("id");
            
            events.add(e);
		}
		
		return events;

	}

}
