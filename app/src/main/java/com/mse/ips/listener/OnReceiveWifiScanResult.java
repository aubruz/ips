package com.mse.ips.listener;

import android.net.wifi.ScanResult;

import java.util.List;

/**
 * Wifi scan results callback
 */

public interface OnReceiveWifiScanResult {
    /**
     * List of wifi scan results
     * @param results List<ScanResult>
     */
    void onWifiScanResult(List<ScanResult> results);
}
