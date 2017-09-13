package me.th3g3ntl3m3n.energyswitch;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.anastr.speedviewlib.ImageSpeedometer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {


    private static int REQUEST_CODE_FOR_WIFI = 123;
    List<ScanResult> scanResultList;
    WifiManager wifiManager;
    ConnectivityManager connectivityManager;
    WiFiListReceiver wifiListReceiver;
    int hasWifiPermission;
    int hasLocationPermission;
    int hasTelephonePermission;
    ImageSpeedometer wifiMeter, networkMeter;
    TextView ipAddress, linkSpeed, snrValue;

    TelephonyManager teleManager;
    String globalIp, localIp;
    int wifiStrength, networkStrength;
    int randomNum = 90;
    FloatingActionButton speedTest;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);
        teleManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        teleManager.listen(new CustomPhoneStateListener2(this), PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.netWorkSwitch);
        wifiMeter = (ImageSpeedometer) findViewById(R.id.bigMeter);
        networkMeter = (ImageSpeedometer) findViewById(R.id.smallMeter);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                switch (checkedId) {
                    case R.id.wifi:
                        wifiMeter = (ImageSpeedometer) findViewById(R.id.bigMeter);
                        networkMeter = (ImageSpeedometer) findViewById(R.id.smallMeter);
                        ipAddress.setText("IP Address : " + localIp);
                        wifiMeter.speedTo(wifiStrength);
                        networkMeter.speedTo(networkStrength);
                        Log.d("Type", "" + networkStrength + " " + wifiStrength);
                        break;
                    case R.id.network:
                        wifiMeter = (ImageSpeedometer) findViewById(R.id.smallMeter);
                        networkMeter = (ImageSpeedometer) findViewById(R.id.bigMeter);
                        ipAddress.setText("IP Address : " + globalIp);
                        wifiMeter.speedTo(wifiStrength);
                        networkMeter.speedTo(networkStrength);
                        Log.d("Type", "" + networkStrength + " " + wifiStrength);
                        break;
                    default:
                        Toast.makeText(MainActivity.this, "MAKE A WISH", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
        getNoiseValue();
        ipAddress = (TextView) findViewById(R.id.ipaddress);
        linkSpeed = (TextView) findViewById(R.id.linkSpeed);
        snrValue = (TextView) findViewById(R.id.snrValue);
        speedTest = (FloatingActionButton) findViewById(R.id.takeSpeedTest);

        speedTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SpeedTest.class);
                startActivity(intent);
            }
        });

        ((TextView) findViewById(R.id.headerText)).setTypeface(Typeface.createFromAsset(getAssets(), "Quicksand-Bold.otf"));

        getGlobalIPAddress();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasWifiPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            hasTelephonePermission = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
            if (hasTelephonePermission != PackageManager.PERMISSION_GRANTED && hasWifiPermission != PackageManager.PERMISSION_GRANTED && hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_FOR_WIFI);
            } else {
                wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                ipAddress.setTypeface(Typeface.createFromAsset(getAssets(), "Quicksand-Bold.otf"));
                linkSpeed.setTypeface(Typeface.createFromAsset(getAssets(), "Quicksand-Bold.otf"));
                snrValue.setTypeface(Typeface.createFromAsset(getAssets(), "Quicksand-Bold.otf"));

                wifiMeter.speedTo((float) (wifiInfo.getRssi()));
                networkMeter.speedTo(new CustomPhoneStateListener(this).getGSmSignal());

                getLocalIpAddress(wifiInfo.getIpAddress());
                linkSpeed.setText("Link Speed : " + wifiInfo.getLinkSpeed() + "Mbps");
                snrValue.setText("WiFi SNR : " + String.valueOf(randomNum + wifiInfo.getRssi()));

                wifiListReceiver = new WiFiListReceiver();
                registerReceiver(wifiListReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                wifiManager.startScan();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 123:
                if (grantResults[0] == PERMISSION_GRANTED) {

                    wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();


                    ipAddress.setTypeface(Typeface.createFromAsset(getAssets(), "Quicksand-Bold.otf"));
                    linkSpeed.setTypeface(Typeface.createFromAsset(getAssets(), "Quicksand-Bold.otf"));
                    snrValue.setTypeface(Typeface.createFromAsset(getAssets(), "Quicksand-Bold.otf"));

                    wifiMeter.speedTo((float) (wifiInfo.getRssi()));
                    networkMeter.speedTo(new CustomPhoneStateListener(this).getGSmSignal());

                    getLocalIpAddress(wifiInfo.getIpAddress());
                    linkSpeed.setText("Link Speed : " + wifiInfo.getLinkSpeed() + "Mbps");
                    snrValue.setText("WiFi SNR : " + String.valueOf(randomNum + wifiInfo.getRssi()));

                    wifiListReceiver = new WiFiListReceiver();
                    registerReceiver(wifiListReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                    wifiManager.startScan();
                } else {
                    Toast.makeText(MainActivity.this, "Provide permissions", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void getGlobalIPAddress() {
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();
        Retrofit retrofit = new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://jpss.herokuapp.com").build();

        IpInterface ipInterface = retrofit.create(IpInterface.class);
        Call<IPAddress> getIp = ipInterface.getIPAddress();
        getIp.enqueue(new Callback<IPAddress>() {
            @Override
            public void onResponse(@NonNull Call<IPAddress> call, @NonNull Response<IPAddress> response) {
                globalIp = response.body().getAddress();
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<IPAddress> call, Throwable t) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(wifiListReceiver);
        super.onDestroy();
    }

    private void getLocalIpAddress(int myIp) {
        int intMyIp3 = myIp / 0x1000000;
        int intMyIp3mod = myIp % 0x1000000;

        int intMyIp2 = intMyIp3mod / 0x10000;
        int intMyIp2mod = intMyIp3mod % 0x10000;

        int intMyIp1 = intMyIp2mod / 0x100;
        int intMyIp0 = intMyIp2mod % 0x100;

        localIp = String.valueOf(intMyIp0) + "." + String.valueOf(intMyIp1)
                + "." + String.valueOf(intMyIp2)
                + "." + String.valueOf(intMyIp3);

        ipAddress.setText("IP Address : " + String.valueOf(intMyIp0) + "." + String.valueOf(intMyIp1)
                + "." + String.valueOf(intMyIp2)
                + "." + String.valueOf(intMyIp3)
        );
    }

    public void getNoiseValue() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog, null);
        dialogBuilder.setView(dialogView);
        final EditText edt = (EditText) dialogView.findViewById(R.id.edit1);
        edt.setInputType(InputType.TYPE_CLASS_NUMBER);
        edt.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        dialogBuilder.setTitle("Enter Noise");
        dialogBuilder.setMessage("Please consider entering a correct integer value");
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                randomNum = Integer.parseInt(edt.getText().toString());
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    class WiFiListReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanResultList = wifiManager.getScanResults();

            wifiStrength = (wifiManager.getConnectionInfo().getRssi());
            wifiMeter.speedTo((float) wifiManager.getConnectionInfo().getRssi());
            linkSpeed.setText("Link Speed : " + wifiManager.getConnectionInfo().getLinkSpeed() + "Mbps");
            snrValue.setText("WiFi SNR : " + String.valueOf(randomNum + wifiManager.getConnectionInfo().getRssi()));

            wifiManager.startScan();
        }
    }

    public class CustomPhoneStateListener2 extends PhoneStateListener {

        private static final String LOG_TAG = "CPSL";
        Context context;

        public CustomPhoneStateListener2(Context context) {
            this.context = context;
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            if (signalStrength.isGsm()) {
                Log.i(LOG_TAG, "onSignalStrengthsChanged: getGsmBitErrorRate "
                        + signalStrength.getGsmBitErrorRate());
                Log.i(LOG_TAG, "onSignalStrengthsChanged: getGsmSignalStrength "
                        + signalStrength.getGsmSignalStrength());
                if (signalStrength.getGsmSignalStrength() != 99) {
                    networkStrength = (2 * signalStrength.getGsmSignalStrength() - 113);
                    networkMeter.speedTo((2 * signalStrength.getGsmSignalStrength() - 113));

                }
            }

            try {
                Method[] methods = SignalStrength.class
                        .getMethods();
                for (Method mthd : methods) {

                    if (mthd.getName().equals("getLteSignalStrength")) {
                        int valueOfStrength = (int) mthd.invoke(signalStrength);
                        if (valueOfStrength != 99) {
                            networkStrength = (valueOfStrength - 113);
                            networkMeter.speedTo((valueOfStrength - 113));
                        }
                    }
                }
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
