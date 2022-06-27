package com.tobyatherton.travelbuddy.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.tobyatherton.travelbuddy.travelbuddy.ARCamera.ARCameraActivity;
import com.tobyatherton.travelbuddy.travelbuddy.Login.LoginActivity;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    Button signout;

    Button btnMaps;
    Button btnProfile;
    Button btnStats;
    Button btnCamera;
    Button btnExit;

    TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Firebase.setAndroidContext(this);

        mAuth = FirebaseAuth.getInstance(); //get firebase instance

        signout = (Button) findViewById(R.id.btnSignOut);
        tvTitle = (TextView) findViewById(R.id.tvTitle);

        Bundle extras;

        extras = getIntent().getExtras();
        if (extras != null) {
            String userEmail = extras.getString("user_email");
            tvTitle.setText("Welcome: " + userEmail);
            // and get whatever type user account id is
        }

        btnMaps = (Button) findViewById(R.id.btnMap);
        btnProfile = (Button) findViewById(R.id.btnProfile);
        btnStats = (Button) findViewById(R.id.btnStats);
        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnExit = (Button) findViewById(R.id.btnExit);

        btnMaps.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Intent activityChangeIntent = new Intent(MainActivity.this, MapsActivity.class);

                 //currentContext.startActivity(activityChangeIntent);

                MainActivity.this.startActivity(activityChangeIntent);
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Intent activityChangeIntent = new Intent(MainActivity.this, UserProfile.class);

                // currentContext.startActivity(activityChangeIntent);

                MainActivity.this.startActivity(activityChangeIntent);
            }
        });

        btnStats.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Intent activityChangeIntent = new Intent(MainActivity.this, Stats.class);

                // currentContext.startActivity(activityChangeIntent);

                MainActivity.this.startActivity(activityChangeIntent);
            }
        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Intent activityChangeIntent = new Intent(MainActivity.this, ARCameraActivity.class);

                // currentContext.startActivity(activityChangeIntent);

                MainActivity.this.startActivity(activityChangeIntent);
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
                System.exit(0);
            }
        });

        signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOut();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * This method will be called any time a user selects one of the options
     * on the menu. For the implementation, whichever button is clicked is
     * mapped onto the relevant activity.
     * @param item MenuItem
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        /*switch (item.getItemId())
        {
            case R.id.prefs:
                startActivity(new Intent(this, EditPreferences.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }*/
        return false;
    }

    public void logOut(){
        mAuth.signOut(); //signs out from firebase
        Toast.makeText(this,"you have been signed out",Toast.LENGTH_LONG).show();
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }
}
