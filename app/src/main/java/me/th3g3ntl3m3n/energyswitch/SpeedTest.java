package me.th3g3ntl3m3n.energyswitch;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SpeedTest extends AppCompatActivity {

    float y = 0;
    List<Entry> ydir;
    LineChart lineChart;
    LineDataSet dataSet;
    LineData data;
    TextView bitrate, maxSpeed;
    int maxSpeedValue = 0;
    FloatingActionButton chooseFile;
    int speedLimit;
    SpeedTestTask speedTestTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);

        speedLimit = 1;

        ydir = new ArrayList<>();
        ydir.add(new Entry(0.0f, 0.0f));
        lineChart = (LineChart) findViewById(R.id.lineChart);
//        lineChart = new LineChart(this);
        dataSet = new LineDataSet(ydir, "KB/s");
        data = new LineData(dataSet);
        lineChart.setData(data);
        lineChart.invalidate();
        dataSet.setFillColor(Color.RED);
        dataSet.setDrawFilled(true);
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        YAxis right = lineChart.getAxisRight();
        right.setDrawLabels(false); // no axis labels
        right.setDrawAxisLine(false); // no axis line
        lineChart.getAxisLeft().setValueFormatter(new XisDataFormatter());

        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getAxisRight().setTextColor(Color.WHITE);
        lineChart.getXAxis().setTextColor(Color.WHITE);

        bitrate = (TextView) findViewById(R.id.bitrate);
        maxSpeed = (TextView) findViewById(R.id.maxSpeed);
        bitrate.setText("Speed : 0.0 Kbps");
        maxSpeed.setText("Max Speed : 0.0 Kbps");

        dataSet.setValueFormatter(new LargeValueFormatter());
        speedTestTask = new SpeedTestTask();
        speedTestTask.execute("http://2.testdebit.info/fichiers/10Mo.dat");
    }

    @Override
    protected void onDestroy() {
        if (speedTestTask != null) {
            speedTestTask.cancel(true);
        }
        super.onDestroy();
    }

    private class SpeedTestTask extends AsyncTask<String, String, String> {
        long startTime;

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            y++;
            Log.d("TAG", "onProgressUpdate: " + values[0] + " " + y);

            dataSet.addEntry(new Entry(y, Float.valueOf(values[0])));
            data.notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();

            double temp = Double.parseDouble(values[0]);
            String speed;

            speed = (temp > 1000) ? temp / 1000 + " Mbps" : temp + " Kbps";
            bitrate.setText("Speed : " + speed);
            maxSpeedValue = Math.max(maxSpeedValue, (int) temp);

            String maxSpeedString = (maxSpeedValue > 1000) ? maxSpeedValue / 1000.0 + " Mbps" : maxSpeedValue + " Kbps";
            maxSpeed.setText("Max Speed : " + maxSpeedString);
        }


        @Override
        protected String doInBackground(String... paramVarArgs) {
            long UPDATE_DELAY = 800;
            int i = 0;
            try {
                URL ulrn = new URL(paramVarArgs[0]);
                HttpURLConnection con = (HttpURLConnection) ulrn.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                InputStream is = con.getInputStream();

                byte[] buffer = new byte[1024 * speedLimit];
                int bufferLength = 0;

                startTime = System.currentTimeMillis();
                while ((bufferLength = is.read(buffer)) > 0 && !Thread.interrupted()) {
                    i += bufferLength;
                    long currentTime = System.currentTimeMillis();
                    if ((currentTime - startTime) > UPDATE_DELAY) {

                        if (i > (speedLimit * 1024 * 1000)) {
                            i = (speedLimit * 1024 * 1000);
                        }

                        publishProgress(String.valueOf(i / 1000.0));
                        i = 0;
                        startTime = currentTime;
                    }
                }

                publishProgress(String.valueOf(i / 1000.0));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }
    }

    public class XisDataFormatter implements IAxisValueFormatter {

        private DecimalFormat mFormat;

        public XisDataFormatter() {
            mFormat = new DecimalFormat("###.##");
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            String newValue;
            if (value > 1000) {
                value /= 1000.0;
                newValue = mFormat.format(value) + " Mbps";
            } else {
                newValue = mFormat.format(value) + " Kbps";
            }
            return newValue;
        }
    }
}
