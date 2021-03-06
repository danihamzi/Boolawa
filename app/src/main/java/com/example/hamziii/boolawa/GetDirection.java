package com.example.hamziii.boolawa;

import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.directions.route.*;
import com.directions.route.Route;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.hamziii.boolawa.R.id.map;

public class GetDirection extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener , RoutingListener {

    private GoogleMap mMap;

    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Location lastLocation ;

    public static final int REQUEST_LOCATION_CODE = 99 ;

    private Marker currentLocationMarker ;
    private Marker destMarker ;
    private MarkerOptions mo ;

    private LatLng currentLatLng , destLatLng ;
    private List<Polyline> polylinePaths ;

    private List<LatLng> points;

    private EditText et_searchDest ;
    private Button btn_searchDest ;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.colorPrimaryDark,R.color.colorPrimary,R.color.gbtn,R.color.colorAccent,R.color.primary_dark_material_light};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_direction);

        et_searchDest = (EditText)findViewById(R.id.et_searchDest);
        btn_searchDest = (Button)findViewById(R.id.btn_searchDest);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            checkLocationPermission();
        }

        polylines = new ArrayList<>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        btn_searchDest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(destMarker!=null){

                    destMarker.remove();
                }

                String searchString = et_searchDest.getText().toString();
                List<Address> addressList = null;

                mo = new MarkerOptions();

                if(! searchString.equals("")){

                    Geocoder geocoder = new Geocoder(getApplicationContext()) ;
                    try {
                        addressList = geocoder.getFromLocationName(searchString , 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    for(int i = 0 ; i<addressList.size() ; i++){
                        Address myAddresss = addressList.get(i);

                        destLatLng = new LatLng(myAddresss.getLatitude() , myAddresss.getLongitude());
                        mo.position(destLatLng);
                        //mMap.addMarker(mo);
                        destMarker = mMap.addMarker(mo) ;
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(destLatLng));
                    }
                }

                /*LatLng start = new LatLng(18.015365, -77.499382);
                LatLng end1 = new LatLng(18.012590, -77.500659);

                Routing routing = new Routing.Builder()
                        .travelMode(AbstractRouting.TravelMode.DRIVING)
                        //.withListener(this)
                        .waypoints(start, end1)
                        .build();
                routing.execute();*/

                getRouteToMarker(destLatLng);

            }
        });
    }

    private void getRouteToMarker(LatLng destLatLng) {

        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(true)
                .waypoints(currentLatLng, destLatLng)
                .build();
        routing.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){

            case REQUEST_LOCATION_CODE:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    //permission is granted
                    if(ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        if(client == null){

                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else {

                    //permission is denied

                    Toast.makeText(this , "Permission denied" , Toast.LENGTH_LONG).show();
                }

                return;
        }
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }


        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }



    protected synchronized void buildGoogleApiClient() {

        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        client.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        lastLocation = location ;

        if(currentLocationMarker != null){
            currentLocationMarker.remove();
        }

        currentLatLng = new LatLng(location.getLatitude() , location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions() ;

        markerOptions.position(currentLatLng);
        markerOptions.title("Your location");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        currentLocationMarker = mMap.addMarker(markerOptions);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(15));

        if(client != null){

            LocationServices.FusedLocationApi.removeLocationUpdates(client , this);
        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        locationRequest = new LocationRequest();

        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {

            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
        }

    }

    public Boolean checkLocationPermission(){

        if(ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this , Manifest.permission.ACCESS_FINE_LOCATION)){

                ActivityCompat.requestPermissions(this , new String[] {Manifest.permission.ACCESS_FINE_LOCATION} , REQUEST_LOCATION_CODE);
            }
            else {

                ActivityCompat.requestPermissions(this , new String[] {Manifest.permission.ACCESS_FINE_LOCATION} , REQUEST_LOCATION_CODE);
            }
            return false ;
        }
        else {
            return false ;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public String[] parseDirections(String jsonData){

        JSONArray jsonArray = null ;
        JSONObject jsonObject ;

        try {
            jsonObject = new JSONObject(jsonData) ;
            jsonArray = jsonObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps");
        }catch (JSONException e){
            e.printStackTrace();
        }
        return getPaths(jsonArray);
    }

    public String[] getPaths(JSONArray googleStepJson){

        int count = googleStepJson.length();
        String[] polylines = new String[count];

        for(int i=0 ; i<count ; i++){

            try {
                polylines[i] = getPath(googleStepJson.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return polylines ;
    }

    public String getPath(JSONObject googlePathJson){

        String polyline = null;
        try {
        polyline = googlePathJson.getJSONObject("polyline").getString("points");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return polyline ;
    }

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

        for(Polyline line : polylines){

            line.remove();
        }
        polylines.clear();
    }
}
