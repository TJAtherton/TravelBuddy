package com.tobyatherton.travelbuddy.travelbuddy.ARCamera;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.MotionEvent;

import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;
import com.tobyatherton.travelbuddy.travelbuddy.R;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.renderer.Renderer;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by alex on 12/15/16.
 */

public class SceneRenderer extends Renderer {

    private static final String TAG = SceneRenderer.class.getSimpleName();
    private boolean mTakeScreenshot = false;

    public interface ScreenshotCallback {

        void onScreenshotTaken(Bitmap screenshot);
    }

    private ScreenshotCallback mScreenshotCallback;

    public static final double MIN_SCALE = 0.0005f;
    public static final double MAX_SCALE = 0.004f;
    public static final double SCALE_FACTOR = 0.001f;

    private float[] textureCoords0 = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F};

    // Augmented Reality related fields
    private ATexture mTangoCameraTexture;
    private boolean mSceneCameraConfigured;

    private Object3D mObjects;
    private Matrix4 mObjectTransform;
    private boolean mObjectPoseUpdated = false;

    private ScreenQuad mBackgroundQuad;

    private double mObjectScale = 0.003f; //= 0.003f;
    private boolean mObjectScaleUpdated = false;

    private int numOfObjects = 0;

    private Context mContext;

    public SceneRenderer(Context context, ScreenshotCallback callback) {
        super(context);
        mContext = context;
        mScreenshotCallback = callback;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void initScene() {
        // Create a quad covering the whole background and assign a texture to it where the
        // Tango color camera contents will be rendered.

        //mObjects = new ArrayList<Object3D>();

        if (mBackgroundQuad == null) {
            mBackgroundQuad = new ScreenQuad();
            mBackgroundQuad.getGeometry().setTextureCoords(textureCoords0);
        }
        Material tangoCameraMaterial = new Material();
        tangoCameraMaterial.setColorInfluence(0);
        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering
        mTangoCameraTexture =
                new StreamingTexture("camera", (StreamingTexture.ISurfaceListener) null);
        try {
            tangoCameraMaterial.addTexture(mTangoCameraTexture);
            mBackgroundQuad.setMaterial(tangoCameraMaterial);
        } catch (ATexture.TextureException e) {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e);
        }
        getCurrentScene().addChildAt(mBackgroundQuad, 0);

        // Add a directional light in an arbitrary direction.
        DirectionalLight light = new DirectionalLight(1, 0.2, -1);
        light.setColor(1, 1, 1);
        light.setPower(2.5f);
        light.setPosition(3, 3, 4);
        getCurrentScene().addLight(light);

        // Set-up a material: green with application of the light and
        // instructions.
        Material material = new Material();

        try {
            Texture t = new Texture("sign", drawimagetext());//drawTextToBitmap(mContext, "test string for sign")); //R.drawable.h00_mkan);
            material.addTexture(t);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
        material.setColorInfluence(0.1f);
        material.enableLighting(true);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());

        try {
            LoaderOBJ objParser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.com_bm_murekanban);
            objParser.parse();
            mObjects = objParser.getParsedObject();
            mObjects.rotate(Vector3.Axis.X, 0.0f);
            mObjects.rotate(Vector3.Axis.Y, 180f);
            mObjects.setScale(mObjectScale *10);
//            mObject = new Plane(.5f, .5f, 1, 1, Vector3.Axis.Z);
            //mObject.setMaterial(material);
            mObjects.setPosition(0, 0, -3);
            mObjects.setColor(getContext().getResources().getColor(R.color.surfaceSelection, null));
            getCurrentScene().addChild(mObjects);
            numOfObjects++;
        } catch(ParsingException e) {
            e.printStackTrace();
        }

        // Build a Cube and place it initially three meters forward from the origin.
//        mObject = new Cube(CUBE_SIDE_LENGTH);

    }

    /**
     * Update background texture's UV coordinates when device orientation is changed. i.e change
     * between landscape and portrait mode.
     * This must be run in the OpenGL thread.
     */
    public void updateColorCameraTextureUvGlThread(int rotation) {
        if (mBackgroundQuad == null) {
            mBackgroundQuad = new ScreenQuad();
        }

        float[] textureCoords = TangoSupport.getVideoOverlayUVBasedOnDisplayRotation(textureCoords0, rotation);
        mBackgroundQuad.getGeometry().setTextureCoords(textureCoords, true);
        mBackgroundQuad.getGeometry().reload();
    }

    @Override
    protected void onRender(long elapsedRealTime, double deltaTime) {
        // Update the AR objects if necessary
        // Synchronize against concurrent access with the setter below.
        synchronized (this) {
            if (mObjectPoseUpdated) {
                // Place the 3D object in the location of the detected plane.
                mObjects.setPosition(mObjectTransform.getTranslation());
                // Note that Rajawali uses left-hand convetion for Quaternions so we need to
                // specify a quaternion with rotation in the opposite direction.
//                mObject.setOrientation(new Quaternion().fromMatrix(mObjectTransform));
                // Move it forward by half of the size of the cube to make it
                // flush with the plane surface.
//                mObject.rotate(Vector3.Axis.X, 180f);
//                mObject.moveForward(CUBE_SIDE_LENGTH / 2.0f);
                mObjectPoseUpdated = false;
            }

            if (mObjectScaleUpdated) {
                mObjects.setScale(mObjectScale);
                mObjectScaleUpdated = false;
            }

            // Take screenshot if needed.
            if (mTakeScreenshot) {
                mTakeScreenshot = false;
                Bitmap screenshot = createBitmapFromGLSurface(0, 0, mDefaultViewportWidth,
                        mDefaultViewportHeight);
                mScreenshotCallback.onScreenshotTaken(screenshot);
            }
        }

        super.onRender(elapsedRealTime, deltaTime);
    }

    /**
     * Save the updated plane fit pose to update the AR object on the next render pass.
     * This is synchronized against concurrent access in the render loop above.
     */
    public synchronized void updateObjectPose(float[] planeFitTransform) {
        mObjectTransform = new Matrix4(planeFitTransform);
        mObjectPoseUpdated = true;
    }

    public synchronized void updateObjectScale(double s) {
        mObjectScale = Math.min(Math.max(MIN_SCALE, mObjectScale + (s * SCALE_FACTOR)*10), MAX_SCALE);
        mObjectScaleUpdated = true;
    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The camera pose should match the pose of the camera color at the time the last rendered RGB
     * frame, which can be retrieved with this.getTimestamp();
     * <p/>
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public void updateRenderCameraPose(TangoPoseData cameraPose) {
        float[] rotation = cameraPose.getRotationAsFloats();
        float[] translation = cameraPose.getTranslationAsFloats();
        Quaternion quaternion = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
        // Conjugating the Quaternion is need because Rajawali uses left handed convention for
        // quaternions.
        getCurrentCamera().setRotation(quaternion.conjugate());
        getCurrentCamera().setPosition(translation[0], translation[1], translation[2]);
    }

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public int getTextureId() {
        return mTangoCameraTexture == null ? -1 : mTangoCameraTexture.getTextureId();
    }

    /**
     * We need to override this method to mark the camera for re-configuration (set proper
     * projection matrix) since it will be reset by Rajawali on surface changes.
     */
    @Override
    public void onRenderSurfaceSizeChanged(GL10 gl, int width, int height) {
        super.onRenderSurfaceSizeChanged(gl, width, height);
        mSceneCameraConfigured = false;
    }

    public boolean isSceneCameraConfigured() {
        return mSceneCameraConfigured;
    }

    /**
     * Sets the projection matrix for the scene camera to match the parameters of the color camera,
     * provided by the {@code TangoCameraIntrinsics}.
     */
    public void setProjectionMatrix(float[] matrix) {
        getCurrentCamera().setProjectionMatrix(new Matrix4(matrix));
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset,
                                 float xOffsetStep, float yOffsetStep,
                                 int xPixelOffset, int yPixelOffset) {
    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    public void takeScreenshot() {
        mTakeScreenshot = true;
    }

    /**
     * Read pixels from buffer to make a bitmap.
     *
     * @throws RuntimeException if there is a GLException.
     * @trhows OutOfMemoryError
     */
    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h) throws OutOfMemoryError {
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            GLES20.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            // Transformation needed from RGBA to ARGB due to different formats used by OpenGL
            // and Android.
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int green = texturePixel & 0x0000ff00;
                    int blue = (texturePixel >> 16) & 0xff;
                    int alpha = texturePixel & 0xff000000;
                    int pixel = alpha | red | green | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            Log.e(TAG, "Error while creating bitmap from GLSurface.");
            throw new RuntimeException(e);
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void AddSign() {
        // Set-up a material: green with application of the light and
        // instructions.
        Material material = new Material();

        try {
            Texture t = new Texture("sign", R.drawable.h00_mkan);
            material.addTexture(t);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
        material.setColorInfluence(0.1f);
        material.enableLighting(true);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());

        try {
            LoaderOBJ objParser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.com_bm_murekanban);
            objParser.parse();
            mObjects = objParser.getParsedObject();
            mObjects.rotate(Vector3.Axis.X, 0.0f);
            mObjects.rotate(Vector3.Axis.Y, 180f);
            mObjects.setScale(mObjectScale);
//            mObject = new Plane(.5f, .5f, 1, 1, Vector3.Axis.Z);
            //mObject.setMaterial(material);
            mObjects.setPosition(0, 0, -3);
            mObjects.setColor(getContext().getResources().getColor(R.color.surfaceSelection, null));
            getCurrentScene().addChild(mObjects);
            numOfObjects++;
        } catch(ParsingException e) {
            e.printStackTrace();
        }
    }

    //use this to set sign text
    public Bitmap drawTextToBitmap(Context pContext, String gText) {
        Resources resources = pContext.getResources();
        float scale = resources.getDisplayMetrics().density;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.h00_mkan);

        android.graphics.Bitmap.Config bitmapConfig =
                bitmap.getConfig();
        // set default bitmap config if none
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bitmap = bitmap.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(bitmap);
        // new antialised Paint
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.rgb(61, 61, 61));
        // text size in pixels
        paint.setTextSize((int) (14 * scale));
        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        // draw text to the Canvas center
        Rect bounds = new Rect();
        paint.getTextBounds(gText, 0, gText.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width())/2;
        int y = (bitmap.getHeight() + bounds.height())/2;

        canvas.drawText(gText, x, y, paint);

        return bitmap;
    }

    private Bitmap drawimagetext() {
        Paint paint = new Paint();
        Bitmap b = Bitmap.createBitmap(64, 64, Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(b);
        c.drawRect(0, 0, 64, 64, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        paint.setTextSize(40);
        paint.setTextScaleX(1.f);
        paint.setAlpha(0);
        paint.setAntiAlias(true);
        c.drawText("test Sign text", 10, 10, paint);
        paint.setColor(Color.RED);

        c.drawBitmap(b, 10,10, paint);

        return b;
    }
}
