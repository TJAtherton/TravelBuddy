package com.tobyatherton.travelbuddy.travelbuddy.Social;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tobyatherton.travelbuddy.travelbuddy.MapsActivity;
import com.tobyatherton.travelbuddy.travelbuddy.R;
import com.tobyatherton.travelbuddy.travelbuddy.Util.localDBUtil;

import java.util.ArrayList;


public class FriendsRequests extends AppCompatActivity {
    ListView myList;
    Button backButton;
    final localDBUtil db = new localDBUtil(this);
    ArrayAdapter<String> adapter;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_friend_list);

        mAuth = FirebaseAuth.getInstance(); //get firebase instance

        getRequests(); //get request information

        setUpAdapter(db.getAllFriendsRequests());

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
                    Intent backIntent = new Intent(FriendsRequests.this, MapsActivity.class); //should be mainActivity
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
        menu.add(0, v.getId(), 0, "Delete Friend Request");//groupId, itemId, order, title
        menu.add(0, v.getId(), 0, "Delete all Friend Rquests");
        menu.add(0, v.getId(), 0, "Accept Friend Request");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        Object data = adapter.getItem(info.position);
        String requestData = data.toString();//.substring(data.toString().lastIndexOf(": ") + 2);
        requestData = requestData.replace("\n", "");
        if(item.getTitle() == "Delete Friend Request"){
            db.deleteFriend(requestData);
            Toast.makeText(getApplicationContext(),"Friend: "+requestData+" has been deleted",Toast.LENGTH_LONG).show();
            setUpAdapter(db.getAllFriends());
        }
        else if(item.getTitle()=="Delete all Friend Rquests"){
            db.deleteAllFriends();
            Toast.makeText(getApplicationContext(),"All saves have been deleted",Toast.LENGTH_LONG).show();
            setUpAdapter(db.getAllFriends());
        }
        else if(item.getTitle()=="Accept Friend Request"){
            String strRequestData = data.toString();
            String[] splitStr = strRequestData.split("\\s+");
            AcceptRequest(splitStr[5]);
            db.deleteFriend(requestData);
            Toast.makeText(getApplicationContext(),"Friend request accepted",Toast.LENGTH_LONG).show();
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

    private void getRequests() {

        //final ArrayList<String> foundUser = new ArrayList<String>();

        final DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference().child("users");

        usernamesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    for (com.google.firebase.database.DataSnapshot indavidualsnapshot : dataSnapshot.getChildren()) {
                        // Get the username
                        String username = indavidualsnapshot.child("name").getValue().toString();

                        // Get the user phone number
                        String phoneNumber = indavidualsnapshot.child("phoneNumber").getValue().toString();

                        //store the found users id for later use
                        String id = indavidualsnapshot.child("id").getValue().toString();

                        String currentID = mAuth.getCurrentUser().getUid();

                        String friendReq = "";
                        if(!id.equals(currentID)) {
                            friendReq = (String) indavidualsnapshot.child("FriendsRequests").getValue();
                        }

                        if((!friendReq.isEmpty()) && (friendReq != null)) {
                            if (friendReq.equals(currentID)) {
                                //user has been found


                                db.addFriendRequest(friendReq, username, phoneNumber); //add friendRequest to local DB

                            /*for (DataSnapshot snapshot : indavidualsnapshot.child("FriendsRequests").getChildren()) {
                                String requestID = snapshot.getValue().toString();

                                for (com.google.firebase.database.DataSnapshot requestDataSnapshot : dataSnapshot.getChildren()) {

                                    if (requestID.equals(requestDataSnapshot.child("id").getValue().toString())) {*/
                                        /*// Get the username
                                        String requestusername = requestDataSnapshot.child("name").getValue().toString();

                                        // Get the user phone number
                                        String requestphoneNumber = requestDataSnapshot.child("phoneNumber").getValue().toString();

                                        db.addFriendRequest(requestID, requestusername, requestphoneNumber); //add friendRequest to local DB*/
                                    /*}
                                }
                            }*/
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

    private void AcceptRequest(final String pId) {

        //final ArrayList<String> foundUser = new ArrayList<String>();

        final DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference().child("users");
        final DatabaseReference firendsRequestssRef = FirebaseDatabase.getInstance().getReference().child("users").child("FriendsRequests");

        usernamesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    for (com.google.firebase.database.DataSnapshot indavidualsnapshot : dataSnapshot.getChildren()) {
                        // Get the username
                        String username = indavidualsnapshot.child("name").getValue().toString();

                        // Get the user phone number
                        String phoneNumber = indavidualsnapshot.child("phoneNumber").getValue().toString();

                        //store the found users id for later use
                        String id = indavidualsnapshot.child("id").getValue().toString();

                        String RequestedID = indavidualsnapshot.child("FriendsRequests").getValue().toString();

                        //if (id.equals(pId)) {
                            //user has been found

                            if (pId.equals(RequestedID)) {

                                //remove the request
                                indavidualsnapshot.child("FriendsRequests").child(RequestedID).getRef().removeValue();//remove the request from requests

                                //add the friend to friends
                                usernamesRef.child(mAuth.getCurrentUser().getUid()).child("Friends").push().setValue(RequestedID);

                                //send accepted request to user
                                usernamesRef.child(RequestedID).child("AcceptedFriendsRequests").push().setValue(mAuth.getCurrentUser().getUid());
                        /*for (DataSnapshot snapshot : indavidualsnapshot.child("FriendsRequests").getChildren()) {
                            if (pId.equals(snapshot.getValue().toString())) {
                            snapshot.getRef().removeValue();//remove the request from requests
                        }*/
                            }
                        }
                    //}
                }catch (Exception e) {
                    e.printStackTrace();
                }

        }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
