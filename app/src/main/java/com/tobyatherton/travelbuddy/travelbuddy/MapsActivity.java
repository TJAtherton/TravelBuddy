package com.tobyatherton.travelbuddy.travelbuddy;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
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
import com.tobyatherton.travelbuddy.travelbuddy.ARCamera.ARCameraActivity;
import com.tobyatherton.travelbuddy.travelbuddy.Social.FriendList;
import com.tobyatherton.travelbuddy.travelbuddy.Social.FriendsRequests;
import com.tobyatherton.travelbuddy.travelbuddy.Social.ShareActivity;
import com.tobyatherton.travelbuddy.travelbuddy.Util.localDBUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static android.location.Criteria.ACCURACY_FINE;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static com.tobyatherton.travelbuddy.travelbuddy.GPSService.MY_PREFS_NAME;


public class MapsActivity extends AppCompatActivity implements GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {


    private TextView textView;
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker,ARMarkerPin;

    private GoogleMap mMap; //remove this line
    private ArrayList<LatLng> points; //added
    Polyline line; //added
    private LocationRequest mLocationRequest;
    private static final String TAG = "MapsActivity";
    private static final long INTERVAL = 1000 * 60 * 1; //1 minute
    private static final long FASTEST_INTERVAL = 1000 * 60 * 1; // 1 minute
    private static final float SMALLEST_DISPLACEMENT = 0.25F; //quarter of a meter
    private PolylineOptions polylineOptions;

    private String provider;
    private float speedStore;
    ArrayList<Marker> markersToClearFromMap;
    ArrayList<Circle> circlesToClearFromMap;
    ArrayList<Polyline> linesToClearFromMap;
    MarkerOptions startMarkerOptions;

    private FirebaseAuth mAuth;

    Location radiusLoc = null;

    //https://www.codeproject.com/Articles/1172545/Firebase-Authentication-by-Example-with-Android

    //time arraylist store
    //ArrayList<Date> timeAndDate; //datetime

    localDBUtil db;

    Calendar calendar;
    SimpleDateFormat sdf;

    String dtJourneyStart;
    String dtJourneyEnd;
    Location startLocation;
    Location endLocation;

    float[] distance = new float[2]; //should be local perhaps


    //Singleton reference
    JourneyUtil ju;

    //admob
    private InterstitialAd mInterstitialAd;
    private AdView mAdView;
    //end admob


    Intent iGPSService;
    Circle mCircle, mInnerMarkerCircle;
    public static final String EXTRA_LOCATION = "EXTRA_LOCATION";

    IntentFilter filter;

    String[] fileNames;

    //test
    PolylineOptions optionsHistorical;

    //A ProgressDialog object
    private ProgressDialog pd;

    boolean mBounded;
    GPSService mService;
    java.text.DateFormat localeDateFormat;

    boolean hasExtras = false;

    //test
    LocationManager mLocationManager = null;
    int LOCATION_INTERVAL = 0;
    float LOCATION_DISTANCE = 0;
    //com.passgen.tobyatherton.pointtest.LocationListener[] mLocationListeners = null;
    //end test


    public static final String CAMERA_IMAGE_BUCKET_NAME =
            Environment.getExternalStorageDirectory().toString()
                    + "/DCIM/Camera";
    public static final String CAMERA_IMAGE_BUCKET_ID =
            getBucketId(CAMERA_IMAGE_BUCKET_NAME);
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**
     * Matches code in MediaProvider.computeBucketValues. Should be a common
     * function.
     */
    public static String getBucketId(String path) {
        return String.valueOf(path.toLowerCase().hashCode());
    }

    //===

    TextView addressTextView;

    private final BroadcastReceiver LocationUpdateReceiver = new BroadcastReceiver() {

        /**
         * Receives broadcast from GPS class/service.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            //Bundle extras = intent.getBundleExtra("com.passgen.tobyatherton.pointtest.ACTION_RECEIVE_LOCATION");

            Location location = (Location) extras.get(MapsActivity.EXTRA_LOCATION);

            //test
            //if (mService.isGPSGood()) {

                //Do draw lines and markers based on recieved location
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                mLastLocation = location;

                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //createLocationRequest(); //this maybe?

                //Place current location marker
                LatLng latLng = new LatLng(latitude, longitude);

                if (location.getSpeed() != 0) { //if the user is moving
                    if(!hasExtras) { //if it is not a single journey
                        //if(withinRadius(radiusLoc)) { //fix this to get location only within radius
                            points.add(latLng); //added
                            redrawLine(points); //added
                        //}
                        addressTextView.setText("GPS Accuracy: " + location.getAccuracy() + "\n" + "GPS Bearing: " + location.getBearing() + "\n" + "Logging position");
                    /*}else if(withinRadius(radiusLoc)) { //fix this to get location only within radius
                        points.add(latLng); //added
                        redrawLine(points, R.color.linecolour); //not sure if this is needed*/
                    }
                    if (mLastLocation != null) {
                        radiusLoc = mLastLocation;
                    }
                    addressTextView.setText("GPS Accuracy: " + location.getAccuracy() + "\n" + "GPS Bearing: " + location.getBearing() + "\n" + "Logging position");
                } else {
                    addressTextView.setText("GPS Accuracy: " + location.getAccuracy() + "\n" + "GPS Bearing: " + location.getBearing() + "\n" + "Logging Stopped");
                }

                //addressTextView.setText(Html.fromHtml(mService.getStrGPSStatus()));

                //localeDateFormat.format(calendar.getTime());


                //sdf.format(calendar.getTime());
                //timeAndDate.add(sdf.format(calendar.getTime())); //added
                //timeAndDate.add(calendar.getTime());
                //

                //test
                //addressTextView.setText("" + checkArrayForDistance(points));
                //end test


                //Toast.makeText(MapsActivity.this, "Current Speed: "+ location.getSpeed() + "", Toast.LENGTH_SHORT).show();
                speedStore = location.getSpeed();

                if(mGoogleMap != null) { //if map  has loaded

                    MarkerOptions CurrentMarkerOptions = new MarkerOptions();
                    CurrentMarkerOptions.position(latLng);
                    CurrentMarkerOptions.title("I am here");
                    CurrentMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.start));
                    mCurrLocationMarker = mGoogleMap.addMarker(CurrentMarkerOptions);
                    //add to clear from map list
                    markersToClearFromMap.add(mCurrLocationMarker);
                }

            //============update currentLocation in FireBase if the user has internet
            //this should be updated so that it only updates firebase if user is paired with another
            if(isOnline()) { //may be better in the network listener
                updateUserLocation(mLastLocation);
            }
            //============

            //}


        }
    };

    @Override
    public boolean onMarkerClick(Marker marker) {

        if (marker.equals(ARMarkerPin))
        {
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(MapsActivity.this);
            //builderSingle.setIcon(R.drawable.logo);
            builderSingle.setTitle("Choose an option:-");

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MapsActivity.this, android.R.layout.select_dialog_singlechoice);
            arrayAdapter.add("View this Marker");
            arrayAdapter.add("Rate this Marker");


            builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String itemName = arrayAdapter.getItem(which);
                    if (itemName.equals("View this Marker")) {
                        //give options dialog for placing points of interest

                        drawMarkerWithCloseCircle(mService.getmLocLastLocation(), 5.0);

                        if (withinCloseRadius(ARMarkerPin.getPosition().latitude, ARMarkerPin.getPosition().longitude)) {//check if AR marker is within close radius of user
                            //Toast.makeText(getBaseContext(), "Inside, distance from center: " + distance[0] + " radius: " + mCircle.getRadius(), Toast.LENGTH_LONG).show();
                            //start ARCameraActivity
                            try {
                                Intent imap = new Intent(MapsActivity.this, ARCameraActivity.class);
                                imap.putExtra("markerViewed", "userViewingMarker");
                                MapsActivity.this.startActivity(imap);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(getBaseContext(), "Outside, distance from center: " + distance[0] + " radius: " + mCircle.getRadius(), Toast.LENGTH_LONG).show();
                        }

                        //if(radiusDialog(mLastLocation)

                    } else if(itemName.equals("Rate this Marker")) {
                        //allow user to rate AR Markers

                    }
                }
            });
            builderSingle.show();

        }
        return false;
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
/*    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Maps Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }*/

    public class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (GPSService.PAUSE_ACTION.equals(action)) {
                Toast.makeText(context, "PAUSE CALLED", Toast.LENGTH_SHORT).show();
            } else if (GPSService.START_ACTION.equals(action)) {
                Toast.makeText(context, "START CALLED", Toast.LENGTH_SHORT).show();
            } else if (GPSService.STOP_ACTION.equals(action)) {
                Toast.makeText(context, "STOP CALLED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);



        sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss a");
        calendar = Calendar.getInstance();

        db = new localDBUtil(this);

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");//"ca-app-pub-3053069510627936~4712702024");

        //implement junit testing
        //https://developer.android.com/training/testing/unit-testing/local-unit-tests.html#build

        // Create the InterstitialAd and set the adUnitId.
        mInterstitialAd = new InterstitialAd(this);
        // Defined in res/values/strings.xml
        mInterstitialAd.setAdUnitId(getString(R.string.ad_unit_id));

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                //resume journey
            }
        });


     /*   mAdView = (AdView) findViewById(R.id.adView);
        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.SMART_BANNER);
        adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");

        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
            }
        });*/


        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when the ad is displayed.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the interstitial ad is closed.
            }
        });
        //end admob


        //test
        filter = new IntentFilter("com.tobyatherton.travelbuddy.travelbuddy.ACTION_RECEIVE_LOCATION");
        registerReceiver(LocationUpdateReceiver, filter);
        //end test

        //test

        textView = (TextView) findViewById(R.id.addressTextView);
        //mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        points = new ArrayList<LatLng>(); //added
        markersToClearFromMap = new ArrayList<Marker>(); //added
        linesToClearFromMap = new ArrayList<Polyline>();
        circlesToClearFromMap = new ArrayList<Circle>();

        //
        //timeAndDate = new ArrayList<Date>();
        //calendar = Calendar.getInstance();
        //sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        //

        getSupportActionBar().setTitle("Travel Mapper");

        addressTextView = (TextView) findViewById(R.id.addressTextView);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            if (getIntent().getStringExtra("readSingleJourney").equals("readSingleJourneyOnly")) {
                Toast.makeText(this, "has extras", Toast.LENGTH_LONG).show();
                hasExtras = true;
            } else {
                Toast.makeText(this, "as normal", Toast.LENGTH_LONG).show();
                hasExtras = false;
            }
        }


        //mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //if(locationManager!=null) {
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, this);
        //}
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000,0, (android.location.LocationListener) this);

//Choosing the best criteria depending on what is available.
        //Criteria criteria = new Criteria();
        //provider = mLocationManager.getBestProvider(criteria, false);
        //get singleton util class
        ju = JourneyUtil.getInstance();

//provider = LocationManager.GPS_PROVIDER; // We want to use the GPS


// Initialize the location fields
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //mLastLocation = mLocationManager.getLastKnownLocation(provider);

        //Toast.makeText(MapsActivity.this, "Lat: " + mLastLocation.getLatitude() + " Lon: " + mLastLocation.getLongitude(), Toast.LENGTH_LONG).show();


        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        //get all user location of users that have allowed this and if the current user allows this
        // get score updates
        //final String str = mUser.getUid().toString();

        mAuth = FirebaseAuth.getInstance(); //get firebase instance
        final FirebaseUser mUser = mAuth.getCurrentUser();// get current user

        //scoreBoard = (ListView) view.findViewById(R.id.score_list_view);

        final ArrayList<String> users = new ArrayList<String>();

        //arrayAdapter = new ArrayAdapter<String>(this,
                //android.R.layout.simple_list_item_1, users);
        //scoreBoard.setAdapter(arrayAdapter);

        final ArrayList<Double> locationArr = new ArrayList<Double>();

        DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference().child("users");

        usernamesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                try {
                    for (com.google.firebase.database.DataSnapshot indavidualsnapshot : dataSnapshot.getChildren()) {
                        // Get the username
                        String username = indavidualsnapshot.child("name").getValue().toString();

                        String journeyShare = indavidualsnapshot.child("name").getValue().toString();

                        // Get the number of journies
                        //String numOfJournies = indavidualsnapshot.child("numberOfJournies").getValue().toString();

                        //replace this with check based on location not locale
                        String currentCountry = "";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            currentCountry = MapsActivity.this.getResources().getConfiguration().getLocales().get(0).getDisplayCountry();
                        } else {
                            currentCountry = MapsActivity.this.getResources().getConfiguration().locale.getDisplayCountry();
                        }

                        String info = "Journies Travelled: , Current country: " + currentCountry;//"Journies Travelled: " + numOfJournies + ", Current country: " + currentCountry;

                        if(journeyShare.equals("1")) { //if the user shares their location
                            locationArr.clear(); //clear the array from old values
                            for (com.google.firebase.database.DataSnapshot snapshot : indavidualsnapshot.child("userLocation").getChildren()) {
                                //mLastLocation = (Location) dataSnapshot.getValue(Location.class);
                                locationArr.add(snapshot.getValue(Double.class));
                            }
                            Location loc = new Location("provider");

                            if (!locationArr.isEmpty()) {
                                loc.setLatitude(locationArr.get(0));
                                loc.setLongitude(locationArr.get(1));
                            }

                            if ((!locationArr.isEmpty()) &&
                                    !(indavidualsnapshot.child("id").getValue().toString().equals(mUser.getUid().toString()))) { //if not current user
                                addMarker(loc, BitmapDescriptorFactory.HUE_GREEN, username, info);
                            } else {
                                //mLastLocation = loc; //add current user marker
                                addMarker(loc, BitmapDescriptorFactory.HUE_RED, username, info);
                            }
                            loc = null;
                            users.add("User: " + username + " " + info);
                        }
                        //arrayAdapter.notifyDataSetChanged();
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //end get users section
        //acceptRequests
        try {
            if ((isOnline()) && (requestsExist()[0])) {
                acceptRequests();
            } /*else {
                Toast.makeText(this, "Cannot connect to Travel Buddy online database", Toast.LENGTH_SHORT).show();
            }*/
        }catch (Exception e) {
            e.printStackTrace();
        }


        Button btnfindAdress = (Button) findViewById(R.id.findAdress);
        Button btnStartJourney = (Button) findViewById(R.id.startJourney);
        Button btnEndJourney = (Button) findViewById(R.id.endJourney);
        //Toast.makeText(MapsActivity.this, "click Done", Toast.LENGTH_LONG).show();

        btnfindAdress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mLocationManager.isProviderEnabled(GPS_PROVIDER) && mLocationManager.isProviderEnabled(GPS_PROVIDER)) {
                    //Toast.makeText(this, "GPS is Enabled in your devide", Toast.LENGTH_SHORT).show();
                    //getAddressBasedOnLocation(mLastLocation);
                } else if (mLocationManager.isProviderEnabled(GPS_PROVIDER)) {
                    showGPSDisabledAlertToUser();
                } else {
                    showGPSDisabledAlertToUser();
                    //Toast.makeText(MapsActivity.this, "GPS not enabled", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnStartJourney.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mLocationManager.isProviderEnabled(GPS_PROVIDER) || mLocationManager.isProviderEnabled(NETWORK_PROVIDER)) {
                    /*if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    if (mLocationManager != null) {
                        mLocationManager.requestLocationUpdates(
                                NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                                mLocationListeners[1]);
                        mLocationManager.requestLocationUpdates(GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                                mLocationListeners[0]);
                    }*/

                    //Choosing the best criteria depending on what is available.
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(ACCURACY_FINE); //sets accuracy to high

                    provider = mLocationManager.getBestProvider(criteria, false);

                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }

                    if (provider != null) {
                        mLastLocation = mLocationManager.getLastKnownLocation(provider);
                    } else {
                        mLastLocation = mLocationManager.getLastKnownLocation(GPS_PROVIDER);
                        if (mLastLocation == null) {
                            mLastLocation = mLocationManager.getLastKnownLocation(NETWORK_PROVIDER);
                        }
                        //mLastLocation = mLocationManager.getLastKnownLocation(PASSIVE_PROVIDER);
                    }


                    if (mLastLocation != null) { // && mService.isGPSGood()
                        centerMapOnMyLocation();
                        addMarker(mLastLocation);

                        //test 10/10/17
/*                        mGoogleMap.addCircle(new CircleOptions()
                                .center(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                                .radius(10000)
                                .strokeColor(Color.BLACK)
                                .fillColor(Color.CYAN)
                        );*/
                        //end test 10/10/17

                        addressTextView.setText("Tracking Started");
                        mService.setJourneyStarted(true);

                        //admod check
                        // Request a new ad if one isn't already loaded, hide the button, and kick off the timer.
                        if (!mInterstitialAd.isLoading() && !mInterstitialAd.isLoaded()) {
                            AdRequest adRequest = new AdRequest.Builder().build();
                            mInterstitialAd.loadAd(adRequest);
                        }
                        //end admob

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mService.createNotification();
                        }

                        dtJourneyStart = sdf.format(new Time(System.currentTimeMillis()).getTime());
                        startLocation = mLastLocation;
                        //Toast.makeText(MapsActivity.this, "click finished", Toast.LENGTH_LONG).show();

                    } else {
                        //addressTextView.setText("GPS not settled");
                        Toast.makeText(MapsActivity.this, "No GPS Lock made", Toast.LENGTH_LONG).show();
                        showGPSDisabledAlertToUser();
                    }

                } else if (!mLocationManager.isProviderEnabled(GPS_PROVIDER)) {
                    showGPSDisabledAlertToUser();
                } else if (!mLocationManager.isProviderEnabled(NETWORK_PROVIDER)) {
                    Toast.makeText(MapsActivity.this, "Network signal not available", Toast.LENGTH_LONG).show();
                } else {
                    //showGPSDisabledAlertToUser();
                    Toast.makeText(MapsActivity.this, "Signal not available", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnEndJourney.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mLocationManager.isProviderEnabled(GPS_PROVIDER) || mLocationManager.isProviderEnabled(NETWORK_PROVIDER)) {
                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }

                    if (mLastLocation != null) {
                        LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                        MarkerOptions EndMarker = new MarkerOptions();
                        EndMarker.position(latLng);
                        EndMarker.title("End");
                        EndMarker.snippet("My Journey ended at: \n" ); //+ getAddressBasedOnLocation(mLastLocation)
                        EndMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                        mCurrLocationMarker = mGoogleMap.addMarker(EndMarker);

                        addressTextView.setText("Tracking Ended");
                        mService.setJourneyStarted(false);

                        dtJourneyEnd = sdf.format(new Time(System.currentTimeMillis()).getTime());

                        endLocation = mLastLocation;
                        //test
                        //Intent iGPSService = new Intent(MapsActivity.this, GPSService.class);
                        //stopService(iGPSService);
                        //test

                        //admob
                        if (mInterstitialAd.isLoaded()) {
                            showInterstitial();
                        } else {
                            Log.d("TAG", "The interstitial wasn't loaded yet.");
                        }
                        //end admob

                        //save Journey line
                        //if (line.getPoints() != null) {
                        if (mService.getPoints() != null) {
                            //make read separate method call?
                            //redrawLine(ju.Initialise(line.getPoints(), timeAndDate));
                            AlertDialog.Builder builderWriteWait = new AlertDialog.Builder(MapsActivity.this);
                            AlertDialog alertDialog = builderWriteWait.create();
                            //builderSingle.setIcon(R.drawable.cast_ic_notification_pause);
                            alertDialog.setTitle("Processing");

                            alertDialog.setMessage("Please wait...");
                            alertDialog.show();
                            //writes to file
                            //ju.Initialise(line.getPoints(), timeAndDate);


                            //bind test need to draw line based on mService.getPoints()
                            ju.Initialise(mService.getPoints(), mService.gettimeAndDate());
                            //bind test
                            alertDialog.dismiss();


                            Date dtJS = null;
                            Date dtJE = null;
                            try {
                                dtJS = sdf.parse(dtJourneyStart);
                                dtJE = sdf.parse(dtJourneyEnd);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            LatLng startLatlng = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
                            LatLng endLatlng = new LatLng(endLocation.getLatitude(), endLocation.getLongitude());

                            db.addSave("Journey started at: " +dtJourneyStart+ "\n" + "Journey ended at: " +dtJourneyEnd+ "\n" +"Journey duration: "+ printDifference(dtJS,dtJE)+"", "\n" + "Start Location: " +startLatlng+ "\n End Location: " +endLatlng);

                        }
                    } else {
                        Toast.makeText(MapsActivity.this, "No GPS Lock made", Toast.LENGTH_LONG).show();
                    }

                    mLocationManager.removeUpdates(mService);


                } else if (!mLocationManager.isProviderEnabled(GPS_PROVIDER)) {
                    showGPSDisabledAlertToUser();
                } else if (!mLocationManager.isProviderEnabled(NETWORK_PROVIDER)) {
                    Toast.makeText(MapsActivity.this, "Network signal not available", Toast.LENGTH_LONG).show();
                } else {
                    //showGPSDisabledAlertToUser();
                    Toast.makeText(MapsActivity.this, "Signal not available", Toast.LENGTH_LONG).show();
                }
                mService.stopService(iGPSService);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mService.closeNotification();
                }
            }
        });
    }

/*    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.getStringExtra("readSingleJourney").equals("readSingleJourneyOnly")){
            Toast.makeText(this,"has extras",Toast.LENGTH_LONG).show();
            hasExtras = true;
        } else {
            Toast.makeText(this,"as normal",Toast.LENGTH_LONG).show();
            hasExtras = false;
        }
    }*/

    //https://stackoverflow.com/questions/21285161/android-difference-between-two-dates
    //1 minute = 60 seconds
//1 hour = 60 x 60 = 3600
//1 day = 3600 x 24 = 86400
    public String printDifference(Date startDate, Date endDate) {
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        System.out.println("startDate : " + startDate);
        System.out.println("endDate : "+ endDate);
        System.out.println("different : " + different);

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        //System.out.printf("%d days, %d hours, %d minutes, %d seconds%n", elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds);
        return ""+ elapsedDays+ " Days, "+ elapsedHours+" Hours, "+ elapsedMinutes+" Minutes, "+ elapsedSeconds+ " Seconds, ";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stats:
                Intent iStats = new Intent(this, Stats.class);
                this.startActivity(iStats);
                this.finish();
                break;
            case R.id.profile:
                Intent iUserProfile = new Intent(this, UserProfile.class);
                this.startActivity(iUserProfile);
                this.finish();
                break;
            case R.id.leaderboard:
                Intent iLeaderboard = new Intent(this, ShowSaves.class);
                this.startActivity(iLeaderboard);
                this.finish();
                break;
            case R.id.share:
                Intent iShare = new Intent(this, ShareActivity.class);
                this.startActivity(iShare);
                this.finish();
                break;
            case R.id.Map_Hybrid:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                if(!item.isChecked()) {
                    item.setChecked(true);
                } else {
                    item.setChecked(false);
                }
                break;
            case R.id.Map_Normal:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                if(!item.isChecked()) {
                    item.setChecked(true);
                } else {
                    item.setChecked(false);
                }
                break;
            case R.id.Map_Satellite:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                if(!item.isChecked()) {
                    item.setChecked(true);
                } else {
                    item.setChecked(false);
                }
                break;
            case R.id.invite:
                //show invite dialog

                break;
            case R.id.addFriend:
                //show add friend dialog
                addFriendDialog();
                break;

            case R.id.FriendsList:
                //show friends list
                Intent iFriendsList = new Intent(this, FriendList.class);
                this.startActivity(iFriendsList);
                this.finish();
                break;
            case R.id.FriendRequests:
                //show friends requests
                Intent iFriendRequests = new Intent(this, FriendsRequests.class);
                this.startActivity(iFriendRequests);
                this.finish();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (mLocationManager != null) {
            mLocationManager.requestLocationUpdates(
                    NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mService);
            mLocationManager.requestLocationUpdates(GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mService);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //mLocationManager.removeUpdates(mService);
        //mLocationManager.removeUpdates(mService);
    }

    private int metersPerSecondToMPH(float speed)
    {
        //USE This if not in UK
        //for km/hour
        //double a = 3.6 * (event);
        //int kmhSpeed = (int) (Math.round(speed));


        //for mile/hour
        double lSpeed = 2.23694 * (speed);
        return (int) (Math.round(lSpeed));


    }

    private void redrawLine(ArrayList<LatLng> points) {
        if (mCurrLocationMarker != null) {
            //removes all markers stored
            for (Marker marker : markersToClearFromMap) {
                marker.remove();
            }
            markersToClearFromMap.clear();

            //removes all lines stored
            for (Polyline line : linesToClearFromMap) {
                line.remove();
            }
            linesToClearFromMap.clear();
            //mGoogleMap.clear();  //clears all Markers and Polylines
        }
        PolylineOptions options = null;
        //if (mService.getLineStatusColour() == 0) {
            options = new PolylineOptions().width(5).color(this.getResources().getColor(R.color.linecolour)).geodesic(true);

            //DYNAMIC LINE COLOUR TEST
            if (mLastLocation != null) {
                //addMarker(mLastLocation); //add Marker in current position
                float currentSpeed = metersPerSecondToMPH(mLastLocation.getSpeed());
                if(currentSpeed < 10) {
                    options.color(this.getResources().getColor(R.color.linecolour));
                } else if((currentSpeed > 10) && (currentSpeed < 30)) {
                    options.color(this.getResources().getColor(R.color.linecolourLow));
                } else if((currentSpeed > 30) && (currentSpeed < 70)) {
                    options.color(this.getResources().getColor(R.color.linecolourMedium));
                } else if(currentSpeed > 70) {
                    options.color(this.getResources().getColor(R.color.linecolourHigh));
                }
            }
            //END TEST
        //} else {
            //options = new PolylineOptions().width(5).color(mService.getLineStatusColour()).geodesic(true);
        //}
        for (int i = 0; i < points.size(); i++) { //adds the points to an arraylist
            LatLng point = points.get(i);
            options.add(point);
            //Toast.makeText(this, "option: " + i + " added", Toast.LENGTH_LONG).show();
        }

        //mGoogleMap.addPolyline(options);
        line = mGoogleMap.addPolyline(options); //add Polyline
        linesToClearFromMap.add(line);
    }

    private void drawHistoricalLines(ArrayList<LatLng> points, int colour) {
        Polyline line2;
        if (mCurrLocationMarker != null) {
            //removes all markers stored
            for (Marker marker : markersToClearFromMap) {
                marker.remove();
            }
            markersToClearFromMap.clear();

            //removes all lines stored
            for (Polyline line : linesToClearFromMap) {
                line.remove();
            }
            linesToClearFromMap.clear();
            //mGoogleMap.clear();  //clears all Markers and Polylines
        }
        optionsHistorical = new PolylineOptions().width(5).color(this.getResources().getColor(colour)).geodesic(true);
        for (int i = 0; i < points.size(); i++) { //adds the points to an arraylist
            LatLng point = points.get(i);
            optionsHistorical.add(point);
            //Toast.makeText(this, "option: " + i + " added", Toast.LENGTH_LONG).show();
        }
        //options.addAll(points);
        if (mLastLocation != null) {
            //addMarker(mLastLocation); //add Marker in current position
        }
        mGoogleMap.addPolyline(optionsHistorical);
        //test
        line2 = mGoogleMap.addPolyline(optionsHistorical); //add Polyline
        //test
        //linesToClearFromMap.add(line2);
    }

    private void addMarker(Location location) {
        startMarkerOptions = new MarkerOptions();
        mGoogleMap.addMarker(startMarkerOptions
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .title("Start")
                .snippet("My Journey started at: \n" ) //+ getAddressBasedOnLocation(location)
                .rotation((float) -15.0)

                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        );
        //add markeroptions to clear list if needed here
        //if (i == 0) { //if there is a marker
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 20));
        /*mGoogleMap.addCircle(new CircleOptions().center(new LatLng(location.getLatitude(), location.getLongitude()))
                .radius(5000)
                .strokeColor(Color.RED)
                .fillColor(Color.RED));*/
        //}
    }

    //addmarker for users
    private void addMarker(Location location, float colour, String pName, String pInfo) {
        startMarkerOptions = new MarkerOptions();
        mGoogleMap.addMarker(startMarkerOptions
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .title(pName)
                .snippet(pInfo)
                .rotation((float) -15.0)
                .icon(BitmapDescriptorFactory.defaultMarker(colour))
        );
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 20));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setOnMapLongClickListener(this);
        //make map markers clickable
        mGoogleMap.setOnMarkerClickListener(this);
        //==========================

        //Initialize Google Play Services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                //buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                //checkLocationPermission();
            }
        } else {
            //buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }
        //test


         AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
             ArrayList<MarkerOptions> markers = new ArrayList<>();
            @Override
            protected void onPreExecute() {
                pd = new ProgressDialog(MapsActivity.this);
                pd.setTitle("Just a Moment...");
                pd.setMessage("Loading your journeys");
                pd.setCancelable(false);
                pd.setIndeterminate(true);
                pd.show();
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                try {
                    //Do something...
                   try {
                        //while (ju.readFile() != null) {
                        //drawHistoricalLines(ju.readFile());
                       ArrayList<MarkerOptions> picsArr = new ArrayList<MarkerOptions>(findPicsOnRoute());
                       if (!picsArr.isEmpty()) {
                           markers.addAll(picsArr); //add pictures to the journey
                       }
                        //}
                    } catch (Exception E) {
                        E.printStackTrace();
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (pd!=null) {
                    pd.dismiss();
                    //add loaded markers to map
                    try {
                        if(!hasExtras) { //test
                            //draw all previous journeys
                            drawHistoricalLines(ju.readFile(), R.color.linecolourHigh);
                        } else {
                            //draw single journey
                            String start = ju.getStart();
                            String end = ju.getEnd();
                            drawHistoricalLines(ju.readSingleJourney(start, end), R.color.linecolourLow);
                            Toast.makeText(getBaseContext(), "ELSE HIT!", Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //if (!markers.isEmpty()) {
                    if(!hasExtras) {
                        ArrayList<MarkerOptions> picsOnJourney = findPicsOnRoute();
                        if(picsOnJourney != null) {
                            if (!picsOnJourney.isEmpty()) {
                                //for (MarkerOptions m : markers) {
                                for (MarkerOptions m : picsOnJourney) {
                                    mGoogleMap.addMarker(m);
                                }
                            } else {
                                Toast.makeText(getBaseContext(), "Markers array not populated.", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            }

        };
        task.execute((Void[])null);

        // Enable / Disable my location button
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        //test
        /*try {
            //while (ju.readFile() != null) {
            drawHistoricalLines(ju.readFile());
            //findPicsOnRoute();
            //}
        } catch (Exception E) {
            E.printStackTrace();
        }*/

    }

    @Override
    protected void onDestroy() {
        if (pd != null) {
            pd.dismiss();
            try {
                mGoogleMap.addPolyline(optionsHistorical);
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        super.onDestroy();
        //TEST, maybe in service itself?
        unregisterReceiver(LocationUpdateReceiver);
        if(mService != null) {
            mService.stopService(iGPSService);
        }
    }

    @Override
    public void onBackPressed() {
        /*Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);*/
        //super.onBackPressed();
        new AlertDialog.Builder(this)
                .setTitle("Go to main menu?")
                .setMessage("If a journey is still active the data may be lost unless ended.\nAre you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        MapsActivity.super.onBackPressed();
                    }
                }).create().show();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        //add marker
        /*MarkerOptions marker=new MarkerOptions();
        marker.position(latLng);
        mGoogleMap.addMarker(marker);
        // settin polyline in the map
        polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(5);
        points.add(latLng);
        polylineOptions.addAll(points);
        mGoogleMap.addPolyline(polylineOptions);*/

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        //mGoogleMap.clear();
        //points.clear();

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MapsActivity.this);
        //builderSingle.setIcon(R.drawable.logo);
        builderSingle.setTitle("Select Action:-");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MapsActivity.this, android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add("Add Point of Interest");
        arrayAdapter.add("Remove all Journeys");
        arrayAdapter.add("My average Speed");
        arrayAdapter.add("Current country");
        arrayAdapter.add("Get AR Markers within radius of me");

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String itemName = arrayAdapter.getItem(which);
                if (itemName.equals("Add Point of Interest")) {
                    //give options dialog for placing points of interest
                } else if (itemName.equals("Remove all Journeys")) {
                    mGoogleMap.clear();
                    points.clear();
                } else if (itemName.equals("My average Speed")) {
                    AlertDialog.Builder builderAvgSpeed = new AlertDialog.Builder(MapsActivity.this);
                    //builderSingle.setIcon(R.drawable.cast_ic_notification_pause);
                    builderAvgSpeed.setTitle("Average Speed: ");

                    final ArrayAdapter<String> avgspeedAdapter = new ArrayAdapter<String>(MapsActivity.this, android.R.layout.select_dialog_singlechoice);
                    avgspeedAdapter.add(averageOutSpeed(mLastLocation.getSpeed()));

                    builderAvgSpeed.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builderAvgSpeed.show();

                } else if (itemName.equals("Current country")) {
                    //show country in dialog
                    showCurrentCountryDialog();
                } else if (itemName.equals("Get AR Markers within radius of me")) {
                    showARMarkerWithinRAdius();
                }
            }
        });
        builderSingle.show();

    }


    private boolean withinRadius(double latitude, double longitude) {
        //if(loc != null) {
            Location.distanceBetween(latitude, longitude,
                    mCircle.getCenter().latitude, mCircle.getCenter().longitude, distance);

            if (distance[0] > mCircle.getRadius()) {
                return false;
                //Toast.makeText(getBaseContext(), "Outside, distance from center: " + distance[0] + " radius: " + mCircle.getRadius(), Toast.LENGTH_LONG).show();
            } else {
                return true;
                //Toast.makeText(getBaseContext(), "Inside, distance from center: " + distance[0] + " radius: " + mCircle.getRadius(), Toast.LENGTH_LONG).show();
            }
        //} else {
            //return false;
        //}
    }

    private boolean withinCloseRadius(double latitude, double longitude) {
        //if(loc != null) {
        Location.distanceBetween(latitude, longitude,
                mInnerMarkerCircle.getCenter().latitude, mInnerMarkerCircle.getCenter().longitude, distance);

        if (distance[0] > mInnerMarkerCircle.getRadius()) {
            return false;
            //Toast.makeText(getBaseContext(), "Outside, distance from center: " + distance[0] + " radius: " + mCircle.getRadius(), Toast.LENGTH_LONG).show();
        } else {
            return true;
            //Toast.makeText(getBaseContext(), "Inside, distance from center: " + distance[0] + " radius: " + mCircle.getRadius(), Toast.LENGTH_LONG).show();
        }
        //} else {
        //return false;
        //}
    }


    private void drawMarkerWithCircle(Location loc, double pRadius) {
        LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());
        double radiusInMeters = 0.0;
        int strokeColor = 0xffff0000; //red outline
        int shadeColor = 0x44ff0000; //opaque red fill

        radiusInMeters = pRadius;

        CircleOptions circleOptions = new CircleOptions().center(latlng).radius(radiusInMeters).fillColor(shadeColor).strokeColor(strokeColor).strokeWidth(8);
        mCircle = mGoogleMap.addCircle(circleOptions);
    }

    private void drawMarkerWithCloseCircle(Location loc, double pRadius) {
        LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());
        double radiusInMeters = 0.0;
        int strokeColor = R.color.circleBorder; //red outline
        int shadeColor = R.color.linecolourLow; //opaque red fill

        radiusInMeters = pRadius;

        CircleOptions circleOptions = new CircleOptions().center(latlng).radius(radiusInMeters).fillColor(shadeColor).strokeColor(strokeColor).strokeWidth(8);
        mInnerMarkerCircle = mGoogleMap.addCircle(circleOptions);
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Goto Settings Page To Enable GPS",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void centerMapOnMyLocation() {
        LatLng myLocation = null;
        if (mLastLocation != null) {
            myLocation = new LatLng(mLastLocation.getLatitude(),
                    mLastLocation.getLongitude());
        }
        if (myLocation != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        }
    }

    private String averageOutSpeed(float speed) {
        float fTmp;
        float highspeed;
        float lowspeed;
        String sReturnVal;
        highspeed = 0;
        lowspeed = 0;
        sReturnVal = "";

        if (speed > speedStore) {
            highspeed = speed;
        } else if (speed < speedStore) {
            lowspeed = speed;
        }

        if (highspeed != 0 && lowspeed != 0) {
            fTmp = highspeed / lowspeed;
            return sReturnVal = "" + fTmp;
        }
        return "";
    }

    //perhaps move this method to the journeystore class to write to file
    private float checkArrayForDistance(ArrayList<LatLng> ppoints) { //add points as parameter
        Location loc1 = new Location("");
        Location loc2 = new Location("");
        float distanceInMeters = 0;
        //sort distance of line based on arraylist here
        for (int i = 0; i < ppoints.size(); i++) {
            for (int j = i + 1; j < ppoints.size(); j++) {
                // compare list.get(i) and list.get(j)
                loc1.setLongitude(ppoints.get(i).longitude);
                loc1.setLatitude(ppoints.get(i).latitude);

                loc2.setLongitude(ppoints.get(j).longitude);
                loc2.setLatitude(ppoints.get(j).latitude);

                //adds the current distance to old distance to make overall distance from file. //returns in meters
                distanceInMeters += distanceInMeters = loc1.distanceTo(loc2) / 1000; //in km
            }
        }
        if (distanceInMeters != 0) {
            //distanceInMeters = loc1.distanceTo(loc2);
            return distanceInMeters;
        } else {
            //distanceInMeters = 0;
            return 0;
        }
    }

    private ArrayList<MarkerOptions> findPicsOnRoute() {

        //make these params
        ArrayList<MarkerOptions> imgMarkers = new ArrayList<MarkerOptions>();
        //ArrayList<Date> historicalDateTimes = new ArrayList<Date>(ju.getDateTime());

        //ArrayList<Date> galleryDateTimes = new ArrayList<Date>(getCameraImages());

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(80, 80, conf);
        //Canvas canvas1 = new Canvas(bmp);

        // paint defines the text color, stroke width and size
        Paint colour = new Paint();
        Rect rect = new Rect(0, 0, 80, 80);
        RectF rectF = new RectF(rect); //test
        //colour.setTextSize(35);
        colour.setAntiAlias(true);
        colour.setFilterBitmap(true);
        colour.setDither(true);
        //colour.setColor(Color.BLACK);

        //long diffInMs  = 0;
        //long diffInSec = 0;

        Date picDate = null;
        Date historicalDate;

    try {
        for (int i = 0; i < ju.getDateTime().size(); i++) {
            historicalDate = ju.getDateTime().get(i);
            //for (Date date : getCameraImages()) {
            for (int ii = 0; ii < getCameraImageDates().size(); ii++) {
                picDate = new Date(getCameraImageDates().get(ii).lastModified());

                //diffInMs  = historicalDate.getTime() - picDate.getTime();
                //diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);
                //if (!(diffInSec > 20)) {
                if (historicalDate.getTime() == picDate.getTime()) {

                    //for(int iii = 0; iii < fileNames.length; iii++) {
                    //addressTextView.setText("There was an image match!");
                    //BitmapDescriptor icon = BitmapDescriptorFactory.fromFile(pictureFiles.get(ii).getAbsolutePath());
                    // modify canvas
                    Canvas picOnRouteCanvas = new Canvas(bmp);
                    //test
                    Bitmap bitmap = BitmapFactory.decodeFile(getCameraImageDates().get(ii).getPath());

                    bitmap = Bitmap.createScaledBitmap(bitmap, 80, 80, true);
                    //end test
                    //canvas1.drawCircle(canvas1.getWidth() / 2, canvas1.getHeight() / 2, 100, colour);

                    picOnRouteCanvas.drawOval(rectF, colour); //remove for square image
                    colour.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                    picOnRouteCanvas.drawBitmap(bitmap, rect, rect, colour);

                    colour.setStyle(Paint.Style.STROKE);
                    colour.setColor(Color.BLACK);
                    picOnRouteCanvas.drawRect(rect, colour);

                    bitmap.recycle();
                    //canvas1.drawBitmap(BitmapFactory.decodeFile(getCameraImageDates().get(ii).getPath()), rect, rect, colour); //previously rect for x an y //BitmapFactory.decodeFile(getCameraImageDates().get(ii).getPath()+"/"+fileNames[iii]) BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher)//code works but is very very slow must be optimised
                    //canvas1.drawText("Picture on journey", 30, 40, colour);

                    //http://anusreeanair.blogspot.co.uk/2012/09/android-fetch-images-from-sdcard-and.html
                    //http://android-er.blogspot.co.uk/2012/07/gridview-loading-photos-from-sd-card.html
                    //https://www.google.co.uk/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=how+to+make+a+gallery+from+images+on+phone+app+in+android&*

// add marker to Map
                    try {
                        MarkerOptions marker = new MarkerOptions().position(ju.readFile().get(i)) //gets the locations of where it should be drawn o the line and then adds the marker
                                .icon(BitmapDescriptorFactory.fromBitmap(bmp))
                                .anchor(0.5f, 1);
                        imgMarkers.add(marker);
                       /* mGoogleMap.addMarker(new MarkerOptions().position(ju.readFile().get(i)) //gets the locations of where it should be drawn o the line and then adds the marker
                                .icon(BitmapDescriptorFactory.fromBitmap(bmp))
                                // Specifies the anchor to be at a particular point in the marker image.
                                .anchor(0.5f, 1));*/
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //}
            }
            return imgMarkers;
        }
    }catch(Exception e) {
        e.printStackTrace();
    }
        return null;
    }

    public ArrayList<File> getCameraImageDates() {
        ArrayList<File> result = new ArrayList<File>();
        String dateString;
        ExifInterface intf = null;
        File dcim = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM+"/Camera");
        File[] pics = dcim.listFiles();
        //SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        try {
            //test
            if (dcim.exists()) {
                fileNames = dcim.list();
            }

            /*intf = new ExifInterface(path);
            if (intf != null && dcim != null) {
                dateString = intf.getAttribute(ExifInterface.TAG_DATETIME);
                //Date lastModDate = dateFormat.parse(dateString);
                Toast.makeText(this, "Hit Exinterface",Toast.LENGTH_LONG).show();
                //Log.i("Dated : " + dateString.toString()); //Dispaly dateString. You can do/use it your own way
            } else */if (pics != null) {
                for (File pic : pics) {
                    //Date lastModDate = new Date(pic.lastModified());

                    //dateString = sdf.format(lastModDate);;
                    //passes the formatted string to date
                    //lastModDate = sdf.parse(dateString);


                    //Toast.makeText(this, "for hit",Toast.LENGTH_LONG).show();
                    //Log.i("Dated : " + lastModDate.toString());//Dispaly lastModDate. You can do/use it your own way

                    result.add(pic);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //bind test

    @Override
    protected void onStart() {
        super.onStart();
        Intent mIntent = new Intent(this, GPSService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //Toast.makeText(MapsActivity.this, "Service is disconnected", Toast.LENGTH_LONG).show();
            mBounded = false;
            mService = null;
            //mService.stopService(iGPSService);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Toast.makeText(MapsActivity.this, "Service is connected", Toast.LENGTH_LONG).show();
            GPSService.LocalBinder bindService = (GPSService.LocalBinder) service;
            mService = bindService.getService();
            mService.setMapsContext(MapsActivity.this);
            mBounded = true;

            //test
            iGPSService = new Intent(MapsActivity.this, GPSService.class);
            //startService(iGPSService);
            mService.startService(iGPSService);
            //test
            mLocationManager = mService.getmLocationManager();
            LOCATION_INTERVAL = mService.getLOCATION_INTERVAL();
            LOCATION_DISTANCE = mService.getLOCATION_DISTANCE();

            //use this to get location?
            SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
            String spGPSLocation = prefs.getString("gpsLocation", "");
            if (!spGPSLocation.equalsIgnoreCase("")) {
                //mLastLocation = prefs.getAll("gpsLocation", "")//spGPSLocation;  /* Edit the value here*/
            }


            try {

                if (Build.VERSION.SDK_INT >= 23 && mService != null) {
                    if (!checkIfAlreadyhavePermission()) {
                        requestForSpecificPermission();
                    } else {
                        mLocationManager.requestLocationUpdates(NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mService);
                    }
                } else if (mService != null) {
                    mLocationManager.requestLocationUpdates(
                            NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                            mService);
                }

            } catch (SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "network provider does not exist, " + ex.getMessage());
            }

            try {

                //do all this in mapsactivity
                if (Build.VERSION.SDK_INT >= 23 && mService != null) {
                    if (!checkIfAlreadyhavePermission()) {
                        requestForSpecificPermission();
                    } else {
                        mLocationManager.requestLocationUpdates(
                                GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                                mService);
                    }
                } else if (mService != null) {
                    mLocationManager.requestLocationUpdates(
                            GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                            mService);
                }

            } catch (SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "gps provider does not exist " + ex.getMessage());
            }
            //end test

        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
            //try to run this?
            //mService.performOnBackgroundThread();
        }
    }
    //bind test

    //test area
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //granted
                } else {
                    //not granted
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
    }

    public boolean checkIfAlreadyhavePermission() {
        int resultAFL = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int resultACL = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int resultRES = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            resultRES = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        int resultWES = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return resultAFL == PackageManager.PERMISSION_GRANTED && resultACL == PackageManager.PERMISSION_GRANTED && resultRES == PackageManager.PERMISSION_GRANTED && resultWES == PackageManager.PERMISSION_GRANTED;
    }

    //perhaps put this in the service

    /*if (Build.VERSION.SDK_INT >= 23) {
        if (!checkIfAlreadyhavePermission()) {
            requestForSpecificPermission();
        }
    }*/
    //end test area

    //test

    //add this to the functionality
    private void setupInviteDialogList() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MapsActivity.this);
        builderSingle.setTitle("Select A Friend to Invite:-");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MapsActivity.this, android.R.layout.select_dialog_singlechoice, db.getAllSaves());
        //populate list with friends of user
        arrayAdapter.add("friend");

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = arrayAdapter.getItem(which);
                AlertDialog.Builder builderInner = new AlertDialog.Builder(MapsActivity.this);
                builderInner.setMessage(strName);
                builderInner.setTitle("Your Selected Item is");
                builderInner.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        dialog.dismiss();
                    }
                });
                builderInner.show();
            }
        });
        builderSingle.show();
    }

    private void addFriendDialog() {
        LayoutInflater li = LayoutInflater.from(MapsActivity.this);
        View dialogView = li.inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
        // set title
        alertDialogBuilder.setTitle("Add a Friend");
        // set custom_dialog.xml to alertdialog builder
        alertDialogBuilder.setView(dialogView);
        final EditText userInput = (EditText) dialogView.findViewById(R.id.editText);
        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //find the searched for user in firebase
                                getAndSendRequest(userInput.getText().toString());
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

    private void radiusDialog() {
        LayoutInflater li = LayoutInflater.from(MapsActivity.this);
        View dialogView = li.inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
        // set title
        alertDialogBuilder.setTitle("Show AR arkers near you");
        // set custom_dialog.xml to alertdialog builder
        alertDialogBuilder.setView(dialogView);

        final EditText userInput = (EditText) dialogView.findViewById(R.id.editText);
        userInput.setHint("Enter radius search for markers (in meters)");
        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //find the searched for user in firebase

                                String input = userInput.getText().toString();
                                if (!input.equals("")) {
                                    double meters = 0;
                                    meters = Double.parseDouble(input);
                                    drawMarkerWithCircle(mService.getmLocLastLocation(), meters);
                                } else {
                                    drawMarkerWithCircle(mService.getmLocLastLocation(), 10.0);
                                }

                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();

    }

    private void showARMarkerWithinRAdius() {
        LayoutInflater li = LayoutInflater.from(MapsActivity.this);
        View dialogView = li.inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
        // set title
        alertDialogBuilder.setTitle("Show AR arkers near you");
        // set custom_dialog.xml to alertdialog builder
        alertDialogBuilder.setView(dialogView);

        final EditText userInput = (EditText) dialogView.findViewById(R.id.editText);
        userInput.setHint("Enter radius search for markers (in meters)");
        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //find the searched for user in firebase

                                String input = userInput.getText().toString();
                                if (!input.equals("")) {
                                    double meters = 0;
                                    meters = Double.parseDouble(input);
                                    drawMarkerWithCircle(mService.getmLocLastLocation(), meters);
                                } else {
                                    drawMarkerWithCircle(mService.getmLocLastLocation(), 500.0);
                                }

                                if(isOnline()) {

                                    getARMarkerLocations(); //populate the arraylist
                                }

                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();

    }

    private void showCurrentCountryDialog() {
        LayoutInflater li = LayoutInflater.from(MapsActivity.this);
        View dialogView = li.inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
        // set title
        alertDialogBuilder.setTitle("Custom Dialog");
        // set custom_dialog.xml to alertdialog builder
        alertDialogBuilder.setView(dialogView);
        final EditText userInput = (EditText) dialogView.findViewById(R.id.editText);
        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String teststr = "";
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    teststr = MapsActivity.this.getResources().getConfiguration().getLocales().get(0).getDisplayCountry();
                                } else {
                                    teststr = MapsActivity.this.getResources().getConfiguration().locale.getDisplayCountry();
                                }
                                userInput.setText("" + teststr + "");
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        // create the dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show the dialog
        alertDialog.show();
    }

    private void getAndSendRequest(final String pUserInputValue) {

        //final ArrayList<String> foundUser = new ArrayList<String>();

        final DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference().child("users");

        usernamesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                for (com.google.firebase.database.DataSnapshot indavidualsnapshot : dataSnapshot.getChildren()) {

                    try {
                        // Get the username
                        String username = indavidualsnapshot.child("name").getValue().toString();

                        // Get the user phone number
                        String phoneNumber = indavidualsnapshot.child("phoneNumber").getValue().toString();

                        //store the found users id for later use
                        String id = indavidualsnapshot.child("id").getValue().toString();


                        if ((pUserInputValue.equals(username)) || (pUserInputValue.equals(phoneNumber))) {
                            //user has been found

                            //instead add the below line in the method that updates firebase and pass all info in here into array
                         /*foundUser.add(id);
                        foundUser.add(username);
                        foundUser.add(phoneNumber);*/

                            usernamesRef.child(id).child("FriendsRequests").push().setValue(mAuth.getCurrentUser().getUid()); //update the request in the requested user
                            db.addFriendRequest(id, username, phoneNumber); //add friendRequest to local DB
                            Toast.makeText(MapsActivity.this, "Request Semt", Toast.LENGTH_SHORT).show();

                            //db.addFriend(username, phoneNumber); //add friend to local DB
                        } else {
                            Toast.makeText(MapsActivity.this, "ERROR: Request not Semt", Toast.LENGTH_SHORT).show();
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        // return the found users (if any)
        //return foundUser;
    }

    private void acceptRequests() {

        final DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference().child("users");

        usernamesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    for (com.google.firebase.database.DataSnapshot indavidualsnapshot : dataSnapshot.getChildren()) {
                        // Get the username
                        String username = indavidualsnapshot.child("name").getValue().toString();

                        // Get the user phone number
                        //String phoneNumber = indavidualsnapshot.child("phoneNumber").getValue().toString();

                        //store the found users id for later use
                        String id = indavidualsnapshot.child("id").getValue().toString();

                        //String RequestedID = indavidualsnapshot.child("AcceptedFriendsRequests").getValue().toString();

                        if (id.equals(mAuth.getCurrentUser().getUid())) {

                            for (com.google.firebase.database.DataSnapshot acceptedRequests : indavidualsnapshot.child(id).child("AcceptedFriendsRequests").getChildren()) {

                                acceptedRequests.getRef().child(acceptedRequests.getKey()).removeValue();

                                //updateFirebase with all friends as well as searched user
                                //updateFireBaseFriendList(db.getAllFriends(), inviteUserData.get(1), inviteUserData.get(2));

                                usernamesRef.child(id).child("Friends").push().setValue(acceptedRequests.getValue().toString());
                            }


                        }
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //add this to functionality
    public void updateFireBaseFriendList(ArrayList<String> pFriends,String pUsername, String pPhoneNumber) {
        // Update the friends list
        //need to add list to Firebase
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(mAuth.getCurrentUser().getUid());
        if(pFriends.isEmpty()) {
            usersRef.child("Friends").setValue("You have no friends :(");
        } else {
            db.addFriend(pUsername, pPhoneNumber); //add latest found friend to local DB
            usersRef.child("Friends").setValue(pFriends); //update vurrent user friends
        }
    }

    public void updateUserLocation(Location pUserLocation) {
        // Update the users score
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(mAuth.getCurrentUser().getUid());
        if(pUserLocation == null) {
            usersRef.child("userLocation").setValue("Cannot get location of player");
        } else {
            LatLng latlng = new LatLng(pUserLocation.getLatitude(),pUserLocation.getLongitude()); //put latitude and longitude into latlng object
            usersRef.child("userLocation").setValue(latlng);
        }
    }

    protected boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null)
        {
            NetworkInfo netInfos = connectivityManager.getActiveNetworkInfo();
            if(netInfos != null)
            {
                return netInfos.isConnected();
            }
        }
        return false;
    }

    private boolean[] requestsExist() {
        final DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference().child("users");

        final boolean[] returnVal = {false};

        usernamesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data: dataSnapshot.getChildren()){
                    if (data.child("AcceptedFriendsRequests").hasChildren()) {
                        returnVal[0] = true;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
        return returnVal;
    }

    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise toast and restart the game.
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            Toast.makeText(this, "Ad did not load", Toast.LENGTH_SHORT).show();
            //resume journey
        }
    }

    private void getARMarkerLocations() {

        final DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference().child("users");
        final ArrayList<LatLng> ARLocations = new ArrayList<LatLng>();
        final ArrayList<Double> locationArr = new ArrayList<Double>();

        usernamesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    for (DataSnapshot indavidualsnapshot : dataSnapshot.getChildren()) {

                        // Get the username
                        String username = indavidualsnapshot.child("name").getValue().toString();

                        // Get the user phone number
                        //String phoneNumber = indavidualsnapshot.child("phoneNumber").getValue().toString();

                        //store the found users id for later use
                        String id = indavidualsnapshot.child("id").getValue().toString();


                        if (!id.equals(mAuth.getCurrentUser().getUid())) {

                            if(indavidualsnapshot.child("ARMarkerLocation").exists()) {

                                locationArr.clear(); //clear the array from old values
                                for (DataSnapshot markerLocations : indavidualsnapshot.child("ARMarkerLocation").getChildren()) {

                                    //mLastLocation = (Location) dataSnapshot.getValue(Location.class);
                                    locationArr.add(markerLocations.getValue(Double.class));


                                }
                                ARLocations.add(new LatLng(locationArr.get(0), locationArr.get(1)));
                            }
                        }
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.getMessage();
            }
        });

        //========================= test =============================

        Handler handler = new Handler(); //perhaps remove this handler
        handler.postDelayed(new Runnable() {
            public void run() {
                for(LatLng ARLocation : ARLocations) {

                    if (withinRadius(ARLocation.latitude, ARLocation.longitude)) {//check if AR marker is within radius of user
                        //Toast.makeText(getBaseContext(), "Inside, distance from center: " + distance[0] + " radius: " + mCircle.getRadius(), Toast.LENGTH_LONG).show();
                        if (!ARLocations.isEmpty()) { //if not empty
                            createARMarker(ARLocation, "test username","Some test info");
                        } else {
                            //mLastLocation = loc; //add current user marker
                            Toast.makeText(MapsActivity.this,"Error adding AR Marker",Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getBaseContext(), "Outside, distance from center: " + distance[0] + " radius: " + mCircle.getRadius(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }, 5000);

    }

    private void createARMarker(LatLng location, String pName, String pInfo) {
        MarkerOptions arMarkerOptions = new MarkerOptions();
        ARMarkerPin = mGoogleMap.addMarker(arMarkerOptions
                .position(location)
                .title("Left here by: " + pName)
                .snippet(pInfo)
                .rotation((float) -15.0)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ar_marker))
        );
    }

}
