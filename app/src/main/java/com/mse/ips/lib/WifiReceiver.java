package com.mse.ips.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import com.mse.ips.listener.OnReceiveWifiScanResult;

import java.util.ArrayList;


public class WifiReceiver extends BroadcastReceiver {
    private ArrayList<OnReceiveWifiScanResult> mListeners;
    private WifiManager mWifiManager;

    public WifiReceiver(WifiManager wifiManager) {
        this.mWifiManager = wifiManager;
    }

    // This method is called when the a new set of results is available
    public void onReceive(Context c, Intent intent) {
        if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
            for (OnReceiveWifiScanResult listener : mListeners) {
                listener.onWifiScanResult(mWifiManager.getScanResults());
            }
        }
    }

    public void addOnReceiveWifiScanResult(OnReceiveWifiScanResult handler ) {
        if (handler != null) {
            if (mListeners == null) {
                mListeners = new ArrayList<>();
            }
            mListeners.add(handler);
        }
    }

    public void removeOnReceiveWifiScanResult(OnReceiveWifiScanResult handler ) {
        if (mListeners != null && handler != null) {
            mListeners.remove(handler);
        }
    }
}
