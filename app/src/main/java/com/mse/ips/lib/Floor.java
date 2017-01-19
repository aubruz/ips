package com.mse.ips.lib;

import org.json.JSONException;
import org.json.JSONObject;

public class Floor {
    private int mId;
    private String mName;
    private String mBlueprint;
    private float mHeight;
    private float mWidth;

    public Floor(JSONObject floor) {
        try {
            this.mId = floor.getInt("id");
            this.mName = floor.getString("name");
            this.mBlueprint = floor.getString("blueprint");
            this.mHeight = (float) floor.getDouble("height");
            this.mWidth = (float) floor.getDouble("width");
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public Floor(int id, String name, String blueprint) {
        this.mId = id;
        this.mName = name;
        this.mBlueprint = blueprint;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getBlueprint() {
        return mBlueprint;
    }

    public void setBlueprint(String blueprint) {
        this.mBlueprint = blueprint;
    }

    public float getWidth() {
        return this.mWidth;
    }

    public void setWidth(float width) {
        this.mWidth = width;
    }

    public float getHeight() {
        return this.mHeight;
    }

    public void setmHeight(float height) {
        this.mHeight = height;
    }

    @Override
    public String toString() {
        return String.valueOf(this.mName);
    }
}
