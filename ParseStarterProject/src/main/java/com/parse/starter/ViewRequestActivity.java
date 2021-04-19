package com.parse.starter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestActivity extends AppCompatActivity {
    ListView requestUserListView;
    ArrayList<String> request = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    ArrayList<Double> requestLatitude = new ArrayList<>();
    ArrayList<Double> requestLongitude = new ArrayList<>();
    ArrayList<String> username = new ArrayList<>();

    LocationManager locationManager;
    LocationListener locationListener;

    public void updateListView(Location location){
         if(location != null) {
             ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
             final ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

             query.whereNear("location", parseGeoPoint);
             query.whereDoesNotExist("driverUsername");
             query.setLimit(10);
             query.findInBackground(new FindCallback<ParseObject>() {
                 @Override
                 public void done(List<ParseObject> objects, ParseException e) {

                     if(e == null){

                         request.clear();
                         requestLongitude.clear();
                         requestLatitude.clear();

                         if(objects.size() > 0 ){
                             for(ParseObject object: objects) {
                                 ParseGeoPoint requestLocation = (ParseGeoPoint) object.get("location");
                                 if (requestLocation != null) {
                                     Double distanceIsMiles = parseGeoPoint.distanceInMilesTo(requestLocation);
                                     Double distanceOneDP = (double) Math.round(distanceIsMiles * 10) / 10;
                                     if (!request.contains(distanceOneDP.toString() + " miles")) {
                                         request.add(distanceOneDP.toString() + " miles");
                                         requestLatitude.add(requestLocation.getLatitude());
                                         requestLongitude.add(requestLocation.getLongitude());
                                         username.add(object.getString("username"));
                                     }
                                 }
                             }
                         }
                     }else{
                         request.add("No active request nearby:( ");
                     }

                     arrayAdapter.notifyDataSetChanged();
                 }
             });

         }
    }

    // request permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateListView(lastKnownLocation);
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_request);
        setTitle("Nearby Requests");

        requestUserListView = findViewById(R.id.userRequestListView);

        request.clear();
        request.add("Getting nearby requests.........");
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, request){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView)super.getView(position, convertView, parent);
                textView.setTextColor(Color.MAGENTA);
                return textView;
            }
        };
        requestUserListView.setAdapter(arrayAdapter);
        requestUserListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(ViewRequestActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (requestLatitude.size() > i && requestLongitude.size() > i && username.size() > i && lastKnownLocation != null) {

                        Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
                        intent.putExtra("requestLatitude",requestLatitude.get(i));
                        intent.putExtra("requestLongitude", requestLongitude.get(i));
                        intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());
                        intent.putExtra("driverLongitude",lastKnownLocation.getLongitude());
                        intent.putExtra("username", username.get(i));

                        startActivity(intent);

                    }
                }
            }

        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListView(location);
                ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        if(Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }else{
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(lastKnownLocation != null){}
                updateListView(lastKnownLocation);
            }
        }


    }
}