package com.tobyatherton.travelbuddy.travelbuddy.Social;

/**
 * Created by sta971 on 10/01/2018.
 */

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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.tobyatherton.travelbuddy.travelbuddy.MapsActivity;
import com.tobyatherton.travelbuddy.travelbuddy.R;
import com.tobyatherton.travelbuddy.travelbuddy.Util.localDBUtil;

import java.util.ArrayList;


public class FriendList extends AppCompatActivity {
    ListView myList;
    Button backButton;
    final localDBUtil db = new localDBUtil(this);
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_friend_list);
        setUpAdapter(db.getAllFriends());

        registerForContextMenu(myList);

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


            }
        });

        setListViewHeightBasedOnChildren(myList);


        backButton=(Button) findViewById(R.id.backButton);

        backButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                try {
                    Intent backIntent = new Intent(FriendList.this, MapsActivity.class); //should be mainActivity
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
        menu.add(0, v.getId(), 0, "Delete Friend");//groupId, itemId, order, title
        menu.add(0, v.getId(), 0, "Delete all Friends");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        Object data = adapter.getItem(info.position);
        String friendData = data.toString().substring(data.toString().lastIndexOf(": ") + 2);
        friendData = friendData.replace("\n", "");
        if(item.getTitle() == "Delete Friend"){
            db.deleteFriend(friendData);
            Toast.makeText(getApplicationContext(),"Friend: "+friendData+" has been deleted",Toast.LENGTH_LONG).show();
            setUpAdapter(db.getAllFriends());
        }
        else if(item.getTitle()=="Delete all Friends"){
            db.deleteAllFriends();
            Toast.makeText(getApplicationContext(),"All saves have been deleted",Toast.LENGTH_LONG).show();
            setUpAdapter(db.getAllFriends());
        }else{
            return false;
        }
        return true;
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

}

