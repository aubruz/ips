package com.mse.ips.lib;

import com.estimote.sdk.Beacon;
import com.mse.ips.view.MapView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Tools {
    private static HashMap<String, String> mKnownBeacons = null;
    private static HashMap<String, Integer> mBeaconsIdx = null;

    static {
        mKnownBeacons = new HashMap<String, String>();
        mKnownBeacons.put("36366", "ice");
        mKnownBeacons.put("35376", "blueberry 2");
        mKnownBeacons.put("31801", "mint");
        mKnownBeacons.put("25070", "blueberry");
        mKnownBeacons.put("180", "mint 2");
        mKnownBeacons.put("60369", "ice 2");
        mBeaconsIdx = new HashMap<String, Integer>();
        mBeaconsIdx.put("36366", 0);
        mBeaconsIdx.put("35376", 1);
        mBeaconsIdx.put("31801", 2);
        mBeaconsIdx.put("25070", 3);
        mBeaconsIdx.put("180", 4);
        mBeaconsIdx.put("60369", 5);

    }

    public static String getBeaconName(int major){
        return mKnownBeacons.get(String.valueOf(major));
    }

    public static int getIndex(int major){
        return mBeaconsIdx.get(String.valueOf(major));
    }


    public static double[][] getAddResults(double[][] currentValues, List<Beacon> list){

        for (Beacon beacon: list) {
            if(getBeaconName(beacon.getMajor()) == null){
                continue; //Don't mind other beacons that can be around
            }
            currentValues[getIndex(beacon.getMajor())][0] = beacon.getMajor();
            currentValues[getIndex(beacon.getMajor())][1] += beacon.getRssi();
            currentValues[getIndex(beacon.getMajor())][2] = beacon.getMeasuredPower();
        }

        return currentValues;
    }

    public static double[][] initialize(double[][] array){
        for(int i= 0; i < array.length; i++){
            for(int j = 0; j < array[0].length; j++){
                array[i][j] = 0;
            }
        }
        return array;
    }

    public static double getNextValue(double currentValue){
        if(currentValue >= 70.0){
            return -1;
        }
        return currentValue + 1.0d;
    }

    public static float distance(Point pointA, Point pointB, Floor floor, MapView imageView) {
        float widthRatio = floor.getWidth() / imageView.getImageWidth(); // Meters / pixels
        float heightRatio = floor.getHeight() / imageView.getImageHeight(); // Meters / pixels
        return (float) Math.sqrt(Math.pow((pointA.getX() - pointB.getX()) * widthRatio, 2) +  Math.pow((pointA.getY() - pointB.getY()) * heightRatio, 2));
    }

    public static float getAveragePrecision(ArrayList<Float> distances){
        float sum = 0;
        for(float value: distances){
            sum += value;
        }
        return sum / (float) distances.size();
    }

    public static float getStandartDeviation(ArrayList<Float> distances, float mean){
        float sum = 0;
        for(float value: distances){
            sum += Math.pow((value - mean),2);
        }
        return (float) Math.sqrt(sum/ (float)distances.size());
    }

    public static boolean isInteger(String s) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),10) < 0) return false;
        }
        return true;
    }
}
