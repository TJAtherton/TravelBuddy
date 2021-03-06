package com.tobyatherton.travelbuddy.travelbuddy.ARCamera;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaActionSound;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.atap.tango.ux.TangoUx;
import com.google.atap.tango.ux.TangoUxLayout;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;
import com.tobyatherton.travelbuddy.travelbuddy.GPSService;
import com.tobyatherton.travelbuddy.travelbuddy.R;

import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.view.SurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static com.tobyatherton.travelbuddy.travelbuddy.GPSService.MY_PREFS_NAME;

public class ARCameraActivity extends AppCompatActivity implements View.OnTouchListener, SceneRenderer.ScreenshotCallback {
    private static final String TAG = ARCameraActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;
    // For all current Tango devices, color camera is in the camera id 0.
    private static final int PERMISSION_REQUEST = 1;
    private static final int STATE_PLACE_TREE = 1;
    private static final int STATE_SCALE_TREE = 2;

    private SurfaceView mSurfaceView;
    private TextView mInstructions;
    private FrameLayout mPanelFlash;
    private Button mCameraButton;

    private SceneRenderer mRenderer;
    private TangoPointCloudManager mPointCloudManager;
    private Tango mTango;
    private TangoUx mTangoUx;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;
    private double mCameraPoseTimestamp = 0;

    private FirebaseAuth mAuth;
    public static final String EXTRA_LOCATION = "EXTRA_LOCATION";
    private Location lastLocation;
    private IntentFilter filter;
    private GPSService mService;
    private boolean mBounded;
    private Intent iGPSService;
    private LocationManager mLocationManager;
    private int LOCATION_INTERVAL = 0;
    private float LOCATION_DISTANCE = 0;


    // Texture rendering related fields
    // NOTE: Naming indicates which thread is in charge of updating this variable
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);
    private double mRgbTimestampGlThread;
    private int mDisplayRotation = 0;

    private boolean mPermissionsGranted = false;
    private int mTouchState = STATE_PLACE_TREE;
    private boolean mIsTreePlaced = false;

    private boolean hasExtras = false;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arcamera);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        mInstructions = (TextView) findViewById(R.id.instructions);
        mPanelFlash = (FrameLayout) findViewById(R.id.panel_flash);
        mCameraButton = (Button) findViewById(R.id.btn_screenshot);

        mRenderer = new SceneRenderer(ARCameraActivity.this, this);
        mSurfaceView.setSurfaceRenderer(mRenderer);

        mSurfaceView.setOnTouchListener(this);

        mPointCloudManager = new TangoPointCloudManager();

        mAuth = FirebaseAuth.getInstance(); //get firebase instance

        //setup and register listner for the user location
        filter = new IntentFilter("com.tobyatherton.travelbuddy.travelbuddy.ACTION_RECEIVE_LOCATION");
        registerReceiver(LocationUpdateReceiver, filter);

        mTangoUx = new TangoUx(this);
        TangoUxLayout tangoUxLayout = (TangoUxLayout) findViewById(R.id.layout_tango_ux);
        mTangoUx.setLayout(tangoUxLayout);

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {}

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized (this) {
                        setDisplayRotation();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {}
            }, null);
        }

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            if (getIntent().getStringExtra("markerViewed").equals("userViewingMarker")) {
                //place user marker down automatically
                hasExtras = true;
            } else {
                Toast.makeText(this, "as normal", Toast.LENGTH_LONG).show();
                hasExtras = false;
            }
        }

    }

    @SuppressLint("WrongConstant")
    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL thread.
        mSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mIsConnected) {
                    mRenderer.updateColorCameraTextureUvGlThread(mDisplayRotation);
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mSurfaceView.onResume();

        Intent mIntent = new Intent(this, GPSService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        if (ContextCompat.checkSelfPermission(ARCameraActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(ARCameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(ARCameraActivity.this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
        } else {
            initTango();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "Unable to launch application without camera permission.");
                            finish();
                        }
                    }
                }

                Log.d(TAG, "permission granted so next onResume will start Tango");
            }
        }
    }

    private void initTango() {
        // Initialize Tango Service as a normal Android Service, since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time when onResume gets called, we
        // should create a new Tango object.
        mTango = new Tango(ARCameraActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready, this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there is no UI
            // thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in the OpenGL
                // thread or in the UI thread.
                synchronized (ARCameraActivity.this) {
                    try {
                        TangoSupport.initialize();
                        TangoUx.StartParams params = new TangoUx.StartParams();
//                        params.showConnectionScreen = false;
                        mTangoUx.start(params);
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                        connectRenderer();
                        mIsConnected = true;
                        setDisplayRotation();
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);

                        if (mTangoUx != null) {
                            mTangoUx.showTangoOutOfDate();
                        }

                        showsToastAndFinishOnUiThread(R.string.exception_out_of_date);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);

                        showsToastAndFinishOnUiThread(R.string.exception_tango_error);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);

                        showsToastAndFinishOnUiThread(R.string.exception_tango_invalid);
                    }
                }
            }
        });
    }

    private void showsToastAndFinishOnUiThread(final int resId) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ARCameraActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }

        if (mSurfaceView != null) {
            mSurfaceView.onPause();
        }

        // Synchronize against disconnecting while the service is being used in the OpenGL thread or
        // in the UI thread.
        synchronized (this) {
            try {
                if (mRenderer != null) {
                    mRenderer.getCurrentScene().clearFrameCallbacks();
                }
                if (mTango != null) {
                    mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    // We need to invalidate the connected texture ID so that we cause a re-connection
                    // in the OpenGL thread after resume
                    mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                    mTango.disconnect();
                    mTangoUx.stop();
                }
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }
    }

    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Use default configuration for Tango Service (motion tracking), plus low latency
        // IMU integration, color camera, depth and drift correction.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        // NOTE: Low latency integration is necessary to achieve a precise alignment of
        // virtual objects with the RBG image and produce a good AR effect.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        // Drift correction allows motion tracking to recover after it loses tracking.
        // The drift corrected pose is is available through the frame pair with
        // base frame AREA_DESCRIPTION and target frame DEVICE.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);

        return config;
    }

    /**
     * Set up the callback listeners for the Tango service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the RGB camera and Point Cloud.
     */
    private void startupTango() {
        // No need to add any coordinate frame pairs since we are not
        // using pose data. So just initialize.
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // We are not using OnPoseAvailable for this app.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // Check if the frame available is for the camera we want and update its frame
                // on the view.
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Mark a camera frame is available for rendering in the OpenGL thread
                    mIsFrameAvailableTangoThread.set(true);
                    mSurfaceView.requestRender();
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                if (mTangoUx != null) {
                    mTangoUx.updateXyzCount(xyzIj.xyzCount);
                }
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                // Save the cloud and point data for later use.
                mPointCloudManager.updatePointCloud(pointCloud);
            }

            @Override
            public void onTangoEvent(TangoEvent event) {
                if (mTangoUx != null) {
                    mTangoUx.updateTangoEvent(event);
                }
            }
        });
    }

    /**
     * Connects the view and renderer to the color camara and callbacks.
     */
    private void connectRenderer() {
        // Register a Rajawali Scene Frame Callback to update the scene camera pose whenever a new
        // RGB frame is rendered.
        // (@see https://github.com/Rajawali/Rajawali/wiki/Scene-Frame-Callbacks)
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // NOTE: This is called from the OpenGL render thread, after all the renderer
                // onRender callbacks had a chance to run and before scene objects are rendered
                // into the scene.

                try {
                    synchronized (ARCameraActivity.this) {
                        // Don't execute any tango API actions if we're not connected to the service
                        if (!mIsConnected) {
                            return;
                        }

                        // Set-up scene camera projection to match RGB camera intrinsics
                        if (!mRenderer.isSceneCameraConfigured()) {
                            TangoCameraIntrinsics intrinsics =
                                    TangoSupport.getCameraIntrinsicsBasedOnDisplayRotation(
                                            TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                            mDisplayRotation);
                            mRenderer.setProjectionMatrix(
                                    projectionMatrixFromCameraIntrinsics(intrinsics));
                        }

                        // Connect the camera texture to the OpenGL Texture if necessary
                        // NOTE: When the OpenGL context is recycled, Rajawali may re-generate the
                        // texture with a different ID.
                        if (mConnectedTextureIdGlThread != mRenderer.getTextureId()) {
                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    mRenderer.getTextureId());
                            mConnectedTextureIdGlThread = mRenderer.getTextureId();
                            Log.d(TAG, "connected to texture id: " + mRenderer.getTextureId());
                        }

                        // If there is a new RGB camera frame available, update the texture with it
                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
                            mRgbTimestampGlThread =
                                    mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                        }

                        if (mRgbTimestampGlThread > mCameraPoseTimestamp) {
                            // Calculate the camera color pose at the camera frame update time in
                            // OpenGL engine.
                            //
                            // When drift correction mode is enabled in config file, we need
                            // to query the device with respect to Area Description pose in
                            // order to use the drift corrected pose.
                            //
                            // Note that if you don't want to use the drift corrected pose, the
                            // normal device with respect to start of service pose is still
                            // available.
                            TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(
                                    mRgbTimestampGlThread,
                                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                    mDisplayRotation);

                            if (mTangoUx != null) {
                                mTangoUx.updatePoseStatus(lastFramePose.statusCode);
                            }

                            // We have not placed a tree yet so show the instructions
                            if (mIsTreePlaced == false && mInstructions.getVisibility() == View.INVISIBLE) {
                                mInstructions.setVisibility(View.VISIBLE);
                                //mCameraButton.setVisibility(View.INVISIBLE);
                            }

                            if (lastFramePose.statusCode == TangoPoseData.POSE_VALID) {
                                // Update the camera pose from the renderer
                                mRenderer.updateRenderCameraPose(lastFramePose);
                                mCameraPoseTimestamp = lastFramePose.timestamp;

                            } else {
                                // When the pose status is not valid, it indicates the tracking has
                                // been lost. In this case, we simply stop rendering.
                                //
                                // This is also the place to display UI to suggest the user walk
                                // to recover tracking.
                                Log.w(TAG, "Can't get device pose at time: " +
                                        mRgbTimestampGlThread);
                            }
                        }
                    }
                    // Avoid crashing the application due to unhandled exceptions
                } catch (TangoErrorException e) {
                    Log.e(TAG, "Tango API call error within the OpenGL render thread", e);
                } catch (Throwable t) {
                    Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }

            @Override
            public boolean callPreFrame() {
                return true;
            }
        });
    }

    /**
     * Use Tango camera intrinsics to calculate the projection Matrix for the Rajawali scene.
     * The function also rotates the intrinsics based on current rotation from color camera to
     * display.
     */
    private static float[] projectionMatrixFromCameraIntrinsics(TangoCameraIntrinsics intrinsics) {
        // Uses frustumM to create a projection matrix taking into account calibrated camera
        // intrinsic parameter.
        // Reference: http://ksimek.github.io/2013/06/03/calibrated_cameras_in_opengl/
        float near = 0.1f;
        float far = 100;

        double cx = intrinsics.cx;
        double cy = intrinsics.cy;
        double width = intrinsics.width;
        double height = intrinsics.height;
        double fx = intrinsics.fx;
        double fy = intrinsics.fy;

        double xscale = near / fx;
        double yscale = near / fy;

        double xoffset = (cx - (width / 2.0)) * xscale;
        // Color camera's coordinates has y pointing downwards so we negate this term.
        double yoffset = -(cy - (height / 2.0)) * yscale;

        float m[] = new float[16];
        Matrix.frustumM(m, 0,
                (float) (xscale * -width / 2.0 - xoffset),
                (float) (xscale * width / 2.0 - xoffset),
                (float) (yscale * -height / 2.0 - yoffset),
                (float) (yscale * height / 2.0 - yoffset), near, far);
        return m;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        // Assume in place mode
        // if two pointers detected then switch to scale mode
        // if distance between pointers is greater on ACTION_MOVE then scale up
        // if distance between pointers is less then on ACTION_MOVE scale down

        // Need to store position of pointers on touch in an array so that calculation can happen for move
        int action = MotionEventCompat.getActionMasked(motionEvent);
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d(TAG, "Changed to scaling");
                mTouchState = STATE_SCALE_TREE;
                break;
            case MotionEvent.ACTION_UP:
                if (mTouchState == STATE_PLACE_TREE) {
                    Log.d(TAG, "Placing the tree");
                    placeARMarker(view, motionEvent);
                    //mRenderer.AddSign();//adds a new sign
                } else {
                    Log.d(TAG, "Changing state to placing");
                    mTouchState = STATE_PLACE_TREE;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchState == STATE_SCALE_TREE && motionEvent.getPointerCount() > 1) {
                    // Find distance between points
                    try {
                        float x0 = motionEvent.getHistoricalX(0, 0);
                        float x1 = motionEvent.getHistoricalX(1, 0);
                        float y0 = motionEvent.getHistoricalY(0, 0);
                        float y1 = motionEvent.getHistoricalY(1, 0);
                        double dist0 = Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2));

                        x0 = motionEvent.getX(0);
                        x1 = motionEvent.getX(1);
                        y0 = motionEvent.getY(0);
                        y1 = motionEvent.getY(1);
                        double dist1 = Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2));

                        Log.d(TAG, String.format("Distance changed from %.2f to %.2f", dist0, dist1));

                        double scale = (dist1 - dist0) / dist0;

                        Log.d(TAG, String.format("Scaled changed by %.2f", scale));

                        // Update tree scale
                        mRenderer.updateObjectScale(scale);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        }

        return true;
    }

    private void placeARMarker(View view, MotionEvent motionEvent) {
        // Calculate click location in u,v (0;1) coordinates.
        float u = motionEvent.getX() / view.getWidth();
        float v = motionEvent.getY() / view.getHeight();

        try {
            // Fit a plane on the clicked point using the latest poiont cloud data
            // Synchronize against concurrent access to the RGB timestamp in the OpenGL thread
            // and a possible service disconnection due to an onPause event.
            float[] planeFitTransform;
            synchronized (this) {
                planeFitTransform = doFitPlane(u, v, mRgbTimestampGlThread);
            }

            if (planeFitTransform != null) {
                // Update the position of the rendered cube to the pose of the detected plane
                // This update is made thread safe by the renderer
                mRenderer.updateObjectPose(planeFitTransform);

                // Now that we have placed the tree hide the instructions.
                if (mIsTreePlaced == false) {
                    mInstructions.setVisibility(View.INVISIBLE);
                    //mCameraButton.setVisibility(View.VISIBLE);
                    mIsTreePlaced = true;
                }
            }

        } catch (TangoException t) {
            Toast.makeText(getApplicationContext(),
                    R.string.failed_measurement,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.failed_measurement), t);
        } catch (SecurityException t) {
            Toast.makeText(getApplicationContext(),
                    R.string.failed_permissions,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.failed_permissions), t);
        }
    }

    /**
     * Use the TangoSupport library with point cloud data to calculate the plane
     * of the world feature pointed at the location the camera is looking.
     * It returns the transform of the fitted plane in a double array.
     */
    private float[] doFitPlane(float u, float v, double rgbTimestamp) {
        TangoPointCloudData pointCloud = mPointCloudManager.getLatestPointCloud();

        if (pointCloud == null) {
            return null;
        }

        // We need to calculate the transform between the color camera at the
        // time the user clicked and the depth camera at the time the depth
        // cloud was acquired.
        TangoPoseData depthTcolorPose = TangoSupport.calculateRelativePose(
                pointCloud.timestamp, TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                rgbTimestamp, TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR);

        // Perform plane fitting with the latest available point cloud data.
        double[] identityTranslation = {0.0, 0.0, 0.0};
        double[] identityRotation = {0.0, 0.0, 0.0, 1.0};
        TangoSupport.IntersectionPointPlaneModelPair intersectionPointPlaneModelPair =
                TangoSupport.fitPlaneModelNearPoint(pointCloud,
                        identityTranslation, identityRotation, u, v, mDisplayRotation,
                        depthTcolorPose.translation, depthTcolorPose.rotation);

        // Get the transform from depth camera to OpenGL world at the timestamp of the cloud.
        TangoSupport.TangoMatrixTransformData transform =
                TangoSupport.getMatrixTransformAtTime(pointCloud.timestamp,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                        TangoSupport.ROTATION_IGNORED);
        if (transform.statusCode == TangoPoseData.POSE_VALID) {
            float[] openGlTPlane = calculatePlaneTransform(
                    intersectionPointPlaneModelPair.intersectionPoint,
                    intersectionPointPlaneModelPair.planeModel, transform.matrix);

            return openGlTPlane;
        } else {
            Log.w(TAG, "Can't get depth camera transform at time " + pointCloud.timestamp);
            return null;
        }
    }

    /**
     * Calculate the pose of the plane based on the position and normal orientation of the plane
     * and align it with gravity.
     */
    private float[] calculatePlaneTransform(double[] point, double normal[],
                                            float[] openGlTdepth) {
        // Vector aligned to gravity.
        float[] openGlUp = new float[]{0, 1, 0, 0};
        float[] depthTOpenGl = new float[16];
        Matrix.invertM(depthTOpenGl, 0, openGlTdepth, 0);
        float[] depthUp = new float[4];
        Matrix.multiplyMV(depthUp, 0, depthTOpenGl, 0, openGlUp, 0);
        // Create the plane matrix transform in depth frame from a point, the plane normal and the
        // up vector.
        float[] depthTplane = matrixFromPointNormalUp(point, normal, depthUp);
        float[] openGlTplane = new float[16];
        Matrix.multiplyMM(openGlTplane, 0, openGlTdepth, 0, depthTplane, 0);
        return openGlTplane;
    }

    /**
     * Calculates a transformation matrix based on a point, a normal and the up gravity vector.
     * The coordinate frame of the target transformation will a right handed system with Z+ in
     * the direction of the normal and Y+ up.
     */
    private float[] matrixFromPointNormalUp(double[] point, double[] normal, float[] up) {
        float[] zAxis = new float[]{(float) normal[0], (float) normal[1], (float) normal[2]};
        normalize(zAxis);
        float[] xAxis = crossProduct(up, zAxis);
        normalize(xAxis);
        float[] yAxis = crossProduct(zAxis, xAxis);
        normalize(yAxis);
        float[] m = new float[16];
        Matrix.setIdentityM(m, 0);
        m[0] = xAxis[0];
        m[1] = xAxis[1];
        m[2] = xAxis[2];
        m[4] = yAxis[0];
        m[5] = yAxis[1];
        m[6] = yAxis[2];
        m[8] = zAxis[0];
        m[9] = zAxis[1];
        m[10] = zAxis[2];
        m[12] = (float) point[0];
        m[13] = (float) point[1];
        m[14] = (float) point[2];
        return m;
    }

    /**
     * Normalize a vector.
     */
    private void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }

    /**
     * Cross product between two vectors following the right hand rule.
     */
    private float[] crossProduct(float[] v1, float[] v2) {
        float[] result = new float[3];
        result[0] = v1[1] * v2[2] - v2[1] * v1[2];
        result[1] = v1[2] * v2[0] - v2[2] * v1[0];
        result[2] = v1[0] * v2[1] - v2[0] * v1[1];
        return result;
    }

    public void takeScreenshot(View view) {
        //take Screnshot in renderer
        mRenderer.takeScreenshot();
        //save pic locations for later updating

        if((isOnline()) && lastLocation != null) {
            updateARMarkerLocation(lastLocation); //update the location of the AR Marker in firebase
        }
    }

    @Override
    public void onScreenshotTaken(final Bitmap screenshotBitmap) {
        // Give immediate feedback to the user.
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPanelFlash.setVisibility(View.VISIBLE);
                // Run a fade in and out animation of a white screen.
                ObjectAnimator fadeIn =
                        ObjectAnimator.ofFloat(mPanelFlash, View.ALPHA, 0, 1);
                fadeIn.setDuration(100);
                fadeIn.setInterpolator(new DecelerateInterpolator());
                ObjectAnimator fadeOut =
                        ObjectAnimator.ofFloat(mPanelFlash, View.ALPHA, 1, 0);
                fadeOut.setInterpolator(new AccelerateInterpolator());
                fadeOut.setDuration(100);

                AnimatorSet animation = new AnimatorSet();
                animation.playSequentially(fadeIn, fadeOut);
                animation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mPanelFlash.setVisibility(View.GONE);
                    }
                });
                animation.start();
            }
        });
        // Save bitmap to gallery in background.
        new BitmapSaverTask(screenshotBitmap).execute();
    }

    /**
     * Internal AsyncTask that saves a given Bitmap.
     */
    private class BitmapSaverTask extends AsyncTask<Void, Void, Boolean> {
        private Bitmap mBitmap;

        BitmapSaverTask(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                File bitmapFile = getNewFileForBitmap();
                saveBitmap(bitmapFile, mBitmap);
                addScreenshotToGallery(bitmapFile);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result != null) {
                Toast.makeText(ARCameraActivity.this, "Photograph taken and saved.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ARCameraActivity.this, "Error! photo could not be saved.", Toast.LENGTH_LONG).show();
            }
        }

        /**
         * returns a new bitmap File. This is a random generated filename.
         *
         * @return File
         * @throws IOException
         */
        private File getNewFileForBitmap() throws IOException {
            File bmpFile = null;
            Random rn = new Random();
            boolean validPath = false;
            while (!validPath) {
                String fileId = String.valueOf(rn.nextInt());
                bmpFile = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "TravelBuddy" + fileId + ".png");
                validPath = !bmpFile.exists();
                
            }
            bmpFile.createNewFile();
            return bmpFile;
        }

        /**
         * Saves the given bitmap to disk (as a PNG file).
         *
         * @param localFile
         * @param bmp
         * @throws IOException
         */
        private void saveBitmap(File localFile, Bitmap bmp) throws IOException {
            FileOutputStream fos = new FileOutputStream(localFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        }

        /**
         * Adds a file to the Android gallery.
         */
        private void addScreenshotToGallery(File localFile) throws IOException {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            Log.d(TAG, localFile.getCanonicalPath());
            values.put(MediaStore.MediaColumns.DATA, localFile.getCanonicalPath());
            ARCameraActivity.this.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }
    }

    public void updateARMarkerLocation(Location pUserLocation) {
        // Update the users score
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(mAuth.getCurrentUser().getUid());
        if(pUserLocation == null) {
            //usersRef.child("ARMarkerLocation").push().setValue("Cannot get location of AR marker");
            usersRef.child("ARMarkerLocation").setValue("Cannot get location of AR marker");
        } else {
            LatLng latlng = new LatLng(pUserLocation.getLatitude(),pUserLocation.getLongitude()); //put latitude and longitude into latlng object
            //usersRef.child("ARMarkerLocation").push().setValue(latlng);
            usersRef.child("ARMarkerLocation").setValue(latlng);
        }
    }

    private final BroadcastReceiver LocationUpdateReceiver = new BroadcastReceiver() {

        /**
         * Receives broadcast from GPS service.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            Location location = (Location) extras.get(ARCameraActivity.EXTRA_LOCATION);

            lastLocation = location;
        }
    };

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
    protected void onDestroy() {
        super.onDestroy();
        //TEST, maybe in service itself?
        unregisterReceiver(LocationUpdateReceiver);
        if(mService != null) {
            mService.stopService(iGPSService);
        }
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
            //mService.setMapsContext(ARCameraActivity.this);
            mBounded = true;

            //test
            iGPSService = new Intent(ARCameraActivity.this, GPSService.class);
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

    //bind test

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

}
