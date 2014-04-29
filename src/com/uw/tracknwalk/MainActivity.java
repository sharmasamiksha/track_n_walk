package com.uw.tracknwalk;

import java.util.ArrayList;
import java.util.List;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.uw.tracknwalk.R;
import com.uw.tracknwalk.model.DirectionResponse;
import com.uw.tracknwalk.model.Leg;
import com.uw.tracknwalk.model.Step;
import com.uw.tracknwalk.utils.GsonRequest;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
//******************************************************************************
//Track N Walk: Plots walking routes using google maps by taking start and end location from user. 
//Samiksha Sharma
//-----------------------------------------------------------------------------
//Date     Notes                                                       who
//======== ==========================================================  ===
//03/07/14	Added Google Maps API to load maps						  Samiksha
//03/08/14 Added UI to input start and end locations 				  Samiksha
// and used Volley and Gson libraries to communicate with
// Google directions API.
//03/09/14 Added Polyline to display path 							  Samiksha
// and markers to display start and end location in map							              
//******************************************************************************
public class MainActivity extends Activity implements LocationListener {

	private static String DIRECTIONS_API_KEY = "AIzaSyCbSx6jN8UPsff71vp_cacXarnPXY2P8sI";
	private static String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json?origin=%s" 
			+ "&destination=%s&sensor=false&mode=walking&key=" + DIRECTIONS_API_KEY;
	
	LocationManager locationManager;
	GoogleMap mMap;
	RequestQueue requestQueue;//
	DirectionResponse directionResponse;
	Location lastKnownLocation;
	
	EditText etStart;
	EditText etDestination;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		repositionMyLocationButton();
		
		getActionBar().hide();
		
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap(); // way to instantiate GoogleMap object.
		mMap.setBuildingsEnabled(true);
		mMap.setIndoorEnabled(true);
		mMap.setMyLocationEnabled(true);
		mMap.setPadding(0, 220, 0, 0);
		
		mMap.setOnMapClickListener(new OnMapClickListener() {
			
			@Override
			public void onMapClick(LatLng arg0) {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
			    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
			}
		});
		
		// Get the LocationManager object from the System Service LOCATION_SERVICE
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		registerForLocationUpdates();
		requestQueue = Volley.newRequestQueue(this);
		
		etStart = (EditText) findViewById(R.id.etStart);
		etDestination = (EditText) findViewById(R.id. etDestination);
		
		registerSearchKeyListener();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(this);
	}
	
	private void repositionMyLocationButton() {
	// repositions the myLocation Button on map
		View b = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getView().findViewById(2);
		
		if (b != null) {
			// ZoomControl is inside of RelativeLayout
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) b.getLayoutParams();
			
			// Align it to - parent BOTTOM|LEFT
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
			params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
			
			// Update margins, set to 10dp
			final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
					getResources().getDimension(R.dimen.map_margin),
			        getResources().getDisplayMetrics());
			params.setMargins(margin, margin, margin, margin);
			 
			b.setLayoutParams(params);
		}
	}

	private void registerForLocationUpdates() {
		// Create a criteria object needed to retrieve the provider
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		
		// Get the name of the best available provider
		String provider = locationManager.getBestProvider(criteria, true);
				
		// request that the provider send this activity GPS updates every 20
		// seconds
		locationManager.requestLocationUpdates(provider, 10000, 0, this);
		
		// We can use the provider immediately to get the last known location
		this.lastKnownLocation = locationManager.getLastKnownLocation(provider);
		
		zoomToLocation(this.lastKnownLocation);
	}
	
	private void registerSearchKeyListener() {
		etDestination.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
		            switch (keyCode) {
		                case KeyEvent.KEYCODE_ENTER:
		                	fetchRoute();
		                    return true;
		                default:
		                    break;
		            }
		        }
		        return false;
			}
		});
	}
	
	
	public void onClick(View view) {
		// Implements the functionality of search button and swap button.
		switch(view.getId()) 
		{
		case R.id.buttonGo:

			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

			fetchRoute();
			break;
		case R.id.ibSwap:
			swapLocations();
			break;
		}
	}
	
	private void swapLocations() {
		// swaps the start and end location.
		String start = etStart.getText().toString();
		String destination = etDestination.getText().toString();
		
		etStart.setText(destination);
		etDestination.setText(start);
	}

	private void fetchRoute() {
		String start = getAddress(etStart);
		String end = getAddress(etDestination);
		
		if (start.isEmpty() || end.isEmpty()) {
			Toast.makeText(this, "Please enter proper address.", Toast.LENGTH_SHORT).show();
			return;
		}
		
		String url = String.format(DIRECTIONS_URL, start, end);
		
		Log.d(MainActivity.class.getSimpleName(), "URL: " + url);
		
		fetchRouteIfNetworkAvailable(url);
	}

	private void fetchRouteIfNetworkAvailable(String url) {
		// fetches route info using the url
		Listener<DirectionResponse> responseListener = new Listener<DirectionResponse>() {
			@Override
			public void onResponse(DirectionResponse directionResponse) {
				Log.i(MainActivity.class.getSimpleName(), "Direction Response: " + directionResponse);
				MainActivity.this.directionResponse = directionResponse;
				showRoute(MainActivity.this.directionResponse);
			}
		};
		
		ErrorListener errorListener = new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				Toast.makeText(MainActivity.this, "Error: " + arg0.getMessage(), Toast.LENGTH_SHORT).show();
			}
		};
		
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
        	requestQueue.add(new GsonRequest<DirectionResponse>(url, DirectionResponse.class, null, responseListener, errorListener));        
        } else {
            Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
        }
	}

	private String getAddress(EditText editText) {
		String address = editText.getText().toString();
		
		if (0 == address.trim().compareToIgnoreCase(getString(R.string.my_location))) {
			address = this.lastKnownLocation.getLatitude() + "," + this.lastKnownLocation.getLongitude();
		} else {
			address = address.replace(' ', '+');
		}
		return address;
	}
	
	private void zoomToLocation(Location location) {
		// convert the location object to a LatLng object that can be used by the map API
		LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
			 
		// zoom to the current location
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition,16));
	}
	
	private void showRoute(DirectionResponse directionResponse) {
	// This method plots the route on map using Polyline
		mMap.clear();
		
		if (directionResponse.getStatus().compareTo("OK") != 0) {
			Toast.makeText(this, "No valid route found. Please check addresses.", Toast.LENGTH_LONG).show();
			return;
		}
		
		Leg firstLeg = directionResponse.getRoutes()[0].getLegs()[0];
		
		Toast.makeText(this, "This route is " + firstLeg.getDistance().getText() 
				+ " and will take "+ firstLeg.getDuration().getText(), Toast.LENGTH_LONG).show();

		Step[] steps = firstLeg.getSteps();
		
		List<LatLng> listOfPolylines = new ArrayList<LatLng>();
		
		for(Step step: steps) {
			List<LatLng> list = PolyUtil.decode(step.getPolyline().getPoints());
			listOfPolylines.addAll(list);
		}
		
		LatLng[] latLngArray = new LatLng[listOfPolylines.size()];
		listOfPolylines.toArray(latLngArray);
		
		mMap.addPolyline(new PolylineOptions().add(latLngArray).width(15).color(0x550000FF));
		
		Leg directionLeg = directionResponse.getRoutes()[0].getLegs()[0];
		com.uw.tracknwalk.model.Location start = directionLeg.getStart_location();
		com.uw.tracknwalk.model.Location end = directionLeg.getEnd_location();
		
		LatLng startLatLng = new LatLng(start.getLat(), start.getLng());
		LatLng endLatLng = new LatLng(end.getLat(), end.getLng());
		
		Builder builder = new Builder()
			.include(startLatLng)
			.include(endLatLng);
		
		LatLngBounds bounds = builder.build();
		
		mMap.addMarker(new MarkerOptions()
		  .position(startLatLng)
		  .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
		  .title(getString(R.string.marker_start))
		  .snippet(directionLeg.getStart_address())
		  );
		
		mMap.addMarker(new MarkerOptions()
		  .position(endLatLng)
		  .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
		  .title(getString(R.string.marker_end))
		  .snippet(directionLeg.getEnd_address())
		  );
		
		mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
	}
	
	public void onBackgroundClick(View view) {
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
	    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
	}
	
	@Override
	public void onLocationChanged(Location location) {
		this.lastKnownLocation = location;
	}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}
}