package com.example.ipoint;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;
import com.skyfishjy.library.RippleBackground;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import com.google.android.gms.location.places.AutocompletePrediction;
//import com.google.android.gms.location.places.Places;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

        private GoogleMap mMap;                         // map object
        private FusedLocationProviderClient mClient;    //Responsible for fetching current location of device
        private PlacesClient poi;                       //loads suggested P.O.I based on location
        private List<AutocompletePrediction> pList;     //arrayList to save autocomplte predictions

        private Location mLastKnownLocation;            //last known location
        private LocationCallback locationCallback;      //updating users request


        private MaterialSearchBar searchBar;
        private View mapView;
        private Button btnPOI;
        private RippleBackground rippleBackground;

        LatLng latLngCurrent;

        private final float DEFAULT_ZOOM = 18;



        protected void onCreate(Bundle savedInstanceState){

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_map);

            searchBar = findViewById(R.id.searchBar);
            btnPOI = findViewById(R.id.poi_btn);
            rippleBackground = findViewById(R.id.ripple_bg);

            // find map fragment and load it into variable
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);


            mapFragment.getMapAsync(this);
            mapView = mapFragment.getView();

            //initialize FusedLocationProviderClient
            mClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);
            //initialize places which needs the context and google api key
            //The Places API is a service that returns information about places using HTTP requests.
            // Places are defined within this API as establishments, geographic locations, or prominent points of interest.
            Places.initialize(MapActivity.this, "AIzaSyAxDuHpFaZWg7aLf4SmV2Hi0zWNA7YcU4I");

            poi = Places.createClient(this);
            //Token used for sessionizing multiple instances of FindAutocompletePredictionsRequest.
            final AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

            searchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
                @Override
                public void onSearchStateChanged(boolean enabled) {

                }

                @Override
                public void onSearchConfirmed(CharSequence text) {
                    startSearch(text.toString(), true, null, true );
                }

                @Override
                public void onButtonClicked(int buttonCode) {

                    if(buttonCode == MaterialSearchBar.BUTTON_NAVIGATION){
                        //opening or closing a navigation drawer
                        //searchBar.showSuggestionsList();

                    }else if(buttonCode == MaterialSearchBar.BUTTON_BACK){  // opening and closing navigation bar
                        searchBar.disableSearch();

                    }

                }
            });
            // implement the searching part
            searchBar.addTextChangeListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    FindAutocompletePredictionsRequest predictionRequest = FindAutocompletePredictionsRequest.builder().setCountry("usa")
                            .setTypeFilter(TypeFilter.ADDRESS)
                            .setSessionToken(token)
                            .setQuery(s.toString())
                            .build();
                    poi.findAutocompletePredictions(predictionRequest).addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                            if (task.isSuccessful()){
                                FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                                if(predictionsResponse != null){
                                    pList = predictionsResponse.getAutocompletePredictions();
                                    // convert pList into a list of strings
                                    List<String> suggestionList = new ArrayList<>();
                                    for(int i = 0; i < pList.size(); i++)
                                    {
                                        AutocompletePrediction prediction = pList.get(i);
                                        suggestionList.add(prediction.getFullText(null).toString());
                                    }
                                    searchBar.updateLastSuggestions(suggestionList);
                                    if(!searchBar.isSuggestionsVisible()) {
                                        searchBar.showSuggestionsList();
                                    }
                                }

                            }else{
                                Log.i("mytag", "prediction task unsuccessful");
                            }


                        }
                    });



                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            searchBar.setSuggstionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
                @Override
                public void OnItemClickListener(int position, View v) {
                    // when sugestion is clicked we need location place id and send it to
                    // google api to request lat and long
                    if(position >= pList.size()) {
                        return;
                    }
                    AutocompletePrediction selectedPrediction = pList.get(position);
                    String suggestion = searchBar.getLastSuggestions().get(position).toString();
                    searchBar.clearSuggestions();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            searchBar.clearSuggestions();
                        }
                    }, 1000);
                    // clear keyboard from screen
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if(imm != null){
                        imm.hideSoftInputFromWindow(searchBar.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                    }
                    String placeId = selectedPrediction.getPlaceId();
                    List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);

                    FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields).build();
                    poi.fetchPlace(fetchPlaceRequest).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                        @Override
                        public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                            Place place = fetchPlaceResponse.getPlace();
                            Log.i("mytag", "place found: " + place.getName());
                            LatLng latLngOfPlace = place.getLatLng();
                            if(latLngOfPlace != null)
                            {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngOfPlace, DEFAULT_ZOOM));
                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (e instanceof ApiException) {
                                ApiException apiException = (ApiException) e;
                                apiException.printStackTrace();
                                int statusCode = apiException.getStatusCode();
                                Log.i("mytag", "place not found: " + e.getMessage());
                                Log.i("mytag", "staus code: " + statusCode);
                            }
                        }
                    });
                }

                @Override
                public void OnItemDeleteListener(int position, View v) {

                }
            });
            btnPOI.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {



                    LatLng currentMarkerLocation = mMap.getCameraPosition().target;
                    rippleBackground.startRippleAnimation();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            rippleBackground.stopRippleAnimation();



                            startActivity(new Intent(MapActivity.this, PoiActivity.class));
                            finish();
                        }
                    }, 3000);


                }


            });

    }



    /* callback function called when the map is ready and loaded.
        allow us to implement what we need to do on map ready.
     */

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        /* need to suppress because it wants to make sure it has the permissions in place
            which are already granted in the MainActivity
         */

        // enable location
        mMap.setMyLocationEnabled(true);
        // location button
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // set location of the location button so it is not at the top
        if(mapView != null && mapView.findViewById(Integer.parseInt("1")) != null){
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1") ).getParent()).findViewById(Integer.parseInt("2"));
            // fetch layout params of location button
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE );
            layoutParams.setMargins(0,0,40,180);
        }

        // check if GPS is enabled or not and request user to enable it
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /* Determine if the relevant system settings are enabled on the device to carry out the desired location request.
           Invoke a dialog that allows the user to enable the necessary location settings with a single tap. */


        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(MapActivity.this);
        //Then check whether current location settings are satisfied
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        // Two possible outcomes success or failure
        // handle success
        task.addOnSuccessListener(MapActivity.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                getDeviceLocation();

            }
        });
        // handle failure
        task.addOnFailureListener(MapActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //on failure check if issue can be resolved
                if(e instanceof ResolvableApiException) {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    try {
                        resolvable.startResolutionForResult(MapActivity.this, 51);  //this dialog shows user where to enable location or decline it, see onActivityResult
                    } catch (IntentSender.SendIntentException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if(searchBar.isSuggestionsVisible())
                    searchBar.clearSuggestions();
                if(searchBar.isSearchEnabled())
                    searchBar.disableSearch();
                return false;
            }
        });
    }
    // check for the result. did user accept or deny?
   protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 51){
            //check if user ok'd gps location
            if(resultCode == RESULT_OK){
                // find user's location
                getDeviceLocation();
            }
        }
   }

   @SuppressLint("MissingPermission")
   private void getDeviceLocation() {
        // this function again depends on permission request from MainActivity so
       // we surpress it
       mClient.getLastLocation()
               .addOnCompleteListener(new OnCompleteListener<Location>() {
                   @Override
                   public void onComplete(@NonNull Task<Location> task) {
                       if(task.isSuccessful()){
                           // fetch last location update camera
                           mLastKnownLocation = task.getResult();
                           // try to get the last location from provider
                           // if null create location request to get location object
                           // but if still null just return
                           // if not null get last location
                           if(mLastKnownLocation != null){
                               mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                           }else{
                               final LocationRequest locationRequest = LocationRequest.create();
                               locationRequest.setInterval(10000);
                               locationRequest.setFastestInterval(5000);
                               locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                               locationCallback = new LocationCallback(){
                                   @Override
                                   public void onLocationResult(LocationResult locationResult){
                                       super.onLocationResult(locationResult);
                                       if(locationResult == null){
                                           return;
                                       }
                                       mLastKnownLocation = locationResult.getLastLocation();
                                       mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                       mClient.removeLocationUpdates(locationCallback);
                                   }
                               };
                               mClient.requestLocationUpdates(locationRequest, locationCallback, null);
                           }
                           // if not successful
                       }else{
                           Toast.makeText(MapActivity.this, "Unable to retrieve last location", Toast.LENGTH_SHORT);
                       }

                   }
               });



   }
}
