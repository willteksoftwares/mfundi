package com.willteksolutions.mfundy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class PlumbersMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private static final int REQUEST_PERMISSION_LOCATION = 255;

    private Button plumberLogout, plumberSettingsBtn;

    private String homeownerID = "";
    private Boolean isLoggingOut = false;

    private LinearLayout mHomeownerInfoLinearLayout;
    private ImageView mHomeownerProfilePic;
    private TextView mHomeownerNames, mHomeownerPhoneNumbers, homeownerDestination;

    private DatabaseReference homeownerDatabaseRef;

    private String mName = "";
    private String mPhone = "";
    private String mLocation = "";
    private String mProfilePicture = "";
    private Uri resultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plumbers_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mHomeownerInfoLinearLayout = findViewById(R.id.homeownerInfo);
        mHomeownerProfilePic = findViewById(R.id.homeownerProfilePic);
        mHomeownerNames = findViewById(R.id.homeownerNames);
        mHomeownerPhoneNumbers = findViewById(R.id.homeownerPhoneNumbers);
        homeownerDestination = findViewById(R.id.homeownerDestination);

        plumberLogout = findViewById(R.id.plumberLogout);
        plumberSettingsBtn = findViewById(R.id.plumberSettingsBtn);

        plumberSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlumbersMapsActivity.this, PlumberSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });


        plumberLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;
                disconnectPlumber();
               FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(PlumbersMapsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        getAssignedHomeowner();
    }

    private void getAssignedHomeowner() {
        String plumberID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("users").child("plumbers").child(plumberID).child("homeownerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
               if (dataSnapshot.exists()){
                       homeownerID = dataSnapshot.getValue().toString();
                       getAssignedHomeownerPickUpLocation();
                       getAssignedHomeownerInfo();
               }else {
                   homeownerID = "";
                   if (pickUpMarker!=null){
                       pickUpMarker.remove();
                   }
                   if (assignedCustomerPickUpLocationRefListener !=null){
                       assignedCustomerPickUpLocationRef.removeEventListener(assignedCustomerPickUpLocationRefListener);
                   }
                   mHomeownerInfoLinearLayout.setVisibility(View.GONE);
                   mHomeownerNames.setText("");
                   mHomeownerPhoneNumbers.setText("");
                   homeownerDestination.setText("");
                   mHomeownerProfilePic.setImageResource(R.drawable.profile_image);
               }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedHomeownerInfo(){
        mHomeownerInfoLinearLayout.setVisibility(View.VISIBLE);
        homeownerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users").child("homeowners").child(homeownerID);
        homeownerDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") !=null){
                        mName = map.get("name").toString();
                        mHomeownerNames.setText(mName);
                    }

                    if (map.get("phone") !=null){
                        mPhone = map.get("phone").toString();
                        mHomeownerPhoneNumbers.setText(mPhone);
                    }
                    if (map.get("location") !=null){
                        mLocation = map.get("location").toString();
                        homeownerDestination.setText(mLocation);
                    }
                    if (map.get("profile picture url") !=null){
                        mProfilePicture = map.get("profile picture url").toString();
                        Glide.with(getApplication()).load(mProfilePicture).into(mHomeownerProfilePic);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    Marker pickUpMarker;
    private DatabaseReference assignedCustomerPickUpLocationRef;
    private ValueEventListener assignedCustomerPickUpLocationRefListener;

    private void getAssignedHomeownerPickUpLocation() {
        assignedCustomerPickUpLocationRef = FirebaseDatabase.getInstance().getReference().child("homeowner requests").child(homeownerID).child("l");
        assignedCustomerPickUpLocationRefListener = assignedCustomerPickUpLocationRef .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !homeownerID.equals("")){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng plumberLatLng = new LatLng(locationLat,locationLng);
                    pickUpMarker =  mMap.addMarker(new MarkerOptions().position(plumberLatLng).title("Work Location"));
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
        if (ContextCompat.checkSelfPermission(PlumbersMapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        if (ContextCompat.checkSelfPermission(PlumbersMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
            }
        }
        if (ContextCompat.checkSelfPermission(PlumbersMapsActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        if (getApplicationContext() !=null){
            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference DriversAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("plumbers available");
            GeoFire geoFireAvailability = new GeoFire(DriversAvailabilityRef);

            DatabaseReference DriversWorkingRef = FirebaseDatabase.getInstance().getReference().child("plumbers working");
            GeoFire geoFireWorking = new GeoFire(DriversWorkingRef);

            switch (homeownerID){
                case "":

                    geoFireWorking.removeLocation(userID,new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                        }
                    });

                    geoFireAvailability.setLocation(userID, new GeoLocation(location.getLatitude(),location.getLongitude()),new
                            GeoFire.CompletionListener(){
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                }
                            });
                    break;

                default:

                    geoFireAvailability.removeLocation(userID,new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                        }
                    });

                    geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(),location.getLongitude()),new
                            GeoFire.CompletionListener(){
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                }
                            });

                    break;
            }
        }
    }
    private void disconnectPlumber(){
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,mLocationRequest,this);
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference DriversAvailabiltyRef = FirebaseDatabase.getInstance()
                .getReference().child("plumbers available");
        GeoFire geoFire = new GeoFire(DriversAvailabiltyRef);
        geoFire.removeLocation(userID, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                Toast.makeText(PlumbersMapsActivity.this, "Plumber availability removed", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!isLoggingOut){
            disconnectPlumber();
        }

    }
}
