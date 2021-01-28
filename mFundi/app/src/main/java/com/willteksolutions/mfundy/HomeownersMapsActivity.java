package com.willteksolutions.mfundy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeownersMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private static final int REQUEST_PERMISSION_LOCATION = 255;
    private Button homeownerLogout,homeownerSettings,callAPlumberBtn;

    private LatLng pickUpLocation;
    private Boolean requestBool =false;
    private Marker pickUpMarker;

    private LinearLayout plumberInfoLinearLayout;
    private ImageView plumberProfilePic;
    private TextView plumberNames, plumberPhoneNumbers, plumberCompany;

    private DatabaseReference plumberDatabaseRef;

    private String mName = "";
    private String mPhone = "";
    private String mCompany = "";
    private String mProfilePicture = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homeowners_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyDbBS59Nz5vzYps1nBL8ycq6cQi4jKtuUM");
        }

        plumberInfoLinearLayout = findViewById(R.id.plumberInfo);
        plumberProfilePic = findViewById(R.id.plumberProfilePic);
        plumberNames = findViewById(R.id.plumberNames);
        plumberPhoneNumbers = findViewById(R.id.plumberPhoneNumbers);
        plumberCompany = findViewById(R.id.plumberCompany);

        homeownerLogout = findViewById(R.id.homeownerLogout);
        callAPlumberBtn = findViewById(R.id.callAPlumberBtn);
        homeownerSettings = findViewById(R.id.homeownerSettings);

        homeownerLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(HomeownersMapsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        homeownerSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeownersMapsActivity.this, HomeownerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        callAPlumberBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBool){
                    requestBool = false;
                    geoQuery.removeAllListeners();
                    plumbersLocationRef.removeEventListener(plumbersLocationRefListener);

                    if (plumberFoundID != null){
                        DatabaseReference plumbersRef = FirebaseDatabase.getInstance().getReference().child("users").child("plumbers").child(plumberFoundID).child("homeownerRideId");
                        plumbersRef.removeValue();
                        plumberFoundID = null;
                    }
                    plumberFound = false;
                    radius = 1;

                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("homeowner requests");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userID, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                        }
                    });

                    if (pickUpMarker!=null){
                        pickUpMarker.remove();
                    }
                    callAPlumberBtn.setText("Call a Plumber");
                    plumberInfoLinearLayout.setVisibility(View.GONE);
                    plumberNames.setText("");
                    plumberPhoneNumbers.setText("");
                    plumberCompany.setText("");
                    plumberProfilePic.setImageResource(R.drawable.profile_image);

                }else {
                    requestBool = true;
                    String userID = FirebaseAuth.getInstance().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("homeowner requests");

                    GeoFire geoFire = new GeoFire(ref);

                    if (mLastLocation != null) {
                        geoFire.setLocation(userID, new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()),new
                                GeoFire.CompletionListener(){
                                    @Override
                                    public void onComplete(String key, DatabaseError error) {
                                    }
                                });
                    }

                    if (mLastLocation!=null){
                        pickUpLocation = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                    }

                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("My home"));
                    callAPlumberBtn.setText("Getting your Plumber...");

                    getClosestPlumber();
                }

            }
        });


    }
    private int radius = 1;
    private Boolean plumberFound = false;
    private String plumberFoundID = "";
    GeoQuery geoQuery;
    private void getClosestPlumber() {
        DatabaseReference plumberLocationRef = FirebaseDatabase.getInstance().getReference().child("plumbers available");
        GeoFire geoFire= new GeoFire(plumberLocationRef);

        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!plumberFound && requestBool){
                    plumberFound = true;
                    plumberFoundID = key;

                    DatabaseReference plumbersRef = FirebaseDatabase.getInstance().getReference().child("users").child("plumbers").child(plumberFoundID);
                    String homeownerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("homeownerRideId", homeownerID);
                    plumbersRef.updateChildren(map);

                    getPlumberLocation();
                    getPlumberInfo();
                    callAPlumberBtn.setText("Looking for Plumber Location...");

                }


            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!plumberFound){
                    radius ++;
                    getClosestPlumber();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }


    private  Marker plumberMarker;
    private DatabaseReference plumbersLocationRef;
    private ValueEventListener plumbersLocationRefListener;
    private void getPlumberLocation() {
        plumbersLocationRef = FirebaseDatabase.getInstance().getReference().child("plumbers working").child(plumberFoundID).child("l");
        plumbersLocationRefListener =  plumbersLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBool){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    callAPlumberBtn.setText("Plumber Found");
                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng plumberLatLng = new LatLng(locationLat,locationLng);
                    if (plumberMarker!=null){
                        plumberMarker.remove();
                    }
                    Location location1 = new Location("");
                    location1.setLatitude(pickUpLocation.latitude);
                    location1.setLongitude(pickUpLocation.longitude);

                    Location location2 = new Location("");
                    location2.setLatitude(plumberLatLng.latitude);
                    location2.setLongitude(plumberLatLng.longitude);

                    float distance = location1.distanceTo(location2);
                    if (distance <20){
                        callAPlumberBtn.setText("Plumber has arrived");
                    }else {
                        callAPlumberBtn.setText("Plumber at " + distance + "M. Tap to CANCEL");
                    }

                    plumberMarker = mMap.addMarker(new MarkerOptions().position(plumberLatLng).title("Your Plumber is here"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getPlumberInfo(){
        plumberInfoLinearLayout.setVisibility(View.VISIBLE);
        plumberDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users").child("plumbers").child(plumberFoundID);
        plumberDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") !=null){
                        mName = map.get("name").toString();
                        plumberNames.setText(mName);
                    }

                    if (map.get("phone") !=null){
                        mPhone = map.get("phone").toString();
                        plumberPhoneNumbers.setText(mPhone);
                    }
                    if (map.get("company") !=null){
                        mCompany = map.get("company").toString();
                        plumberCompany.setText(mCompany);
                    }
                    if (map.get("profile picture url") !=null){
                        mProfilePicture = map.get("profile picture url").toString();
                        Glide.with(getApplication()).load(mProfilePicture).into(plumberProfilePic);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleAPIClient();
        if (ContextCompat.checkSelfPermission(HomeownersMapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_LOCATION);
            }
        } else {
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void  buildGoogleAPIClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ContextCompat.checkSelfPermission(HomeownersMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
            }
        }
        if (ContextCompat.checkSelfPermission(HomeownersMapsActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_LOCATION);
            }
        }else {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,mLocationRequest,this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
