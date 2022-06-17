package com.example.jamesalon.find_my_mechanic_app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity2 extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
        private GoogleMap mMap;
        GoogleApiClient mGoogleApiClient;
        Location mLastLocation;
        LocationRequest mLocationRequest;
        private Button mLogout, mRequest, mSettings, mHistory;
        private LatLng pickUpLocation;
        private boolean isLogingout = false;
        private boolean requestBol = false;
        private Marker pickUpMarker;
        private LinearLayout mMechanicInfo;
        private ImageView mMechanicProfileImage;
        private TextView mMechanicName, mMechanicPhone;
        private RadioGroup mRadioGroup;
        private String mRequestService;
        private RatingBar mRatingBar;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_customer_map2);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(CustomerMapActivity2.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            } else {
                mapFragment.getMapAsync(this);
            }

            mMechanicInfo = (LinearLayout) findViewById(R.id.mechanicInfo);
            mMechanicProfileImage = (ImageView) findViewById(R.id.mechanicProfileImage);
            mMechanicName = (TextView) findViewById(R.id.mechanicName);
            mMechanicPhone = (TextView) findViewById(R.id.mechanicphone);

            mLogout = (Button) findViewById(R.id.nav_logout);
            mRequest = (Button) findViewById(R.id.request);
            mSettings = (Button) findViewById(R.id.nav_manage);
            mHistory = (Button) findViewById(R.id.nav_history);
            mRatingBar = (RatingBar) findViewById(R.id.ratingBar);

            mRadioGroup = (RadioGroup) findViewById(R.id.radiogroup);
            mRadioGroup.check(R.id.engine);


            mRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (requestBol) {
                        RequestCancled();
                    } else {

                        int selectedId = mRadioGroup.getCheckedRadioButtonId();

                        final RadioButton radioButton = (RadioButton) findViewById(selectedId);

                        if (radioButton.getText() == null) {
                            return;
                        }

                        mRequestService = radioButton.getText().toString();


                        requestBol = true;
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                        GeoFire geoFire = new GeoFire(ref);
                        geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                        pickUpLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                        pickUpMarker = mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Your here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
                        mRequest.setText("Getting your Mechanic.....");
                        getClosestMechanic();

                    }

                }
            });


            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);
        }

        private int radius = 1;
        private boolean mechanicFound = false;
        private String mechanicFoundID;
        GeoQuery geoQuery;

        private void getClosestMechanic() {

            DatabaseReference mechanicLocation = FirebaseDatabase.getInstance().getReference().child("MechanicsAvailable");

            GeoFire geoFire = new GeoFire(mechanicLocation);
            geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude), radius);
            geoQuery.removeAllListeners();

            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    if (!mechanicFound) {

                        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(key);
                        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                    Map<String, Object> Mechanicmap = (Map<String, Object>) dataSnapshot.getValue();
                                    if (mechanicFound) {
                                        return;
                                    }
                                    if (Mechanicmap.get("service").equals(mRequestService)) {

                                        mechanicFound = true;
                                        mechanicFoundID = dataSnapshot.getKey();

                                        DatabaseReference mechanicRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechanicFoundID);
                                        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                        HashMap map = new HashMap();
                                        map.put("customerRideId", customerId);
                                        mechanicRef.updateChildren(map);

                                        getMechanicInfo();
                                        getMechanicLocation();
                                        //JobHasBeenFinished();
                                        mRequest.setText("Looking For Mechanic Location...");


                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onKeyExited(String key) {

                }

                public void onKeyMoved(String key, GeoLocation location) {

                }

                @Override
                public void onGeoQueryReady() {
                    if (!mechanicFound) {
                        radius++;
                        getClosestMechanic();
                    }
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                }
            });
        }

        private Marker mMechanicMaker;
        private DatabaseReference mechanicLocationRef;
        private ValueEventListener mechanicLocationRefListner;

        private void getMechanicLocation() {
            mechanicLocationRef = FirebaseDatabase.getInstance().getReference().child("MechanicsWorking").child(mechanicFoundID).child("l");
            mechanicLocationRefListner = mechanicLocationRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        List<Object> map = (List<Object>) dataSnapshot.getValue();
                        double locationLat = 0;
                        double locationLong = 0;
                        mRequest.setText("Mechanic Found");
                        if (map.get(0) != null) {
                            locationLat = Double.parseDouble(map.get(0).toString());
                        }
                        if (map.get(1) != null) {
                            locationLong = Double.parseDouble(map.get(1).toString());
                        }

                        LatLng mechanicLatLong = new LatLng(locationLat, locationLong);
                        if (mMechanicMaker != null) {
                            mMechanicMaker.remove();
                        }
                        Location loc1 = new Location("");
                        loc1.setLatitude(pickUpLocation.latitude);
                        loc1.setLongitude(pickUpLocation.longitude);

                        Location loc2 = new Location("");
                        loc2.setLatitude(mechanicLatLong.latitude);
                        loc2.setLongitude(mechanicLatLong.longitude);

                        float distance = loc1.distanceTo(loc2);
                        if (distance < 200) {
                            mRequest.setText("Mechanic is Here:" + String.valueOf(distance));
                        } else {
                            mRequest.setText("Mechanic Found:" + String.valueOf(distance));
                        }
                        mMechanicMaker = mMap.addMarker(new MarkerOptions().position(mechanicLatLong).title("Your Mechanic").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_mech)));
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        }

        private void getMechanicInfo() {
            mMechanicInfo.setVisibility(View.VISIBLE);
            DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechanicFoundID);

            mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                        if (map.get("name") != null) {
                            mMechanicName.setText(map.get("name").toString());
                        }
                        if (map.get("phone") != null) {
                            mMechanicPhone.setText(map.get("phone").toString());
                        }
                        if (dataSnapshot.child("profileImageUrl") != null) {
                            Glide.with(getApplication()).load(dataSnapshot.child("profileImageUrl").toString()).into(mMechanicProfileImage);
                        }
                        int ratingSum = 0;
                        float ratingTotal = 0;
                        float ratingAvg = 0;
                        for (DataSnapshot child : dataSnapshot.child("rating").getChildren()) {
                            ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                            ratingTotal++;

                        }
                        if (ratingTotal != 0) {
                            ratingAvg = ratingSum / ratingTotal;
                            mRatingBar.setRating(ratingAvg);
                        }

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }


        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera. In this case,
         * we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to install
         * it inside the SupportMapFragment. This method will only be triggered once the user has
         * installed Google Play services and returned to the app.
         */
        private DatabaseReference jobHasBeenFinisheRef;
        private ValueEventListener jobHasBeenFinisheRefListner;

        private void JobHasBeenFinished() {
            jobHasBeenFinisheRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechanicFoundID).child("customerRequest");
            jobHasBeenFinisheRefListner = jobHasBeenFinisheRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {

                    } else {
                        RequestCancled();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }

        private void RequestCancled() {
            requestBol = false;
            geoQuery.removeAllListeners();
            mechanicLocationRef.removeEventListener(mechanicLocationRefListner);
            jobHasBeenFinisheRef.removeEventListener(jobHasBeenFinisheRefListner);

            if (mechanicFoundID != null) {
                DatabaseReference mechRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechanicFoundID);
                mechRef.setValue(true);
                mechanicFoundID = null;
            }
            mechanicFound = false;
            radius = 1;
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
            GeoFire geoFire = new GeoFire(ref);
            geoFire.removeLocation(userId);
            if (pickUpMarker != null) {
                pickUpMarker.remove();
            }
            mRequest.setText("Search For Mechanic");
            mMechanicInfo.setVisibility(View.GONE);
            mMechanicName.setText("");
            mMechanicPhone.setText("");
            mMechanicProfileImage.setImageResource(R.mipmap.ic_user);

        }


        @Override
        public void onBackPressed() {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.customer_map_activity2, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();


            return super.onOptionsItemSelected(item);
        }

        @SuppressWarnings("StatementWithEmptyBody")
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            // Handle navigation view item clicks here.
            int id = item.getItemId();

            if (id == R.id.nav_manage) {
                // Handle the camera action
                mSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(CustomerMapActivity2.this, CustomerSettingsActivity.class);
                        startActivity(intent);
                        return;
                    }
                });
            } else if (id == R.id.nav_history) {
                mHistory.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(CustomerMapActivity2.this, HistoryActivity.class);
                        intent.putExtra("CustomerOrMechanic", "Customers");
                        startActivity(intent);
                        return;
                    }
                });

            } else if (id == R.id.nav_logout) {
                mLogout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isLogingout = true;
                        disconnectMechanic();
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(CustomerMapActivity2.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                        return;

                    }
                });

            } else if (id == R.id.nav_manage) {

            }

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);

        }

        protected synchronized void buildGoogleApiClient() {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();

        }

        @Override
        public void onLocationChanged(Location location) {
            if (getApplicationContext() != null) {
                mLastLocation = location;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));


            }

        }


        @Override
        public void onConnected(@Nullable Bundle bundle) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(CustomerMapActivity2.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

        final int LOCATION_REQUEST_CODE = 1;

        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, MapFragment mapFragment) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            switch (requestCode) {
                case LOCATION_REQUEST_CODE: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        mapFragment.getMapAsync(this);
                    } else {
                        Toast.makeText(getApplicationContext(), "please provide the permision", Toast.LENGTH_LONG).show();
                    }
                    break;
                }
            }
        }

        private void disconnectMechanic() {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("MechanicsAvailable");

            GeoFire geoFire = new GeoFire(ref);
            geoFire.removeLocation(userId);

        }


        @Override
        protected void onStop() {
            super.onStop();

        }

    }


