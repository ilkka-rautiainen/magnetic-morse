package fi.datahiiri.magneticmorse;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class MainActivity extends Activity implements SensorEventListener {

    final String TAG = "MagneticMorse";

    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    float[] mGeomagnetic;
    float[] mGravity;
    double azimuth;  // View to draw a compass
    Double baseAzimuth = null;

    final int startAzimuth = -70;
    final int startThreshold = 10;
    final float threshold = 80;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float Rb[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(Rb, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(Rb, orientation);
                azimuth = orientation[0] * 360 / (2 * Math.PI);
                double pitch = orientation[1] * 360 / (2 * Math.PI);
                double roll = orientation[2] * 360 / (2 * Math.PI);

                if (baseAzimuth == null && azimuth > startAzimuth - startThreshold && azimuth < startAzimuth + startThreshold) {
                    baseAzimuth = azimuth;
                    ((TextView) findViewById(R.id.baseAzimuth)).setText(Long.toString(Math.round(baseAzimuth)));
                }
                if (baseAzimuth == null) {
                    return;
                }

                ((TextView) findViewById(R.id.pitch)).setText(Long.toString(Math.round(pitch)));
                ((TextView) findViewById(R.id.roll)).setText(Long.toString(Math.round(roll)));

                ((TextView) findViewById(R.id.azimuth)).setText(Long.toString(Math.round(azimuth)));

                if (baseAzimuth != null) {
                    if (isAwayFromBase(azimuth)) {
                        ((TextView) findViewById(R.id.difference)).setText("On");
                        tickOn();
                    }
                    else {
                        ((TextView) findViewById(R.id.difference)).setText("Off");
                        tickOff();
                    }
                }
                else {
                    ((TextView) findViewById(R.id.difference)).setText("");
                }
            }
        }
    }

    private boolean isAwayFromBase(double azimuth) {
        return (Math.min(Math.min(
                Math.abs(azimuth - baseAzimuth),
                Math.abs(azimuth - 180) + Math.abs(baseAzimuth + 180)),
                Math.abs(azimuth + 180) + Math.abs(baseAzimuth - 180)
        ) > threshold);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private boolean morseStatus = false;
    private void tickOn() {
        if (!morseStatus) {
            morseStatus = true;
            Log.d(TAG, "Tick ON");
        }
    }

    private void tickOff() {
        if (morseStatus) {
            morseStatus = false;
            Log.d(TAG, "Tick OFF");
        }
    }
}
