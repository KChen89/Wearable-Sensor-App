// @author: Kemeng Chen: kemengchen@email.arizona.edu
package com.example.sensor.sensorapp;

import android.os.Bundle;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;

import android.os.Handler;
import android.os.Message;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;

import zephyr.android.BioHarnessBT.*;

public class MainActivity extends AppCompatActivity {

    /** Called when the activity is first created. */
    BluetoothAdapter adapter = null;
    BTClient _bt;
    MyConnectedListener _NConnListener;
    private final static int GEN_PACKET = 1200;
    private final static int R_to_R_PACKET= 0x105;
    private final static int ECG_PACKET = 0x106;
    private final static int NewRmssd_PACKET = 0x108;
    private final static int BREATHING_WAVEFORM = 0x109; // in use
    private final static int ZT = 0x110; // in use
    private final static int SUMMARY_PACKET = 0x112;
    private final static int HEART_RATE = 0x100;        // in use
    private final static int RESPIRATION_RATE = 0x101;  // in use

    private final static int CONNECT_SUCCESS = 1;
    private final static int CONNECT_FAIL = 0;
    private final String initId = "00:07:80:9D:8A:E8" ;
    private Thread btThread;

    private static final int ECG_PLOT_LENGTH = 2000;
    private static final int RR_PLOT_LENGTH = 12;
    private static final int RESPW_PLOT_LENGTH = 360;
    private static final int HRV_PLOT_LENGTH = 15;
    private static final int ACT_PLOT_LENGTH = 10;

    private static XYPlot ecgPlot = null;
    private static XYPlot rrPlot = null;
    private static XYPlot respPlot = null;
    private static XYPlot hrvPlot = null;
    private static XYPlot actPlot = null;

    private static SimpleXYSeries ecgSeries = null;
    private static SimpleXYSeries rrSeries = null;
    private static SimpleXYSeries respSeries = null;
    private static SimpleXYSeries hrvSeries = null;
    private static SimpleXYSeries actSeries = null;

    public byte [] DataBytes ;
    private static Context myContext;
    private static ProgressDialog waitDig;
    private ArrayList<String> deviceName;
    private static String ZTime;
//    private static String fileName;
//    private static String folderName = "/ECG_test";
//    private static File fileTitle;
//    private static boolean success = true;
//    private static String newLine = "\r\n";
//    ConnectedListener<BTClient> _Listener ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myContext = this.getApplicationContext();
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setIcon(R.drawable.icon);
        parameterSetting();
        btListener();
    }

    private void parameterSetting() {
        // TODO Auto-generated method stub
        ecgPlot =  (XYPlot) findViewById(R.id.ECGPlot);
        rrPlot = (XYPlot) findViewById(R.id.RRpeakPlot);
//        respPlot = (XYPlot) findViewById(R.id.RespPlot);
//        hrvPlot = (XYPlot) findViewById(R.id.HRVPlot);
//        actPlot = (XYPlot) findViewById(R.id.ActPlot);

        ecgSeries = new SimpleXYSeries("ECG");
        rrSeries = new SimpleXYSeries("Heart Rate");
//        respSeries = new SimpleXYSeries("Respiration waveform");
//        hrvSeries = new SimpleXYSeries("Heart Rate Variability");
//        actSeries = new SimpleXYSeries("Activity");

        ecgSeries.useImplicitXVals();
        rrSeries.useImplicitXVals();
//        respSeries.useImplicitXVals();
//        hrvSeries.useImplicitXVals();
//        actSeries.useImplicitXVals();

        ecgPlot.setRangeBoundaries(-3, 12, BoundaryMode.AUTO);
        rrPlot.setRangeBoundaries(0,250, BoundaryMode.AUTO);
//        respPlot.setRangeBoundaries(-10, 10, BoundaryMode.AUTO);
//        hrvPlot.setRangeBoundaries(0, 500, BoundaryMode.AUTO);
//        actPlot.setRangeBoundaries(-1, 3, BoundaryMode.FIXED);

        ecgPlot.setDomainBoundaries(0, ECG_PLOT_LENGTH, BoundaryMode.FIXED);
        rrPlot.setDomainBoundaries(0, RR_PLOT_LENGTH, BoundaryMode.FIXED);
//        respPlot.setDomainBoundaries(0, RESPW_PLOT_LENGTH, BoundaryMode.FIXED);
//        hrvPlot.setDomainBoundaries(0,HRV_PLOT_LENGTH, BoundaryMode.FIXED);
//        actPlot.setDomainBoundaries(0, ACT_PLOT_LENGTH, BoundaryMode.FIXED);

        ecgPlot.addSeries(ecgSeries, new LineAndPointFormatter(Color.RED, null, null, null));
        rrPlot.addSeries(rrSeries, new LineAndPointFormatter(Color.rgb(250, 250, 250), Color.MAGENTA, null, null));
//        respPlot.addSeries(respSeries, new LineAndPointFormatter(Color.GREEN, null, null, null));
//        hrvPlot.addSeries(hrvSeries, new LineAndPointFormatter(Color.rgb(250,  250,  250), Color.BLUE, null, null));
//        actPlot.addSeries(actSeries, new LineAndPointFormatter(Color.rgb(250,  250,  250), Color.YELLOW, null, null));

        ecgPlot.setDomainStepValue(5);
        rrPlot.setDomainStepValue(5);
//        respPlot.setDomainStepValue(5);
//        hrvPlot.setDomainStepValue(5);
//        actPlot.setDomainStepValue(5);

        ecgPlot.setTicksPerRangeLabel(3);
        rrPlot.setTicksPerRangeLabel(3);
//        respPlot.setTicksPerRangeLabel(3);
//        hrvPlot.setTicksPerRangeLabel(3);
//        actPlot.setTicksPerRangeLabel(3);
    }

    private void btListener() {
        // TODO Auto-generated method stub
        waitDig = new ProgressDialog(this);
        waitDig.setTitle("Please wait ...");
        waitDig.setMessage("Connecting now ...");
        btSearch();
    }

    private void btSearch() {
        // TODO Auto-generated method stub
        if(btThread == null) {
            waitDig.show();
            adapter = BluetoothAdapter.getDefaultAdapter();
            deviceName = new ArrayList<String> ();
            Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
            if(pairedDevices.size() > 0){
                for(BluetoothDevice device : pairedDevices){
                    if(device.getName().startsWith("BH")){
                        deviceName.add(device.getName());
                    }
                }
                btDig(deviceName);
                waitDig.dismiss();
            }
            else{
                Toast toast = Toast.makeText(myContext, "Please pair your phone with BioPatch", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                waitDig.dismiss();
            }
        }
        else{
            Toast toast = Toast.makeText(myContext, "Connection has already been setup", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            waitDig.dismiss();
        }

    }

    protected void btDig(ArrayList<String> nameList) {
        // TODO Auto-generated method stub
        AlertDialog.Builder builder = new Builder(new ContextThemeWrapper(
                this,android.R.style.Theme_Black));
        if(nameList.isEmpty()){
            builder.setTitle("No BioPatch is available");
            builder.setMessage("Please pair your phone with BioPatch");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface btDig, int which) {
                    btDig.dismiss();
                }
            });
        }
        else{
            int numItems = nameList.size();
            final String [] nameItems = new String[numItems] ;
            for(int i=0;i<numItems;i++){
                nameItems[i] = nameList.get(i);
            }
            builder.setTitle("Please choose device");
            builder.setSingleChoiceItems(nameItems, -1, null);
            builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface btDig, int i) {
                    int sp=((AlertDialog) btDig).getListView().getCheckedItemPosition();
                    String DeviceName=nameItems[sp];
                    btConnect(DeviceName);
                    btDig.dismiss();
                }
            });
        }
        builder.create().show();
    }

    private void btConnect(final String BioName) {

        if(btThread == null){
            waitDig.show();
            adapter = BluetoothAdapter.getDefaultAdapter();
            btThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
                    myContext.registerReceiver(new BTBroadcastReceiver(), filter);

                    IntentFilter filter2 = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
                    myContext.registerReceiver(new BTBondReceiver(), filter2);
                    String BhMacID =  initId;

                    Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
                    if(pairedDevices.size() > 0){
                        for(BluetoothDevice device : pairedDevices){
                            if(device.getName().equals(BioName)){
                                BluetoothDevice btDevice = device;
                                BhMacID = btDevice.getAddress();
                                break;
                            }
                        }
                    }
                    else{
//						Toast toast = Toast.makeText(myContext, "Please pair your phone with BioPatch", Toast.LENGTH_LONG);
//    					toast.setGravity(Gravity.CENTER, 0, 0);
//    					toast.show();
                        System.out.println("Empty available devices list !!!");
                    }

                    BluetoothDevice Device = adapter.getRemoteDevice(BhMacID);
                    String DeviceName = Device.getName();
                    _bt = new BTClient(adapter, BhMacID);
                    _NConnListener = new MyConnectedListener(Signalhandler);
                    _bt.addConnectedEventListener(_NConnListener);
                    if(_bt.IsConnected()) {
                        _bt.start();
                        String message = "Connected to BioHarness"+DeviceName;
                        MessageHandler.obtainMessage(CONNECT_SUCCESS, message).sendToTarget();
                    }
                    else{
                        btFailDig();
                    }
                }
            });
            btThread.start();
        }
    }

    static Handler MessageHandler = new Handler() {
        @Override
        public void handleMessage (Message msg){
            switch(msg.what) {
                case CONNECT_SUCCESS:
                    String message = (String) msg.obj;
                    Toast toast = Toast.makeText(myContext, message, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    waitDig.dismiss();
                    break;

                case CONNECT_FAIL:

                    waitDig.dismiss();
                    break;

                default: break;
            }
            super.handleMessage(msg);
        }
    };


    protected void btFailDig(){
        AlertDialog.Builder builder = new Builder(new ContextThemeWrapper(
                this,android.R.style.Theme_Black));
        builder.setTitle("Connection fails !");
        builder.setMessage("Would you like to try again ?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface btFailDig, int which) {
                btSearch();
                btFailDig.dismiss();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface btFailDig, int which) {
                // TODO Auto-generated method stub
                btFailDig.dismiss();
                onDestroy();
            }

        });
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
    }
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event){
//        if(keyCode == KeyEvent.KEYCODE_BACK){
////            onDestroy();
//            return super.onKeyDown(keyCode, event);
//            return true;
//        }
//        else{
//            return false;
//        }
//    }

    protected void onDestroy() {
        super.onDestroy();
        if(_bt != null){
            if(_bt.IsConnected()){
                _bt.removeConnectedEventListener(_NConnListener);
                _bt.Close();
            }
            else{
                _bt = null;
            }
        }
//        if(btThread!=null){
//            if(btThread.isAlive()){
//              try {
//                btThread.join();
//              } catch (InterruptedException e) {
//                e.printStackTrace();
//              }
//            }
//        }
        MainActivity.this.finish();
        System.exit(0);
    }

    private class BTBondReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            BluetoothDevice device = adapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
            Log.d("Bond state", "BOND_STATED = " + device.getBondState());
        }
    }

    private class BTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BTIntent", intent.getAction());
            Bundle b = intent.getExtras();
            Log.d("BTIntent", b.get("android.bluetooth.device.extra.DEVICE").toString());
            Log.d("BTIntent", b.get("android.bluetooth.device.extra.PAIRING_VARIANT").toString());
            try {
                BluetoothDevice device = adapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
                Method m = BluetoothDevice.class.getMethod("convertPinToBytes", new Class[] {String.class} );
                byte[] pin = (byte[])m.invoke(device, "1234");
                m = device.getClass().getMethod("setPin", new Class [] {pin.getClass()});
                Object result = m.invoke(device, pin);
                Log.d("BTTest", result.toString());
            } catch (SecurityException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (NoSuchMethodException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    static Handler Signalhandler = new Handler(){

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case HEART_RATE:
                    String heartRate = msg.getData().getString("HeartRate");
                    Double heartRateV = Double.parseDouble(heartRate);
                    if(rrSeries.size()>RR_PLOT_LENGTH){
                        rrSeries.removeFirst();
                    }
                    rrSeries.addLast(null, heartRateV);
                    rrPlot.redraw();
                    break;

                case RESPIRATION_RATE:
                    String respRate = msg.getData().getString("RespirationRate");
                    break;
                case R_to_R_PACKET:
                    ArrayList<Integer> R2RText; // = new ArrayList<Integer>();
                    R2RText= msg.getData().getIntegerArrayList("RtoRText");
//                    if(!R2RText.isEmpty()){
//                        int dataSize = R2RText.size();
//                        for(int i=0;i<dataSize;i++){
//                            if(rrSeries.size() > RR_PLOT_LENGTH){
//                                rrSeries.removeFirst();
//                            }
//                            rrSeries.addLast(null, 1000.0/R2RText.get(i));
//                            rrPlot.redraw();
//                        }
//                    }
                    break;

                case NewRmssd_PACKET:
                    String NewRMSSDText = msg.getData().getString("NewRMSSD_Text");
//                    int RMSSD = Integer.parseInt(NewRMSSDText);
//                    if(hrvSeries.size() > HRV_PLOT_LENGTH){
//                        hrvSeries.removeFirst();
//                    }
//                    hrvSeries.addLast(null, RMSSD);
//                    hrvPlot.redraw();
                    break;

                case ZT:
                    ZTime = msg.getData().getString("ZephyrTime");
                    break;

                case BREATHING_WAVEFORM:
//                    short ampBW[] = msg.getData().getShortArray("brWaveform");
//                    if(ampBW == null){
//                        System.out.println("ampBW is empty");
//                    }
//                    else{
//                        for(int i=0;i<17;i++){
//                            if(respSeries.size() > RESPW_PLOT_LENGTH){
//                                respSeries.removeFirst();
//                            }
//                            respSeries.addLast(null, ampBW[i]-512);
//                            respPlot.redraw();
//                        }
//                    }
                    break;

                case ECG_PACKET:
                    short ECGtext[] = msg.getData().getShortArray("ecgText");
                    if(ECGtext != null){
                        for(int i=0;i<63;i++){
                            if(ecgSeries.size() > ECG_PLOT_LENGTH){
                                ecgSeries.removeFirst();
                            }
                            ecgSeries.addLast(null, (ECGtext[i]-512));
                            ecgPlot.redraw();
                        }
                    }
                    break;

                case SUMMARY_PACKET:
                    String ActivityText = msg.getData().getString("Activity");
                    double Activity = Double.parseDouble(ActivityText);
//                    if(actSeries.size() > ACT_PLOT_LENGTH){
//                        actSeries.removeFirst();
//                    }
//                    actSeries.addLast(null, Activity);
//                    actPlot.redraw();
                    break;

                default: break;
            }

        }

    };
}
