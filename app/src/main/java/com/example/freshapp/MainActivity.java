package com.example.freshapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.androidplot.Plot;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;

import java.text.DecimalFormat;
import java.util.Arrays;

public class MainActivity extends Activity implements SensorEventListener
{
    private static final int HISTORY_SIZE = 500;
    private SensorManager sensorMgr = null;
    private Sensor orSensor = null;

    private XYPlot aprLevelsPlot = null;
    private XYPlot aprHistoryPlot = null;

    private SimpleXYSeries azimuthHistorySeries = null;
    private double temp = 0;
    private double temp1 = 0;
    private int i = 0;
    private int counter = 0;
    static final float ALPHA = 0.2f;
    TextView steps;
    TextView orientationCh;
    private Redrawer redrawer;

//    public void showToast(View view){
//        Toast.makeText(this, "Hold device in proper orientation!", Toast    .LENGTH_LONG).show())
//    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        steps = (TextView) findViewById(R.id.steps);
        orientationCh = (TextView) findViewById(R.id.orientationCh);
        // setup the APR History plot:
        aprHistoryPlot = (XYPlot) findViewById(R.id.plot);

        azimuthHistorySeries = new SimpleXYSeries("");
        azimuthHistorySeries.useImplicitXVals();

        aprHistoryPlot.setRangeBoundaries(60, 150, BoundaryMode.FIXED);
        aprHistoryPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        aprHistoryPlot.addSeries(azimuthHistorySeries,
                new LineAndPointFormatter(
                        Color.rgb(100, 100, 200), null, null, null));
        aprHistoryPlot.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
        aprHistoryPlot.setDomainStepValue(HISTORY_SIZE/10);
        aprHistoryPlot.setLinesPerRangeLabel(3);
        aprHistoryPlot.getDomainTitle().pack();
        aprHistoryPlot.getRangeTitle().pack();

        aprHistoryPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).
                setFormat(new DecimalFormat("#"));

        aprHistoryPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
                setFormat(new DecimalFormat("#"));

        // register for orientation sensor events:
        sensorMgr = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        for (Sensor sensor : sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER)) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                orSensor = sensor;
            }
        }

        // if we can't access the orientation sensor then exit:
        if (orSensor == null) {
            System.out.println("Failed to attach to orSensor.");
            cleanup();
        }

        sensorMgr.registerListener(this, orSensor, SensorManager.SENSOR_DELAY_UI);

        redrawer = new Redrawer(
                Arrays.asList(new Plot[]{aprHistoryPlot}),
                100, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        redrawer.start();
    }

    @Override
    public void onPause() {
        redrawer.pause();
        super.onPause();
    }


    @Override
    public void onDestroy() {
        redrawer.finish();
        super.onDestroy();
    }

    private void cleanup() {
        // unregister with the orientation sensor before exiting:
        sensorMgr.unregisterListener(this);
        finish();
    }


    // Called whenever a new orSensor reading is taken.
    @Override
    public synchronized void onSensorChanged(SensorEvent sensorEvent) {


        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        if(Math.abs(x)>3||Math.abs(y)<3.2) {
            orientationCh.setVisibility(View.VISIBLE);
            orientationCh.setText("Hold the device properly!");
            onPause();
        }

        else {
            orientationCh.setVisibility(View.INVISIBLE);
            onResume();
            // get rid the oldest sample in history:
            if (azimuthHistorySeries.size() > HISTORY_SIZE)
                azimuthHistorySeries.removeFirst();

            double mag = Math.sqrt(Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2));

            mag = temp1 + ALPHA*(mag - temp1);
            double magsqr = Math.pow(mag,2);
            double tempsqr = Math.pow(temp1,2);

            if((Math.abs(magsqr-tempsqr)>4)&&((Math.abs(tempsqr)<93&&Math.abs(magsqr)>105)||(Math.abs(tempsqr)<105&&Math.abs(magsqr)<93))){
                if(!((Math.abs(magsqr)>115)||Math.abs(magsqr)<83)){
                    counter++;
                }

            }
            int finalcounter = (int) Math.floor(counter);


            // add the latest history sample:
            azimuthHistorySeries.addLast(null, magsqr);
            temp1 = mag;
            steps.setText(String.valueOf(finalcounter));
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Not interested in this event
    }
}