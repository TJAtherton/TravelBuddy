package com.tobyatherton.travelbuddy.travelbuddy;

/**
 * Created by Toby Atherton on 23/03/2017.
 */

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.os.Handler;

import com.google.android.gms.maps.model.LatLng;
import com.tobyatherton.travelbuddy.travelbuddy.Util.localDBUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;

//http://stackoverflow.com/questions/28535703/best-way-to-get-user-gps-location-in-background-in-android
public class GPSService extends Service implements android.location.LocationListener, SensorEventListener {
    private static final String TAG = "GPSService";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 0;
    private static final float LOCATION_DISTANCE = 1f;

    private IBinder mBinder = new LocalBinder();

    private MapsActivity mMapsContext = null; //test


    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    //private int NOTIFICATION = R.string.cast_notification_connecting_message;



    private Location mLocLastLocation = null;
    private double mLastLocationMillis;


    private boolean isGPSGood;
    public static final String MY_PREFS_NAME = "gpsPrefs";

    //time arraylist store
    private ArrayList<Date> timeAndDate; //datetime
    private Calendar calendar;
    private SimpleDateFormat sdf;
    private ArrayList<LatLng> points; //added

    protected Handler handler;

    private NotificationManager nm;

    private final int NOTIFICATIONID = 10;

    private int lineStatusColour = 0;
    private String strGPSStatus = "";


    private localDBUtil db = null;

    private SensorManager sensorManager;
    private Sensor mStepSensor;
    private double mSteps;



    private boolean journeyStarted = false; //test

    //java.text.DateFormat localeDateFormat;


    //perhaps use access methods
    public static final String PAUSE_ACTION = "com.tobyatherton.travelbuddy.travelbuddy.PAUSE_ACTION";
    public static final String STOP_ACTION = "com.tobyatherton.travelbuddy.travelbuddy.STOP_ACTION";
    public static final String START_ACTION = "com.tobyatherton.travelbuddy.travelbuddy.START_ACTION";


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public double getmSteps() {
        return mSteps;
    }


    public void setJourneyStarted(boolean journeyStarted) {
        this.journeyStarted = journeyStarted;
    }

    public boolean isJourneyStarted() {
        return journeyStarted;
    }

    //SENSORS for step counter
    @Override
    public void onSensorChanged(SensorEvent event) {
        int iUserID = 1; //temp
        mSteps = mSteps + event.values[0];
        if (journeyStarted) {
            db.updateSpecificCol(iUserID, "higheststepsonjourney", String.valueOf(mSteps)); //db calls onsensor change bad for processing
        }
        db.updateSpecificCol(iUserID, "totalsteps", String.valueOf(mSteps));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*@Override
    public void onGpsStatusChanged(int i) {
        boolean isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < (LOCATION_INTERVAL * 2);
        switch (i) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                if (mLocLastLocation != null)
                    isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < (LOCATION_INTERVAL * 2);

                if (isGPSFix) { // A fix has been acquired.
                    // Do something.
                    isGPSGood = true;
                } else { // The fix has been lost.
                    // Do something.
                    isGPSGood = false;
                }

                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                // Do something.
                isGPSFix = true;
                isGPSGood = true;
                break;
        }

    }*/

    public class LocalBinder extends Binder { //move this to it's own class
        public GPSService getService() {
            return GPSService.this;
        }
    }

/*    public ArrayList<LatLng> getLLPoints() {
        if (mLocationListeners[1].getPoints().size() != 0) {
            return mLocationListeners[1].getPoints();
        } else if (mLocationListeners[2].getPoints().size() != 0) {
            return mLocationListeners[2].getPoints();
        }
        return null;
    }*/



 /*   LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };*/


/*    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }*/

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");

        initializeLocationManager();

        //Log.e(TAG, "LocationListener " + provider);
        points = new ArrayList<LatLng>(); //added
        timeAndDate = new ArrayList<Date>();
        //mLocLastLocation = new Location(provider);
        calendar = Calendar.getInstance();
        sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss a");
        db = new localDBUtil(this); //added
        //localeDateFormat = android.text.format.DateFormat.getDateFormat(this); //get date format based on where user is

        //test
        sensorManager = (SensorManager)
                this.getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null)
        {
            mStepSensor =
                    sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR); //Get the sensor manager to detect steps
            sensorManager.registerListener(this, mStepSensor, SensorManager.SENSOR_DELAY_NORMAL); //register the listener to listen for steps from sensor
        }
        //end test

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                //showNotification();
                //TEST
                if (ActivityCompat.checkSelfPermission(GPSService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(GPSService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mLocationManager.requestLocationUpdates(NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, GPSService.this);
                mLocationManager.requestLocationUpdates(GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, GPSService.this);
                //END TEST
// write your code to post content on server
            }
        });
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            //for (int i = 0; i < mLocationListeners.length; i++) {
            try {
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
                mLocationManager.removeUpdates(this);
                Log.e(TAG, "removed updates");
            } catch (Exception ex) {
                Log.i(TAG, "fail to remove location listners, ignore", ex);
            }
            //}
        }
    }

    public Location getmLocLastLocation() {
        return mLocLastLocation;
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Log.e(TAG, "initialized LocationManager");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void createNotification() {
        Notification.Builder notif;
        notif = new Notification.Builder(getApplicationContext());
        notif.setSmallIcon(R.mipmap.ic_launcher);
        notif.setContentTitle("Journey in progress");
        notif.setContentText("Options").setSmallIcon(R.mipmap.ic_launcher);

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Prepare intent which is triggered if the
        // notification is selected
        //Intent intent = new Intent(this, MapsActivity.class);
        //PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        Intent IntentPause = new Intent(this, MapsActivity.class);
        IntentPause.setAction(PAUSE_ACTION);
        PendingIntent pendingIntentPause = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), IntentPause, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.addAction(R.mipmap.ic_launcher, "Pause Journey", pendingIntentPause);



        Intent IntentStop = new Intent(this, MapsActivity.class);
        IntentStop.setAction(STOP_ACTION);
        PendingIntent pendingIntentStop = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), IntentStop, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.addAction(R.mipmap.ic_launcher, "End Journey", pendingIntentStop);

        Intent IntentStart = new Intent(this, MapsActivity.class);
        IntentStart.setAction(START_ACTION);
        PendingIntent pendingIntentStart = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), IntentStart, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.addAction(R.mipmap.ic_launcher, "Start Journey", pendingIntentStart).build();

        // Build notification
        // Actions are just fake
        /*Notification noti = new Notification.Builder(this)
                .setContentTitle("Journey in progress")
                .setContentText("Options").setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pIntent)
                .addAction(R.mipmap.ic_launcher, "Pause Journey", pendingIntentPause);
                .addAction(R.mipmap.ic_launcher, "End Journey", pendingIntentStop);
                .addAction(R.mipmap.ic_launcher, "Start Journey", pendingIntentStart);
                //.addAction(R.mipmap.ic_launcher, "Pause Journey", pIntent)
                //.addAction(R.mipmap.ic_launcher, "End Journey", pIntent).build();
        // hide the notification after its selected*/
        //noti.flags |= Notification.FLAG_AUTO_CANCEL;

        //notificationManager.notify(0, noti);

        nm.notify(NOTIFICATIONID, notif.getNotification());

    }

    public void closeNotification() {
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATIONID);
    }

    public void setMapsContext(MapsActivity context) {
        mMapsContext = context;
    }

    public LocationManager getmLocationManager() {
        return mLocationManager;
    }

    public int getLOCATION_INTERVAL() {
        return LOCATION_INTERVAL;
    }

    public float getLOCATION_DISTANCE() {
        return LOCATION_DISTANCE;
    }

    /*public LocationListener[] getmLocationListeners() {
        return mLocationListeners;
    }*/

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();



        if (location == null) return;

        mLastLocationMillis = SystemClock.elapsedRealtime();


        mLocLastLocation = location;

        //Place current location marker
        LatLng latLng = new LatLng(latitude, longitude);

        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("gpsLatitude", ""+latitude);
        editor.putString("gpsLongitude", ""+longitude);
        editor.putString("gpsLocation", ""+location);
        editor.apply();

        points.add(latLng); //added

        //sdf.format(calendar.getTime());
        Date currentDate = new Date();
        try {
            String currentDateStr = sdf.format(currentDate);
            currentDate = sdf.parse(currentDateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        timeAndDate.add(currentDate); //added

        location.getAltitude();
        if(location.hasAltitude()) {
            try {
                sortAltitude(location);//test
            }catch (Exception e) {
               e.printStackTrace();
            }

        }


        Intent toBroadcast = new Intent("com.tobyatherton.travelbuddy.travelbuddy.ACTION_RECEIVE_LOCATION");
        toBroadcast.putExtra(MapsActivity.EXTRA_LOCATION,location);
        this.sendBroadcast(toBroadcast);

        //redrawLine(points); //added

        Log.e(TAG, "onLocationChanged: " + location + "DATETIME: " + timeAndDate.get(0));

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //final String tvTxt = textView.getText().toString();

        switch (status) {
            case LocationProvider.AVAILABLE:
                lineStatusColour = this.getResources().getColor(R.color.linecolour);
                strGPSStatus = "GPS signal is Good";
                strGPSStatus = sortColour(strGPSStatus,"00FF00");
                //isGPSGood = true;
/*                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, "Message is deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Snackbar snackbar1 = Snackbar.make(coordinatorLayout, "Message is restored!", Snackbar.LENGTH_SHORT);
                                snackbar1.show();
                            }
                        });

                snackbar.show();*/
                break;
            case LocationProvider.OUT_OF_SERVICE:
                //isGPSGood = false;
                lineStatusColour = this.getResources().getColor(R.color.badlinecolour);
                strGPSStatus = "GPS signal is Unavailable";
                strGPSStatus = sortColour(strGPSStatus,"FF0000");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                //isGPSGood = false;
                lineStatusColour = this.getResources().getColor(R.color.linecolourMedium);
                strGPSStatus = "GPS signal is weak";
                strGPSStatus = sortColour(strGPSStatus,"FF6600");
                break;
        }
    }

    //http://stackoverflow.com/questions/15882932/change-string-colour-javamail
    public static String sortColour(String str, String color) {
        return "<font color=#'" + color + "'>" + str + "</font>";
    }

    public int getLineStatusColour() {
        return lineStatusColour;
    }

    public  String getStrGPSStatus(){
        return strGPSStatus;
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e(TAG, "onProviderDisabled: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e(TAG, "onProviderEnabled: " + provider);
    }

    public ArrayList<LatLng> getPoints() {
        if (points.size() != 0) {
            return points;
        }
        return null;
    }

    public ArrayList<Date> gettimeAndDate() {
        if (timeAndDate.size() != 0) {
            return timeAndDate;
        }
        return null;
    }

    public void sortAltitude(Location loc) {
        int iUserID = 1; //temp until userid is setup
        if ((loc.getAltitude() > Double.parseDouble(db.cursorToString(db.getSpecificCol(iUserID,"highestaltitude"),"highestaltitude")))) {
            //add to highest in local DB
            db.updateSpecificCol(iUserID, "highestaltitude", String.valueOf(loc.getAltitude()));
        } else if (loc.getAltitude() < Double.parseDouble(db.cursorToString(db.getSpecificCol(iUserID,"lowestaltitude"),"lowestaltitude"))) {
            //add to lowest in local DB
            db.updateSpecificCol(iUserID, "lowestaltitude", String.valueOf(loc.getAltitude()));
        } else {
            //do nothing and still show in stats
            //return String.valueOf(currentAltitude);
        }
        //return String.valueOf(currentAltitude);
    }

    public boolean isGPSGood() {
        return isGPSGood;
    }

}