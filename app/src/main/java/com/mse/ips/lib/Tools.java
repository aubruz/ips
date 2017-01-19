package com.mse.ips.lib;


import java.util.ArrayList;

public abstract class Tools {

    public static float distance(Point pointA, Point pointB) {
        return (float) Math.sqrt(Math.pow((pointA.getX() - pointB.getX()), 2) +  Math.pow((pointA.getY() - pointB.getY()), 2));
    }

    public static float getAveragePrecision(ArrayList<Float> precisions){
        float sum = 0;
        for(float value: precisions){
            sum += value;
        }
        return sum / (float) precisions.size();
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
