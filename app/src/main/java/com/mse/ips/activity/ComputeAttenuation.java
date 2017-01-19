package com.mse.ips.activity;

import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.mse.ips.R;
import com.mse.ips.lib.CSVUtils;
import com.mse.ips.lib.Tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ComputeAttenuation extends AppCompatActivity {
    private Button mButtonStart = null;
    private Button mButtonStop = null;
    private TextView mInstructionText = null;
    private TextView mCountText = null;
    private int mCount = 0;
    private BeaconManager beaconManager;
    private Region region;
    private double currentValue = 0;
    private double[][] resultsTmp = new double[6][3];
    private boolean isProcessing = false;
    private HashMap<String, double[][]> mResults = null;
    private boolean isAnalysing = false;
    private final int SAMPLE_NUMBER = 10;
    private final NumberFormat oneDecimalFormatter = new DecimalFormat("#0.0");
    private final NumberFormat twoDecimalFormatter = new DecimalFormat("#0.00");
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compute_attenuation);
        mResults =  new HashMap<>(); // <Distance, Array of [Major, Rssi, Power] for each beacon (6 in this case)>
        resultsTmp = Tools.initialize(resultsTmp);

        //Check for permissions
        int permission = ActivityCompat.checkSelfPermission(ComputeAttenuation.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    ComputeAttenuation.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        // Beacon Manager
        beaconManager = new BeaconManager(this);
        beaconManager.setBackgroundScanPeriod(5000,5000);
        beaconManager.setRangingListener((region, list) -> {
            if (!list.isEmpty() && isProcessing) {
                mCountText.setText(String.valueOf(10 - mCount));
                resultsTmp = Tools.getAddResults(resultsTmp, list);

                mCount++;
                if(mCount -1 >= SAMPLE_NUMBER && !mResults.containsKey(oneDecimalFormatter.format(currentValue))){
                    isProcessing = false;
                    mCount = 0;
                    mCountText.setText("");

                    // Compute average
                    for(int i =0; i < resultsTmp.length; i++ ){
                        resultsTmp[i][1] /= (double)SAMPLE_NUMBER;
                    }

                    // Store values
                    mResults.put(oneDecimalFormatter.format(currentValue), resultsTmp);

                    // Next value
                    currentValue = Tools.getNextValue(currentValue);

                    if(currentValue == -1){
                        showPopup("Fin de l'analyse.");
                        saveResults(mResults);
                        endAnalytics();
                    }else{
                        resultsTmp = Tools.initialize(new double[6][3]);
                        mInstructionText.setText("Veuillez placer les beacons à " + oneDecimalFormatter.format(currentValue) + "m du natel");
                        mButtonStart.setEnabled(true);
                        mButtonStop.setEnabled(true);
                    }
                }
            }
        });
        region = new Region("ranged region", UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);

        // Widgets
        mInstructionText = (TextView) findViewById(R.id.instruction_text);
        mCountText = (TextView) findViewById(R.id.count_text);
        mButtonStop = (Button) findViewById(R.id.button_stop);
        mButtonStop.setEnabled(false);
        mButtonStop.setOnClickListener(v -> {
            showPopup("Fin de l'analyse.");
            saveResults(mResults);
            endAnalytics();
        });

        mButtonStart = (Button) findViewById(R.id.button_start);
        mButtonStart.setOnClickListener(v -> {
            if(isAnalysing){
                mButtonStart.setEnabled(false);
                mButtonStop.setEnabled(false);
                isProcessing = true;
            }else {
                startAnalytics();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isAnalysing){
            beaconManager.connect(() -> beaconManager.startRanging(region));
        }
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
    }

    @Override
    protected void onPause() {
        beaconManager.stopRanging(region);

        super.onPause();
    }

    private void startAnalytics(){
        isAnalysing = true;
        mButtonStart.setText(R.string.ok);
        beaconManager.connect(() -> beaconManager.startRanging(region));
        mInstructionText.setText(R.string.placement_initial);
        currentValue = 1.0;

    }

    private void endAnalytics(){
        mInstructionText.setText(R.string.analyse);
        isAnalysing = false;
        mButtonStart.setEnabled(true);
        beaconManager.stopRanging(region);
    }

    private int showPopup(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(ComputeAttenuation.this);

        builder.setMessage(msg).show();
        return 0;
    }

    private boolean saveResults(HashMap<String, double[][]> results){

        List<String> line = new ArrayList<>();
        double[][] array;

        try{
            File csvFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), " attenuation.csv");
            if(!csvFile.exists()){
                if(!csvFile.createNewFile()){
                    Toast.makeText(this, R.string.csv_file_not_created, Toast.LENGTH_SHORT).show();
                }
            }
            FileWriter writer = new FileWriter(csvFile, false);
            boolean firstLine = true;
            for (String key : results.keySet()) {
                array = results.get(key);
                if(firstLine) {
                    // First line <=> Column title
                    line.add("");
                    for (double[] anArray : array) {
                        line.add(Tools.getBeaconName((int) anArray[0]));
                    }
                    CSVUtils.writeLine(writer, line);
                    line.clear();
                    firstLine = false;
                }
                // Add line of result
                line.add(key);
                for (double[] anArray : array) {
                    line.add(twoDecimalFormatter.format(anArray[1]));
                }
                CSVUtils.writeLine(writer, line);
                line.clear();
            }
            writer.flush();
            writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d("WRITE", "Permission granted!");

                } else {
                    finish();
                }
            }
        }
    }
}
