package com.tobyatherton.travelbuddy.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.tobyatherton.travelbuddy.travelbuddy.Util.localDBUtil;

import java.io.IOException;
import java.util.ArrayList;

public class ShowSaves extends AppCompatActivity implements OnMapReadyCallback {
    ListView myList;
    Button backButton;
    CheckBox chkMspType;
    SupportMapFragment mapFrag;
    final localDBUtil db = new localDBUtil(this);
    ArrayAdapter<String> adapter;
    JourneyUtil ju = null;

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_show_saves);
        setUpAdapter(db.getAllSaves());

        registerForContextMenu(myList);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.statsMap);
        mapFrag.getMapAsync(this);

        if(ju == null) {
            ju = ju.getInstance();
        }

        //THIS CODE MAY BE USELESS SORT OUT LISTVIEW PROBLEMS
        myList.setOnTouchListener(new View.OnTouchListener() {
            // Setting on Touch Listener for handling the touch inside ScrollView
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //used when scrollview is in xml shouldnt use scrollviews with listview
                // Disallow the touch request for parent scroll on touch of child view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String startstr = "";
                String endstr = "";
                //Toast.makeText(getApplicationContext(), parent.getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
                startstr = parent.getItemAtPosition(position).toString();
                endstr = parent.getItemAtPosition(position).toString();

                //Pattern p = Pattern.compile("^(\\d+\\.?\\d*),\\s?(\\d+\\.?\\d*)$");

/*                Matcher mstart = p.matcher(startstr);
                if (mstart.matches()) {
                    startstr = Double.parseDouble(mstart.group(1)) + ", " + Double.parseDouble(mstart.group(2));
                }
                Matcher mend = p.matcher(startstr);
                if (mend.matches()) {
                    endstr = Double.parseDouble(mend.group(1)) + ", " + Double.parseDouble(mend.group(2));
                }*/

                startstr = startstr.substring(startstr.indexOf("("), startstr.indexOf(")")).trim();
                endstr = endstr.substring(endstr.lastIndexOf("("), endstr.lastIndexOf(")")).trim();

                ju.setStart(startstr);
                ju.setEnd(endstr);
/*
                String[] split = str.split(",");
                String lon = split[0];
                String lat = split[1];
*/
                if(chkMspType.isChecked()) {
                    if (ju != null) { //show single journey in main map
                        try {
                            //js.readSingleJourney(startstr, endstr);
                            Intent imap = new Intent(ShowSaves.this, MapsActivity.class);
                            imap.putExtra("readSingleJourney", "readSingleJourneyOnly");
                            ShowSaves.this.startActivity(imap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else { //draw in local map
                    String start = ju.getStart();
                    String end = ju.getEnd();
                    try {
                        drawHistoricalLinesLocal(ju.readSingleJourney(start, end), R.color.linecolourLow);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //Intent intent = new Intent(MainActivity.this, SendMessage.class);
                //String message = "abc";
                //intent.putExtra(EXTRA_MESSAGE, message);
                //startActivity(intent);
            }
        });

        setListViewHeightBasedOnChildren(myList);

        chkMspType = (CheckBox) findViewById(R.id.checkBox);


        backButton=(Button) findViewById(R.id.backButton);

        backButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                try {
                    Intent backIntent = new Intent(ShowSaves.this, MapsActivity.class); //should be mainActivity
                    startActivity(backIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Select An Action");
        menu.add(0, v.getId(), 0, "Delete Save");//groupId, itemId, order, title
        menu.add(0, v.getId(), 0, "Delete all Saves");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        Object data = adapter.getItem(info.position);
        String saveData = data.toString().substring(data.toString().lastIndexOf(": ") + 2);
        saveData = saveData.replace("\n", "");
        if(item.getTitle() == "Delete Save"){
            db.deleteSave(saveData);
            Toast.makeText(getApplicationContext(),"Save: "+saveData+" has been deleted",Toast.LENGTH_LONG).show();
            setUpAdapter(db.getAllSaves());
        }
        else if(item.getTitle()=="Delete all Saves"){
            db.deleteAllUsers();
            Toast.makeText(getApplicationContext(),"All saves have been deleted",Toast.LENGTH_LONG).show();
            setUpAdapter(db.getAllSaves());
        }else{
            return false;
        }
        return true;
    }

    public void getData() {
        //test.add(db.getAllSaves());
        /*test.add("one");
        test.add("two");
        test.add("three");
        test.add("four");
        test.add("five");
        test.add("six");
        test.add("seven");
        test.add("eight");
        test.add("nine");
        test.add("ten");*/

    }

    public void setUpAdapter(ArrayList<String> data) {

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data);

        myList=(ListView) findViewById(R.id.list);

        myList.setAdapter(adapter);
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, RelativeLayout.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void drawHistoricalLinesLocal(ArrayList<LatLng> points, int colour) {
        PolylineOptions optionsHistorical = null;
        mMap.clear(); //clears all drawn lines on map

        optionsHistorical = new PolylineOptions().width(5).color(this.getResources().getColor(colour)).geodesic(true);
        for (int i = 0; i < points.size(); i++) { //adds the points to an arraylist
            LatLng point = points.get(i);
            optionsHistorical.add(point);
        }
        mMap.addPolyline(optionsHistorical);
    }

}
