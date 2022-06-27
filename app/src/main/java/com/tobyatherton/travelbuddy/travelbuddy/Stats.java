package com.tobyatherton.travelbuddy.travelbuddy;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.tobyatherton.travelbuddy.travelbuddy.Util.localDBUtil;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Stats extends AppCompatActivity {

    private View mChart;


    private localDBUtil db;
    //private SensorUtil sensorUtil;

    private TextView currentTV;
    private TextView highestTV;
    private TextView lowestTV;
    private TextView currentAddress;
    private TextView currentSpeed;
    private TextView topSpeed;
    private TextView currentCountry;
    private TextView currentTemperatureTV;
    private TextView currentStepsTV;
    private TextView totalStepsTV;

    private IntentFilter filter;

    boolean mBounded;
    GPSService mService;

    Location location;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        currentTV = (TextView) findViewById(R.id.currentAltitude);
        highestTV = (TextView) findViewById(R.id.highestAltitude);
        lowestTV = (TextView) findViewById(R.id.lowestAltitude);
        currentAddress = (TextView) findViewById(R.id.currentAddress);
        currentSpeed = (TextView) findViewById(R.id.currentSpeed);
        topSpeed = (TextView) findViewById(R.id.topSpeed);
        currentCountry = (TextView) findViewById(R.id.currentCountry);
        currentTemperatureTV = (TextView) findViewById(R.id.currentTemperature);
        currentStepsTV = (TextView) findViewById(R.id.currentSteps);
        totalStepsTV = (TextView) findViewById(R.id.highestSteps);

        db = new localDBUtil(this);
        //sensorUtil = new SensorUtil(this);

        //sets the stats from local and online db
        //setupStats();
        db.insertUser("4 test road", "36","25","5","500"); //test
        filter = new IntentFilter("com.passgen.tobyatherton.pointtest.ACTION_RECEIVE_LOCATION");
        registerReceiver(LocationUpdateReceiver, filter);
        getHistoricalStats();

        spinner = (Spinner) findViewById(R.id.spinner);
        List<String> list = new ArrayList<String>();
        list.add("Select a chart"); // index 0
        list.add("Pie Chart"); // index 1
        list.add("Bar Chart"); // index 2
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(dataAdapter);
        spinner.setSelection(0);//sets the spinner to default value upon activity start, index 0
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // On selecting a spinner item
                if(parent.getItemAtPosition(position).toString().equalsIgnoreCase("Bar Chart")) {
                    //opens Bar chart
                    openBarChart(view);
                } else if(parent.getItemAtPosition(position).toString().equalsIgnoreCase("Pie Chart")){
                    //opens Pie chart
                    openPieChart();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }

        });

    }

    private final BroadcastReceiver LocationUpdateReceiver = new BroadcastReceiver() {

        /**
         * Receives broadcast from GPS class/service.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            //Bundle extras = intent.getBundleExtra("com.passgen.tobyatherton.pointtest.ACTION_RECEIVE_LOCATION");

            location = (Location) extras.get(MapsActivity.EXTRA_LOCATION);
            //test
            //if (mService.isGPSGood()) {

            //Do draw lines and markers based on recieved location
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            getLiveStats(location);



            //Place current location marker
            //LatLng latLng = new LatLng(latitude, longitude);

            //speedStore = location.getSpeed();



        }
    };

    private void getLiveStats(Location loc) {

        //db.insertUser("4 test road", "36","25","5","500");
        if (loc.hasAltitude()) {
            currentTV.setText(" Current altitude: " + Double.toString(loc.getAltitude()) + "");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            currentCountry.setText(" Current country: "+Stats.this.getResources().getConfiguration().getLocales().get(0).getDisplayCountry()+"");
        } else {
            currentCountry.setText(" Current country: "+Stats.this.getResources().getConfiguration().locale.getDisplayCountry()+"");
        }

        if (loc.hasSpeed()) {
            currentSpeed.setText(" Current speed: " + Double.toString(loc.getSpeed()) + "");
        }

        //currentTemperatureTV.setText(" Current temperature: "+Float.toString(sensorUtil.getmTemperaturef())+"");

        currentStepsTV.setText(" Current step count: "+mService.getmSteps()+"");

        //add top and slowest speed to local database
        if((isInternetAvailable())){
            currentAddress.setText(getAddressBasedOnLocation(loc));
        }

    }

    private void getHistoricalStats() {
        int iUserID = 1; //temp until user table setup
        highestTV.setText(" Highest altitude: "+db.cursorToString(db.getSpecificCol(iUserID,"highestaltitude"),"highestaltitude")+"");
        lowestTV.setText(" Lowest altitude: "+db.cursorToString(db.getSpecificCol(iUserID,"lowestaltitude"),"lowestaltitude")+"");
        topSpeed.setText(" Top speed: "+db.cursorToString(db.getSpecificCol(iUserID,"topspeed"),"topspeed")+"");
        totalStepsTV.setText(" Total steps: "+db.cursorToString(db.getSpecificCol(iUserID,"higheststepsonjourney"),"higheststepsonjourney")+"");
    }

    private void openPieChart(){

        // Pie Chart Section Names
        String[] code = new String[] {
                "Eclair & Older", "Froyo", "Gingerbread", "Honeycomb",
                "IceCream Sandwich", "Jelly Bean"
        };

        // Pie Chart Section Value
        double[] distribution = { 3.9, 12.9, 55.8, 1.9, 23.7, 1.8 } ;

        // Color of each Pie Chart Sections
        int[] colors = { Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.RED,
                Color.YELLOW };

        // Instantiating CategorySeries to plot Pie Chart
        CategorySeries distributionSeries = new CategorySeries(" Android version distribution as on October 1, 2012");
        for(int i=0 ;i < distribution.length;i++){
            // Adding a slice with its values and name to the Pie Chart
            distributionSeries.add(code[i], distribution[i]);
        }

        // Instantiating a renderer for the Pie Chart
        DefaultRenderer defaultRenderer  = new DefaultRenderer();
        for(int i = 0 ;i<distribution.length;i++){
            SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
            seriesRenderer.setColor(colors[i]);
            seriesRenderer.setDisplayChartValues(true);
            // Adding a renderer for a slice
            defaultRenderer.addSeriesRenderer(seriesRenderer);
        }

        defaultRenderer.setChartTitle("Android version distribution as on October 1, 2012 ");
        defaultRenderer.setChartTitleTextSize(20);
        defaultRenderer.setZoomButtonsVisible(true);

        // Creating an intent to plot bar chart using dataset and multipleRenderer
        //Intent intent = ChartFactory.getPieChartIntent(getBaseContext(), distributionSeries , defaultRenderer, "AChartEnginePieChartDemo");

        // Start Activity
        //startActivity(intent);

        // this part is used to display graph on the xml
        LinearLayout chartContainer = (LinearLayout) findViewById(R.id.chart);
        // remove any views before u paint the chart
        chartContainer.removeAllViews();
        // drawing pie chart
        mChart = ChartFactory.getPieChartView(getBaseContext(),
                distributionSeries, defaultRenderer);
        // adding the view to the linearlayout
        chartContainer.addView(mChart);

    }

    private void openBarChart(View view){

        int[] x = { 0,1,2,3,4,5,6,7 };
        int[] income = { 2000,2500,2700,3000,2800,3500,3700,3800};
        int[] expense = {2200, 2700, 2900, 2800, 2600, 3000, 3300, 3400 };
        String[] mMonth = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        // Creating an  XYSeries for Income
        XYSeries incomeSeries = new XYSeries("Income");
        // Creating an  XYSeries for Expense
        XYSeries expenseSeries = new XYSeries("Expense");
        // Adding data to Income and Expense Series
        for(int i=0;i<x.length;i++){
            incomeSeries.add(i,income[i]);
            expenseSeries.add(i,expense[i]);
        }

        // Creating a dataset to hold each series
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        // Adding Income Series to the dataset
        dataset.addSeries(incomeSeries);
        // Adding Expense Series to dataset
        dataset.addSeries(expenseSeries);

        // Creating XYSeriesRenderer to customize incomeSeries
        XYSeriesRenderer incomeRenderer = new XYSeriesRenderer();
        incomeRenderer.setColor(Color.rgb(130, 130, 230));
        incomeRenderer.setFillPoints(true);
        incomeRenderer.setLineWidth(2);
        incomeRenderer.setDisplayChartValues(true);

        // Creating XYSeriesRenderer to customize expenseSeries
        XYSeriesRenderer expenseRenderer = new XYSeriesRenderer();
        expenseRenderer.setColor(Color.rgb(220, 80, 80));
        expenseRenderer.setFillPoints(true);
        expenseRenderer.setLineWidth(2);
        expenseRenderer.setDisplayChartValues(true);

        // Creating a XYMultipleSeriesRenderer to customize the whole chart
        XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
        multiRenderer.setXLabels(0);
        multiRenderer.setChartTitle("Income vs Expense Chart");
        multiRenderer.setXTitle("Year 2012");
        multiRenderer.setYTitle("Amount in Dollars");
        multiRenderer.setZoomButtonsVisible(true);
        for(int i=0; i< x.length;i++){
            multiRenderer.addXTextLabel(i, mMonth[i]);
        }

        // Adding incomeRenderer and expenseRenderer to multipleRenderer
        // Note: The order of adding dataseries to dataset and renderers to multipleRenderer
        // should be same
        multiRenderer.addSeriesRenderer(incomeRenderer);
        multiRenderer.addSeriesRenderer(expenseRenderer);

        // Creating an intent to plot bar chart using dataset and multipleRenderer
        Intent intent = ChartFactory.getBarChartIntent(getBaseContext(), dataset, multiRenderer, BarChart.Type.DEFAULT);
        // Start Activity
        startActivity(intent);
    }

/*    private void openChart() {

        //Button btn_chart = (Button) findViewById(R.id.btn_chart);
        // Pie Chart Section Names
        String[] code = new String[] { "Froyo", "Gingerbread",
        "IceCream Sandwich", "Jelly Bean", "KitKat" };
        //String[] code = new String[] { "Found Results: " + 4, "Total: " };

        // Pie Chart Section Value btn_chart.getText.length();
        double[] distribution = { 1,2,3,4 };

        // Color of each Pie Chart Sections
        int[] colors = { Color.RED, Color.GREEN,Color.BLUE,Color.YELLOW };

        // Instantiating CategorySeries to plot Pie Chart
        CategorySeries distributionSeries = new CategorySeries(
                " Android version distribution as on October 1, 2012");
        for (int i = 0; i < distribution.length; i++) {
            // Adding a slice with its values and name to the Pie Chart
            distributionSeries.add(code[i], distribution[i]);
        }

        // Instantiating a renderer for the Pie Chart
        DefaultRenderer defaultRenderer = new DefaultRenderer();
        for (int i = 0; i < distribution.length; i++) {
            SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
            seriesRenderer.setColor(colors[i]);
            seriesRenderer.setDisplayChartValues(true);
            //Adding colors to the chart
            defaultRenderer.setBackgroundColor(Color.WHITE);
            defaultRenderer.setApplyBackgroundColor(true);
            // Adding a renderer for a slice
            defaultRenderer.addSeriesRenderer(seriesRenderer);


        }

        defaultRenderer
                .setChartTitle("Total countries travelled. ");
        defaultRenderer.setChartTitleTextSize(40);
        defaultRenderer.setZoomButtonsVisible(true);
        defaultRenderer.setLegendTextSize(30);
        defaultRenderer.setLabelsTextSize(30);


        // Creating an intent to plot bar chart using dataset and
        // multipleRenderer
        // Intent intent = ChartFactory.getPieChartIntent(getBaseContext(),
        // distributionSeries , defaultRenderer, "AChartEnginePieChartDemo");

        // Start Activity
        // startActivity(intent);

        // this part is used to display graph on the xml
        LinearLayout chartContainer = (LinearLayout) findViewById(R.id.chart);
        // remove any views before u paint the chart
        chartContainer.removeAllViews();
        // drawing pie chart
        mChart = ChartFactory.getPieChartView(getBaseContext(),
                distributionSeries, defaultRenderer);
        // adding the view to the linearlayout
        chartContainer.addView(mChart);

    }*/

    public boolean isInternetAvailable() {
        boolean isConnected = false;
        try {
            //InetAddress ipAddr = InetAddress.getByName("google.com"); //You can replace it with your name
            //return !ipAddr.equals("");
            ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        } catch (Exception e) {
            return false;
        }
        return isConnected;
    }

    private String getAddressBasedOnLocation(Location pLocation) { //normall has no return val
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        String strReturn = "";

        try {
            if (isInternetAvailable()) {
                addresses = geocoder.getFromLocation(pLocation.getLatitude(), pLocation.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL
                String premises = addresses.get(0).getPremises();
                String phone = addresses.get(0).getPhone();
                String url = addresses.get(0).getUrl();
                strReturn = "Country: " + country + "\nState: " + state + "\nCity: " + city + "\nKnown Name: " + knownName + "\nAddress: " + address + "\nPremises: " + premises + "\nPhone: " + phone + "\nPost Code: " + postalCode + "\nURL: " + url;
            } else {
                strReturn = "";
            }

            //addressTextView.setText("Country: " +  country + " State: " + state +  " City: " +  city + " Known Name: " + knownName + " Address: " + address + " Premises: " + premises + " Phone: " + phone + " Post Code: " + postalCode + " URL: " + url);
            //return strReturn;
            //Toast.makeText(this,  "Country: " +  country + " State: " + state +  " City: " +  city + " Known Name: " + knownName + " Address: " + address + " Post Code: " + postalCode, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strReturn;
    }

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
            mBounded = true;


        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
            unregisterReceiver(LocationUpdateReceiver);
            //TEST, maybe in service itself?
            //try to run this?
            //mService.performOnBackgroundThread();
        }
    }
    //bind test

}
