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

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity implements
	OnMapClickListener, OnMapLongClickListener {

	private GoogleMap mMap;
	private LatLng myLocation;
	private GoogleApiClient mClient;
	private Location mCurrentLocation;
	private String query;
	private HashMap<String,String> data;
	private HashMap<String, EventInfo> eventMarkerMap;
	private String eventId;
	private EventInfo event;
	private int numEvents;
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private static final String LOG_TAG = "MapMyMeetups";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";
    private static final String PLACES_API_KEY = APIKeys.GOOGLE_PLACES;

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

		return true;
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
			
			// Check if we were successful in obtaining the map
			if (mMap != null) {
				// The map is verified. It is now safe to manipulate the map.
				mMap.setMyLocationEnabled(true);
				mMap.setOnMapClickListener(this);
		        mMap.setOnMapLongClickListener(this);
		        
		        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();

                Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                if (location != null)
                {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), 13));

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                    .zoom(12)                   // Sets the zoom
                    //.bearing(90)                // Sets the orientation of the camera to east
                    //.tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                }
		        			    
			}
		}
	}

	@Override
	public void onMapLongClick(LatLng point) {
		Log.i("MapMyMeetups", "onMapLongClick" + point.toString());
		myLocation = point;
		        
		query = "https://api.meetup.com/2/open_events?&sign=true"
        		+ "&lon=" + myLocation.longitude 
        		+ "&lat=" + myLocation.latitude
        		+ "&time=" + System.currentTimeMillis()
        		+ "," + (System.currentTimeMillis() + 86400000)
        		+ "&radius=10&page=20"
        		+ "&key=" + APIKeys.MEETUP
        		+ "&format=json";
			
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
			data = getMeetups(jq);
		} catch (JSONException e) {
			e.printStackTrace();
		} 
		
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, h:mm a");
		
		eventMarkerMap = new HashMap<String, EventInfo>();
		
		for( int i = 0; i < numEvents; i++ ) {
			
			Log.i("MapMyMeetups", "event_" + i + "_lat: " + data.get("event_" + i + "_lat"));
			Log.i("MapMyMeetups", "event_" + i + "_lon: " + data.get("event_" + i + "_lon"));
			
			event = new EventInfo(
					new LatLng((Float.parseFloat(data.get("event_" + i + "_lat"))), 
					 		    Float.parseFloat(data.get("event_" + i + "_lon"))),
					data.get("event_" + i + "_name"),
					new Date(Long.parseLong(data.get("event_" + i + "_time"))),
					data.get("event_" + i + "_url"));
			
			eventMarkerMap.put(data.get("event_" + i + "_id"), event);
					 		    		
			mMap.addMarker(new MarkerOptions()
			.position(event.getLatLng())
			.title(sdf.format(event.getDate()) + " : " + event.getTitle() )
			.snippet(event.getUrl())
			.icon(BitmapDescriptorFactory
					.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
			.alpha(0.7f));
			
			Log.i("MapMyMeetups", data.get("event_" + i + "_url"));
			
			eventId = "event_" + i + "_id";
			

			//TODO figure out how to handle an info window click.
			// need to get URL of the event and pass it to click listener
			// to trigger URL intent.
			mMap.setOnInfoWindowClickListener(
		      		  new OnInfoWindowClickListener(){
		      		    public void onInfoWindowClick(Marker marker){
		      		    	Intent intent = new Intent(Intent.ACTION_VIEW, 
		      		    				   Uri.parse(marker.getSnippet()));
		      		    	startActivity(intent);
		      		    }
		      		  }
		      		);
		}
		
	}

	@Override
	public void onMapClick(LatLng point) {
		Log.i("MapMyMeetups", "onMapClick" + point.toString());
		
	}
	
	public void onMarkerClick(Marker marker) {
		
	}
	
	public HashMap<String,String> getMeetups(String q) throws JSONException {
		
		HashMap<String, String> data = new HashMap<String, String>();
		
		JSONObject objMain = new JSONObject(q);
		JSONArray results = objMain.getJSONArray("results");
		
		numEvents = results.length();
		
		for( int i = 0; i < numEvents; i++ ){
			if( results.getJSONObject(i).has("venue") ) {
				data.put("event_" + i + "_lon", results.getJSONObject(i).getJSONObject("venue").getString("lon"));
				data.put("event_" + i + "_lat", results.getJSONObject(i).getJSONObject("venue").getString("lat"));
			}
			else {
				data.put("event_" + i + "_lon", results.getJSONObject(i).getJSONObject("group").getString("group_lon"));
				data.put("event_" + i + "_lat", results.getJSONObject(i).getJSONObject("group").getString("group_lat"));
			}
			data.put("event_" + i + "_time", results.getJSONObject(i).getString("time"));
			data.put("event_" + i + "_url", results.getJSONObject(i).getString("event_url"));

            if (results.getJSONObject(i).has("description")) {
                data.put("event_" + i + "_desc", results.getJSONObject(i).getString("description"));
            } else {
                data.put("event_" + i + "_desc", "");
            }

			data.put("event_" + i + "_name", results.getJSONObject(i).getString("name"));
			data.put("event_" + i + "_group", results.getJSONObject(i).getJSONObject("group").getString("name"));
			data.put("event_" + i + "_id", results.getJSONObject(i).getString("id"));
			
			Log.i("MapMyMeetups", "event_" + i + "_lon: " + data.get("event_" + i + "_lon"));
			Log.i("MapMyMeetups", "event_" + i + "_lat: " + data.get("event_" + i + "_lat"));
			Log.i("MapMyMeetups", "event_" + i + "_time: " + data.get("event_" + i + "_time"));
			Log.i("MapMyMeetups", "event_" + i + "_url: " + data.get("event_" + i + "_url"));
			Log.i("MapMyMeetups", "event_" + i + "_desc:" + data.get("event_" + i + "_desc"));
			Log.i("MapMyMeetups", "event_" + i + "_name: " + data.get("event_" + i + "_name"));
			Log.i("MapMyMeetups", "event_" + i + "_group: " + data.get("event_" + i + "_group"));
			Log.i("MapMyMeetups", "event_" + i + "_id " + data.get("event_" + i + "_id"));
		}
		
		return data;

	}

    public String[] getCategories() throws JSONException {
        
        String q = "https://api.meetup.com/2/categories"
                        + "?key=" + APIKeys.MEETUP
                        + "&sign=true"
                        + "&format=json";

        Log.i("MapMyMeetups", "query: " + q);

        String jq = null;

        try {
            jq = new JSONReaderTask().execute(q).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        Log.i("MapMyMeetups", "categories: " + jq);

        JSONArray results = new JSONObject(jq).getJSONArray("results");

        int numCategories = results.length();
        String[] categories = new String[numCategories];

        for (int i = 0; i < numCategories; i++){
            categories[i] = results.getJSONObject(i).getString("name");
        }
        
        return categories;
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
