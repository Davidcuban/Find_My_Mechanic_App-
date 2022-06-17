package com.example.jamesalon.find_my_mechanic_app;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MechanicMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener,RoutingListener{

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLogout, msettings, mmechStatus,mHistory;
    private Switch mworkingSwitch;
    private  int Status = 0;

    private boolean isLogingout = false;
    private String customerId = "";
    private LinearLayout mCustomerInfo;
    private ImageView mCustomerProfileImage;
    private TextView mCustomerName, mCustomerPhone;
    private TextView mNameField;
    FirebaseUser currentUser;
    private DrawerLayout dl;
    private ActionBarDrawerToggle abdt;
    private FirebaseAuth mAuth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_map);

        //navigation section

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        polylines = new ArrayList<>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MechanicMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
        }else {
            mapFragment.getMapAsync(this);
        }

        dl =(DrawerLayout)findViewById(R.id.dl);
        abdt = new ActionBarDrawerToggle(this,dl,toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_close);
        abdt.setDrawerIndicatorEnabled(true);

        dl.addDrawerListener(abdt);
        abdt.syncState();
        final NavigationView nav_view = (NavigationView)findViewById(R.id.nav_view);
        nav_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_manage) {
                    // Handle the camera action

                    Intent intent = new Intent(MechanicMapActivity.this, MechanicSettingsActivity.class);
                    startActivity(intent);
                    return true;

                } else if (id == R.id.nav_history) {

                    Intent intent = new Intent(MechanicMapActivity.this, HistoryActivity.class);
                    intent.putExtra("CustomerOrMechanic", "Mechanics");
                    startActivity(intent);
                    return true;

                } else if (id == R.id.nav_logout) {
                    disconnectMechanic();
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(MechanicMapActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return true;



                } else if (id == R.id.nav_manage) {

                }




                return true;
            }
        });



        mCustomerInfo = (LinearLayout)findViewById(R.id.customerInfo);

        mCustomerProfileImage = (ImageView)findViewById(R.id.customerProfileImage);

        mCustomerName = (TextView)findViewById(R.id.customerName);


        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        mCustomerPhone = (TextView)findViewById(R.id.customerphone);
        mmechStatus =(Button)findViewById(R.id.mechStatus);
        mHistory = (Button)findViewById(R.id.history);

        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MechanicMapActivity.this,HistoryActivity.class);
                intent.putExtra("CustomerOrMechanic","Mechanics");
                startActivity(intent);
                return;
            }
        });


        mmechStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(Status){
                    case 1:
                        Status =2;
                        erasePolylines();
                        mmechStatus.setText("JOB COMPLETED.");

                        break;
                    case 2:
                        recordhistory();
                        jobCompleted();

                        break;

                }
            }
        });

        mworkingSwitch =(Switch)findViewById(R.id.workingSwitch);
        mworkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    connectMechanic();
                }else{
                    disconnectMechanic();
                }
            }
        });

        msettings = (Button)findViewById(R.id.settings);

        mLogout = (Button)findViewById(R.id.logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLogingout = true;
                disconnectMechanic();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MechanicMapActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
        msettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MechanicMapActivity.this,MechanicSettingsActivity.class);
                startActivity(intent);

            }
        });
        getUserInfo();


        getAssignedCustomer();
    }
    @Override
    public void onBackPressed() {
        if(dl.isDrawerOpen(GravityCompat.START)){
            dl.closeDrawer(GravityCompat.START);
        }else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return abdt.onOptionsItemSelected(item)|| super.onOptionsItemSelected(item);
    }

    private void getAssignedCustomer(){
        String mechanicId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechanicId).child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Status = 1;
                        customerId = dataSnapshot.getValue().toString();
                        getAssignedCustomerPickupLocation();
                    getAssignedCustomerInfo();

                }else {
                    erasePolylines();
                  jobCompleted();
                }
                }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
// getting current user info

    private void getUserInfo() {
        NavigationView navigationView = (NavigationView)findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        mNameField  = (TextView) headerView.findViewById(R.id.current_user);
       ImageView mProfileImage = (ImageView) headerView.findViewById(R.id.current_user_image);

        mNameField.setText(currentUser.getEmail());

        // Glide.with(this).load(currentUser.getPhotoUrl()).into(mProfileImage);
    }


    private void  getAssignedCustomerInfo() {
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mCustomerName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        mCustomerPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    Marker pickUpMaker;
    private DatabaseReference assignedCustomerPickupLocationRef;
    private ValueEventListener assignedCustomerPickupLocationRefListner;
private void getAssignedCustomerPickupLocation(){
    assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
    assignedCustomerPickupLocationRefListner = assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.exists() && !customerId.equals("")) {
                List<Object> map = (List<Object>) dataSnapshot.getValue();
                double locationLat = 0;
                double locationLong = 0;
                if (map.get(0) != null) {
                    locationLat = Double.parseDouble(map.get(0).toString());
                }
                if (map.get(1) != null) {
                    locationLong = Double.parseDouble(map.get(1).toString());
                }

                LatLng pickUpLatLong = new LatLng(locationLat, locationLong);
                pickUpMaker = mMap.addMarker(new MarkerOptions().position(pickUpLatLong).title("Customer Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
                getRouteToMaker(pickUpLatLong);
            }

        }


        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    });



}

    private void getRouteToMaker(LatLng pickUpLatLong) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()), pickUpLatLong)
                .build();
        routing.execute();
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


    private void jobCompleted(){
        mmechStatus.setText("Woking Now");
        erasePolylines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mechanicRef =FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(userId).child("customerRequest");
        mechanicRef.removeValue();

        DatabaseReference ref =FirebaseDatabase.getInstance().getReference().child("costomerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerId);
        customerId = "";

        if(pickUpMaker !=null){
            pickUpMaker.remove();

        }
        if(assignedCustomerPickupLocationRefListner !=null){
            assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListner);
        }
        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerProfileImage.setImageResource(R.mipmap.ic_user);
    }

    private void  recordhistory(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mechanicRef =FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(userId).child("history");
        DatabaseReference customerRef =FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef =FirebaseDatabase.getInstance().getReference().child("history");
        String requestId = historyRef.push().getKey();
        mechanicRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("mechanic",userId);
        map.put("customer",customerId);
        map.put("rating",0);
        map.put("timeStamp",getCurrentTimeStamp());
        historyRef.child(requestId).updateChildren(map);



    }

    private Long getCurrentTimeStamp() {
      Long  timeStamp = System.currentTimeMillis()/1000;
        return timeStamp;

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
    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext()!=null){
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("MechanicsAvailable");
        DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("MechanicsWorking");
        GeoFire geoFirerefAvailable = new GeoFire(refAvailable);
        GeoFire geoFirereWorking= new GeoFire(refWorking);

            switch (customerId){
                case "":
                    geoFirereWorking.removeLocation(userId);
                   geoFirerefAvailable.setLocation(userId,new GeoLocation(location.getLatitude(),location.getLongitude()));


                    break;
                default:
                    geoFirerefAvailable.removeLocation(userId);
                    geoFirereWorking.setLocation(userId,new GeoLocation(location.getLatitude(),location.getLongitude()));

                    break;
            }
        }





    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


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
        switch (requestCode){
            case LOCATION_REQUEST_CODE: {
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                }else {
                    Toast.makeText(getApplicationContext(),"please provide the permision",Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void connectMechanic(){

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MechanicMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }
    private void disconnectMechanic(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("MechanicsAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

    }



    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};


    @Override
    public void onRoutingFailure(RouteException e) {

        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingCancelled() {

    }
    private void erasePolylines(){
        for (Polyline line:polylines){
            line.remove();
        }
        polylines.clear();
    }
}
