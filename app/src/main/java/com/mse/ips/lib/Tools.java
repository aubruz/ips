package com.mse.ips.lib;


import java.util.ArrayList;

public class Tools {

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
}
