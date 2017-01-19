package com.mse.ips.lib;


import android.widget.ImageView;

import com.mse.ips.view.MapView;

import java.util.ArrayList;

public abstract class Tools {

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
