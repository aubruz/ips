package com.mse.wifiposition.view.widget;

public class SpinnerItem {
    private String name;
    private String tag;

    public SpinnerItem(String name, String tag) {
        this.name = name;
        this.tag = tag;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public String getTag(){
        return this.tag;
    }

    public String getName(){
        return this.name;
    }
}
