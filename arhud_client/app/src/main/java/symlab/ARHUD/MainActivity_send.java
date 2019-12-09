package symlab.ARHUD;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import symlab.CloudAR.ARManager;
import symlab.CloudAR.Constants;
import symlab.CloudAR.Detected;

//-----------pengzhou---------------
//-----------pengzhou---------------


public class MainActivity_send extends Activity implements LocationListener, SensorEventListener, View.OnTouchListener {

    private SurfaceView mPreview;
    private SurfaceHolder mPreviewHolder;

    private Camera mCamera;
    private boolean mInPreview = false;
    private boolean mCameraConfigured = false;
    private DrawOnTop mDraw;
    private byte[] callbackBuffer;
    private int time_o, time_n, fps;
    private boolean recoFlag = false;
    private int frameID;
    public byte[] locationbyte;

    //-----------------------------------pengzhou: location service---------------------------------
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Context context;
    protected double latitude;
    protected double longitude;
    protected double timeCaptured;
    protected double timeSend;
    protected float bearing;
    TextView txtLat;

    //-----------------------------------pengzhou: orientation service------------------------------
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
    private double angle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(Constants.TAG, " onCreate() called.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtLat = findViewById(R.id.text1);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        //-----------------------------------pengzhou: location service-----------------------------

        mPreview = findViewById(R.id.preview);
        mPreview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        mPreviewHolder = mPreview.getHolder();
        mPreviewHolder.addCallback(surfaceCallback);
        mPreview.setZOrderMediaOverlay(false);
        // pengzhou: following command enable ontouch
        //mPreview.setOnTouchListener(this);

        if (Constants.Show2DView) {
            mDraw = new DrawOnTop(this);
            addContentView(mDraw, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        ARManager.getInstance().init(this, true);
        ARManager.getInstance().setCallback(new ARManager.Callback() {
            @Override
            public void onObjectsDetected(Detected[] detected) {
                mDraw.updateData(detected);
                //mDraw.updateData(frameID);
                //mDraw.updateOrientationAngles();
            }
        });
    }
    //-----------------------------------pengzhou: location service---------------------------------
    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        txtLat = findViewById(R.id.text1);
        txtLat.setText("Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
        String locationstring = txtLat.getText().toString();
        locationbyte = locationstring.getBytes();
        latitude =  location.getLatitude();
        longitude = location.getLongitude();
        bearing = location.getBearing();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }
    //-----------------------------------pengzhou: orientation service------------------------------
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }


    @Override
    public void onStart() {
        Log.i(Constants.TAG, " onStart() called.");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.i(Constants.TAG, " onResume() called.");
        super.onResume();

        mCamera = Camera.open();
        ARManager.getInstance().start();
        //-----------------------------------pengzhou: proximity service--------------------------
        /*if (mProximity != null) {
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_FASTEST);}
        //-----------------------------------pengzhou: orientation service--------------------------
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            mSensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (magneticField != null) {
            mSensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }*/
    }

    @Override
    public void onPause() {
        Log.i(Constants.TAG, " onPause() called.");
        super.onPause();

        if (mInPreview)
            mCamera.stopPreview();

        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.release();
        mCamera = null;
        mInPreview = false;
    //mSensorManager.unregisterListener(this);
        ARManager.getInstance().stop();

    }

    @Override
    public void onStop() {
        Log.i(Constants.TAG, " onStop() called.");
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        Log.i(Constants.TAG, " onDestroy() called.");
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }*/

        //angle = Math.atan2(event.values[0], event.values[1])/(Math.PI/180);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //recoFlag = true;
        return this.onTouchEvent(event);
    }

    private void initPreview(int width, int height) {
        Log.i(Constants.TAG, "initPreview() called");
        if (mCamera != null && mPreviewHolder.getSurface() != null) {
            if (!mCameraConfigured) {
                Camera.Parameters params = mCamera.getParameters();
                params.setPreviewSize(width, height);

                callbackBuffer = new byte[(height + height / 2) * width];
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                params.set("camera-id", 2);
                mCamera.setParameters(params);
                mCameraConfigured = true;
            }

            try {
                mCamera.setPreviewDisplay(mPreviewHolder);
                mCamera.addCallbackBuffer(callbackBuffer);
                mCamera.setPreviewCallbackWithBuffer(frameIMGProcCallback);
            } catch (Throwable t) {
                Log.e(Constants.TAG, "Exception in initPreview()", t);
            }
        }
    }

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(Constants.TAG, " surfaceCreated() called.");
            initPreview(Constants.previewWidth, Constants.previewHeight);
            if (mCameraConfigured && mCamera != null) {
                mCamera.startPreview();
                mCamera.autoFocus(null);
                mInPreview = true;
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(Constants.TAG, " surfaceChanged() called.");
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(Constants.TAG, " surfaceDestroyed() called.");
        }
    };

    Camera.PreviewCallback frameIMGProcCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            mCamera.addCallbackBuffer(callbackBuffer);

            frameID++;
            //pengzhou: in this mode, device only receives and never sends out images
            recoFlag = false;
            //pengzhou : about 0.1 seconds for each pic process, 10pic/second, using 4 phones now, each phone could
            // at most send 2 pic per second.
            //if(frameID % 3 == 0) recoFlag = true;

            if (recoFlag) {
                //String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                long time= System.currentTimeMillis();
                timeCaptured = (double)time;
                timeSend = (double)time;

                //ARManager.getInstance().recognize(frameID, data s);
                //------------------pengzhou: location---------------------------
                ARManager.getInstance().recognizeTime(frameID, data);
                recoFlag = false;
            } else {
                ARManager.getInstance().driveFrame(data);
            }
            mDraw.invalidate();
        }
    };


    class DrawOnTop extends View {
        Paint paintWord;
        Paint paintLine;
        Paint paintAlarm;
        Paint paintIcon;
        private boolean ShowFPS = true;
        private boolean ShowEdge = true;
        private boolean ShowName = true;
        private int preFrameID;
        private float dispScale = (float)(Constants.recoScale*1.4);
        //private int dispScale = Constants.recoScale;
        private Detected[] detecteds;
        private double distance;
        //private Drawable mCustomImage_pedestrian;
        //private Drawable mCustomImage_cup;

        public DrawOnTop(Context context) {
            super(context);

            paintWord = new Paint();
            paintWord.setStyle(Paint.Style.STROKE);
            paintWord.setStrokeWidth(5);
            paintWord.setColor(Color.BLUE);
            paintWord.setTextAlign(Paint.Align.CENTER);
            paintWord.setTextSize(50);

            paintAlarm = new Paint();
            paintAlarm.setStyle(Paint.Style.STROKE);
            paintAlarm.setStrokeWidth(10);
            paintAlarm.setColor(Color.RED);
            paintAlarm.setTextAlign(Paint.Align.CENTER);
            paintAlarm.setTextSize(100);

            paintLine = new Paint();
            paintLine.setStyle(Paint.Style.STROKE);
            paintLine.setStrokeWidth(10);
            paintLine.setColor(Color.GREEN);


            paintIcon = new Paint();
            paintIcon.setColor(Color.CYAN);
            //mCustomImage_pedestrian = ContextCompat.getDrawable(context, R.drawable.pedestrian);
            //mCustomImage_cup = ContextCompat.getDrawable(context, R.drawable.cup);

        }

        public void updateData(int frameID){
            if (ShowFPS) {
                time_n = (int) System.currentTimeMillis();
                fps = 1000 * (frameID - preFrameID) / (time_n - time_o);
                time_o = time_n;
                preFrameID = frameID;
            }
        }

        public double degreesToRadians(double degrees) {
            return degrees * Math.PI / 180;
        }

        public double distanceInKmBetweenEarthCoordinates(double lat1, double lon1, double lat2, double lon2) {
            double earthRadiusKm = 6371;

            double dLat = degreesToRadians(lat2-lat1);
            double dLon = degreesToRadians(lon2-lon1);

            lat1 = degreesToRadians(lat1);
            lat2 = degreesToRadians(lat2);

            double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            return earthRadiusKm * c;
        }

        public void updateData(Detected[] detecteds) {
            this.detecteds = detecteds;
        }

        public void updateOrientationAngles() {
            // Update rotation matrix, which is needed to update orientation angles.
            SensorManager.getRotationMatrix(mRotationMatrix, null,
                    mAccelerometerReading, mMagnetometerReading);

            // "mRotationMatrix" now has up-to-date information.
            //SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);

            SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
            angle = Math.atan2(mAccelerometerReading[0], mAccelerometerReading[2])/(Math.PI/180);

            // "mOrientationAngles" now has up-to-date information.
        }

        @Override
        protected void onDraw(final Canvas canvas) {
            super.onDraw(canvas);
            Bitmap b_cup =BitmapFactory.decodeResource(getResources(), R.drawable.cup);
            Bitmap b_car = BitmapFactory.decodeResource(getResources(),R.drawable.car);
            Bitmap b_bus = BitmapFactory.decodeResource(getResources(),R.drawable.bus);
            Bitmap b_pedestrian = BitmapFactory.decodeResource(getResources(),R.drawable.pedestrian);
            Bitmap b_stopsign = BitmapFactory.decodeResource(getResources(),R.drawable.stopsign);


            if(detecteds != null) {
                for (Detected detected : detecteds) {
                    int width =  Math.round(Math.abs((detected.left - detected.right)) * dispScale);
                    int height = Math.round(Math.abs((detected.bot - detected.top)) * dispScale);
                    if(detected.name=="cup"){
                        Bitmap scaled_cup = Bitmap.createScaledBitmap(b_cup, width, height, true);
                        canvas.drawBitmap(scaled_cup, detected.left*dispScale, detected.top*dispScale, null);
                        //free up Bitmap data
                        scaled_cup.recycle();}
                    if(detected.name.equals("car")){
                        Bitmap scaled_car = Bitmap.createScaledBitmap(b_car, width, height, true);
                        canvas.drawBitmap(scaled_car, detected.left*dispScale, detected.top*dispScale, null);
                        //free up Bitmap data
                        scaled_car.recycle();}
                    else if(detected.name.equals("person")){
                        Bitmap scaled_pedestrian = Bitmap.createScaledBitmap(b_pedestrian, width, height, true);
                        canvas.drawBitmap(scaled_pedestrian, detected.left*dispScale, detected.top*dispScale, null);
                        //free up Bitmap data
                        scaled_pedestrian.recycle();}
                    else if(detected.name.equals("bus")){
                        Bitmap scaled_bus = Bitmap.createScaledBitmap(b_bus, width, height, true);
                        canvas.drawBitmap(scaled_bus, detected.left*dispScale, detected.top*dispScale, null);
                        //free up Bitmap data
                        scaled_bus.recycle();}
                    else if(detected.name.equals("stop sign")){
                        Bitmap scaled_stopsign = Bitmap.createScaledBitmap(b_stopsign, width, height, true);
                        canvas.drawBitmap(scaled_stopsign, detected.left*dispScale, detected.top*dispScale, null);
                        //free up Bitmap data
                        scaled_stopsign.recycle();}
                    else canvas.drawRect(detected.left*dispScale, detected.top*dispScale, detected.right*dispScale, detected.bot*dispScale, paintLine);
                    // pengzhou : printing probability of object
                    //canvas.drawText(detected.name +  ". Prob: " + detected.prob, (detected.left-2)*dispScale, (detected.top-2)*dispScale, paintWord);
                    // pengzhou: print gps of the camera who captures the object
                    //canvas.drawText(detected.name + ": " + (int)detected.lati + "," + (int)detected.longti, (detected.left-2)*dispScale, (detected.top-2)*dispScale, paintWord);
                    // pengzhou : print distance of camera
                    //canvas.drawRect(detected.left*dispScale, detected.top*dispScale, detected.right*dispScale, detected.bot*dispScale, paintLine);
                    //distance = distanceInKmBetweenEarthCoordinates(latitude, longitude, detected.lati, detected.longti);
                    //canvas.drawText(detected.name + ", d: " + new DecimalFormat("##.##").format(distance) + ", o: " +  bearing, detected.left*dispScale, detected.top*dispScale, paintWord);
                    //canvas.drawText(detected.name + ": " + (int)distance, width/2, height/2, paintWord);
                    canvas.drawText(detected.name, detected.left*dispScale, detected.top*dispScale, paintWord);


                }

            }
            //canvas.drawText( ", o: " + bearing, 500,500, paintWord);
        }
    }
}