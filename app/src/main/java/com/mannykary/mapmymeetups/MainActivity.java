package com.mannykary.mapmymeetups;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import java.util.Calendar;
import java.util.GregorianCalendar;
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
    
    private String selectedDate;
    private int selectedRadius = 5;
    private float zoomSetting = 13;
    private LatLng cameraPosition = null;
    
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
                    if (data.hasExtra("date")) {
                        selectedDate = data.getStringExtra("date");
                    }
                    if (data.hasExtra("radius")) {
                        selectedRadius = data.getIntExtra("radius", 10);
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
                    if (data.hasExtra("date")) {
                        selectedDate = data.getStringExtra("date");
                    }
                    if (data.hasExtra("radius")) {
                        selectedRadius = data.getIntExtra("radius", 10);
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
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        zoomSetting = mMap.getCameraPosition().zoom;
        cameraPosition = mMap.getCameraPosition().target;
        Log.i("zoomSetting", Float.toString(zoomSetting));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition, zoomSetting));

    }
	
	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the map
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            setUpMap();
		}
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

            /*if (cameraPosition != null /) {
                Log.i("cameraPosition", cameraPosition.toString());
                Log.i("zoomSetting", Float.toString(zoomSetting));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition, zoomSetting));
                //addMarkers(currentLocation);
            } else */
            if (currentLocation != null && 
                currentLocation.longitude != location.getLongitude() &&
                currentLocation.latitude != location.getLatitude()) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoomSetting));
                addMarkers(currentLocation);
            } else if (location != null) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoomSetting));
                addMarkers(currentLocation);
            }
        }
        
    }

	@Override
	public void onMapLongClick(LatLng point) {
        addMarkers(point);
	}
    
    public HashMap<String, String> getStartAndEndTime() {
        String startTime = Long.toString(System.currentTimeMillis());
        String endTime = "1d";
        if (selectedDate != null) {
            Log.i("Selected date", selectedDate);
            if (selectedDate.equals("today")) {
                Calendar date = new GregorianCalendar();
                date.set(Calendar.HOUR_OF_DAY, 23);
                date.set(Calendar.MINUTE, 59);
                endTime = Long.toString(date.getTimeInMillis());
            } else if (selectedDate.equals("tomorrow")) {
                Calendar date = new GregorianCalendar();
                date.set(Calendar.HOUR_OF_DAY, 0);
                date.set(Calendar.MINUTE, 0);
                date.set(Calendar.SECOND, 0);
                date.set(Calendar.MILLISECOND, 0);
                date.add(Calendar.DAY_OF_MONTH, 1);
                startTime = Long.toString(date.getTimeInMillis());
                date.set(Calendar.HOUR_OF_DAY, 23);
                date.set(Calendar.MINUTE, 59);
                endTime = Long.toString(date.getTimeInMillis());
            } else if (selectedDate.equals("this week")) {
                Calendar date = new GregorianCalendar();
                date.set(Calendar.HOUR_OF_DAY, 0);
                int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
                int numberOfDaysTillEndOfWeek = 8 - dayOfWeek;
                endTime = numberOfDaysTillEndOfWeek + "d";
            } else if (selectedDate.equals("this weekend")) {
                Calendar date = new GregorianCalendar();
                date.set(Calendar.HOUR_OF_DAY, 0);
                date.set(Calendar.MINUTE, 0);
                date.set(Calendar.SECOND, 0);
                date.set(Calendar.MILLISECOND, 0);
                int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
                startTime = Long.toString(date.getTimeInMillis()
                        + (6 - dayOfWeek) * ONE_DAY_IN_MILLISECONDS + 60 * 60 * 1000 * 17); // 5pm Friday!
                endTime = Long.toString(date.getTimeInMillis()
                        + (9 - dayOfWeek) * ONE_DAY_IN_MILLISECONDS);
            } else if (selectedDate.equals("next week")) {
                Calendar date = new GregorianCalendar();
                date.set(Calendar.HOUR_OF_DAY, 0);
                date.set(Calendar.MINUTE, 0);
                date.set(Calendar.SECOND, 0);
                date.set(Calendar.MILLISECOND, 0);
                int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
                int numberOfDaysTillEndOfWeek = 8 - dayOfWeek;
                startTime = Long.toString(date.getTimeInMillis()
                        + numberOfDaysTillEndOfWeek * ONE_DAY_IN_MILLISECONDS);
                endTime = Long.toString(date.getTimeInMillis()
                        + (7 + numberOfDaysTillEndOfWeek) * ONE_DAY_IN_MILLISECONDS);
            } else if (selectedDate.equals("this month")) {
                Calendar date = new GregorianCalendar();
                date.set(Calendar.DATE, date.getActualMaximum(Calendar.DAY_OF_MONTH));
                date.set(Calendar.HOUR_OF_DAY, 23);
                date.set(Calendar.MINUTE, 59);
                endTime = Long.toString(date.getTimeInMillis());
            } else if (selectedDate.equals("next month")) {
                Calendar date = new GregorianCalendar();
                date.add(Calendar.MONTH, 1);
                date.set(Calendar.DATE, date.getActualMinimum(Calendar.DAY_OF_MONTH));
                date.set(Calendar.HOUR_OF_DAY, 0);
                date.set(Calendar.MINUTE, 0);
                date.set(Calendar.SECOND, 0);
                date.set(Calendar.MILLISECOND, 0);
                startTime = Long.toString(date.getTimeInMillis());
                date.set(Calendar.DATE, date.getActualMaximum(Calendar.DAY_OF_MONTH));
                date.set(Calendar.HOUR_OF_DAY, 23);
                date.set(Calendar.MINUTE, 59);
                endTime = Long.toString(date.getTimeInMillis());
            }
        }
        
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        
        return map;
    }
    
    public ArrayList<Event> getEvents(LatLng myLocation) {
        Log.i("MapMyMeetups", "onMapLongClick" + myLocation.toString());
        ArrayList<Event> events = null;

        //https://api.meetup.com/2/open_events?&sign=true&lon=-79.39143087&lat=43.70965413&radius=10&page=20&key=154cb1a653a76231d344073e2d7f16&format=json

        HashMap<String, String> startAndEndTimes = getStartAndEndTime();
        String startTime = startAndEndTimes.get("startTime");
        String endTime = startAndEndTimes.get("endTime");
        int page = 50;
        
        query = "https://api.meetup.com/2/open_events?&sign=true"
                + "&lon=" + myLocation.longitude
                + "&lat=" + myLocation.latitude
                + "&time=" + startTime
                + "," + endTime
                + "&radius=" + selectedRadius
                + "&page=" + page
                + "&key=" + APIKeys.MEETUP
                + "&format=json";

        if (selectedCategory != null && !selectedCategory.equals("all")) {
            Log.i("selectedCategory", selectedCategory);
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
