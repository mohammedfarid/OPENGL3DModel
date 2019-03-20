package com.farid.opengl3dmodel.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.farid.opengl3dmodel.R;
import com.farid.opengl3dmodel.loader.SceneLoader;
import org.andresoviedo.util.android.ContentUtils;

import java.io.IOException;

/**
 * This activity represents the container for our 3D viewer.
 *
 * @author andresoviedo
 */
public class ModelActivity extends Activity implements SurfaceTexture.OnFrameAvailableListener {

    private static final int REQUEST_CODE_LOAD_TEXTURE = 1000;

    /**
     * Type of model if file name has no extension (provided though content provider)
     */
    private int paramType;
    /**
     * The file to load. Passed as input parameter
     */
    private Uri paramUri;
    /**
     * Enter into Android Immersive mode so the renderer is full screen or not
     */
    private boolean immersiveMode = true;
    /**
     * Background GL clear color. Default is light gray
     */
    private float[] backgroundColor = new float[]{0.2f, 0.2f, 0.2f, 1.0f};

    private ModelSurfaceView gLView;

    private SceneLoader scene;

    private Handler handler;

    private Boolean previewing = true;

    SensorManager sensorManager;
    Sensor rotationVectorSensor;
    SensorEventListener rvListener;

    float[] rotationMatrix = new float[16];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Try to get input parameters
        Bundle b = getIntent().getExtras();
        if (b != null) {
            if (b.getString("uri") != null) {
                this.paramUri = Uri.parse(b.getString("uri"));
            }
            this.paramType = b.getString("type") != null ? Integer.parseInt(b.getString("type")) : -1;
            this.immersiveMode = "true".equalsIgnoreCase(b.getString("immersiveMode"));
            try {
                String[] backgroundColors = b.getString("backgroundColor").split(" ");
                backgroundColor[0] = Float.parseFloat(backgroundColors[0]);
                backgroundColor[1] = Float.parseFloat(backgroundColors[1]);
                backgroundColor[2] = Float.parseFloat(backgroundColors[2]);
                backgroundColor[3] = Float.parseFloat(backgroundColors[3]);
            } catch (Exception ex) {
                // Assuming default background color
            }
        }
        Log.i("Renderer", "Params: uri '" + paramUri + "'");

        sensorManager =
                (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVectorSensor =
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        // Create a listener
        rvListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // More code goes here

                SensorManager.getRotationMatrixFromVector(
                        rotationMatrix, sensorEvent.values);
                // Remap coordinate system
                float[] remappedRotationMatrix = new float[16];
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedRotationMatrix);

                // Convert to orientations
                float[] orientations = new float[3];
                SensorManager.getOrientation(remappedRotationMatrix, orientations);

                for (int i = 0; i < 3; i++) {
                    orientations[i] = (float) (Math.toDegrees(orientations[i]));
                }

                if (orientations[0] > 160) {
                    Log.i("Rott+", "X: " + orientations[0] + " Y: " + orientations[1]);
                    if(orientations[1] >30){
                        gLView.setVisibility(View.GONE);
                    }else if(orientations[1]<-30){
                        gLView.setVisibility(View.GONE);
                    }else{
                        gLView.setVisibility(View.VISIBLE);
                    }
                } else if (orientations[0] < -160) {
                    Log.i("Rott-", "X: " + orientations[0] + " Y: " + orientations[1]);
                    if(orientations[1] >30){
                        gLView.setVisibility(View.GONE);
                    }else if(orientations[1]<-30){
                        gLView.setVisibility(View.GONE);
                    }else{
                        gLView.setVisibility(View.VISIBLE);
                    }
                } else {
                    gLView.setVisibility(View.GONE);
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

// Register it
        sensorManager.registerListener(rvListener,
                rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

        handler = new Handler(getMainLooper());

        // Create our 3D sceneario
        if (paramUri == null) {
            //scene = new ExampleSceneLoader(this);
            scene = new SceneLoader(this);
        } else {
            scene = new SceneLoader(this);
        }
        scene.init();

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.


        setContentView(R.layout.activity_model);
        gLView = findViewById(R.id.model);
        gLView.init(this);

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            private Camera mCamera;

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera = Camera.open();
                try {
                    mCamera.setPreviewDisplay(holder);
                } catch (IOException exception) {
                    mCamera.release();
                    mCamera = null;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {
                if (previewing) {
                    mCamera.stopPreview();
                    previewing = false;
                }
                Camera.Parameters parameters = mCamera.getParameters();
                Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).
                        getDefaultDisplay();
                android.hardware.Camera.CameraInfo info =
                        new android.hardware.Camera.CameraInfo();
                int or = info.orientation;
                if (display.getRotation() == Surface.ROTATION_0) {
                    mCamera.setDisplayOrientation(90);
                    or = 90;
                }
                if (display.getRotation() == Surface.ROTATION_180) {
                    mCamera.setDisplayOrientation(270);
                    or = 270;
                }
                if (display.getRotation() == Surface.ROTATION_270) {
                    mCamera.setDisplayOrientation(180);
                    or = 180;
                }

                parameters.setRotation(or);
                mCamera.setParameters(parameters);

                try {
                    mCamera.setPreviewDisplay(holder);
                    mCamera.startPreview();
                    previewing = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        // TODO: Alert user when there is no multitouch support (2 fingers). He won't be able to rotate or zoom
        ContentUtils.printTouchCapabilities(getPackageManager());

        setupOnSystemVisibilityChangeListener();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Register it
        sensorManager.unregisterListener(rvListener);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        gLView.requestRender();
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setupOnSystemVisibilityChangeListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            // Note that system bars will only be "visible" if none of the
            // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                // The system bars are visible. Make any desired
                if (immersiveMode) hideSystemUIDelayed(5000);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (immersiveMode) hideSystemUIDelayed(5000);
            }
        }
    }


    private void hideSystemUIDelayed(long millis) {
        handler.postDelayed(this::hideSystemUI, millis);
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hideSystemUIKitKat();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            hideSystemUIJellyBean();
        }
    }

    // This snippet hides the system bars.
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void hideSystemUIKitKat() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void hideSystemUIJellyBean() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showSystemUI() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public Uri getParamUri() {
        return paramUri;
    }

    public int getParamType() {
        return paramType;
    }

    public float[] getBackgroundColor() {
        return backgroundColor;
    }

    public SceneLoader getScene() {
        return scene;
    }

    public ModelSurfaceView getGLView() {
        return gLView;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_LOAD_TEXTURE:
                // The URI of the selected file
                final Uri uri = data.getData();
                if (uri != null) {
                    Log.i("ModelActivity", "Loading texture '" + uri + "'");
                    try {
                        ContentUtils.setThreadActivity(this);
                        scene.loadTexture(null, uri);
                    } catch (IOException ex) {
                        Log.e("ModelActivity", "Error loading texture: " + ex.getMessage(), ex);
                        Toast.makeText(this, "Error loading texture '" + uri + "'. " + ex
                                .getMessage(), Toast.LENGTH_LONG).show();
                    } finally {
                        ContentUtils.setThreadActivity(null);
                    }
                }
        }
    }

}
