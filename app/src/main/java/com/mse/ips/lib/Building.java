package com.mse.ips.lib;

import org.json.JSONException;
import org.json.JSONObject;

public class Building {
    private int mId;
    private String mName;
    private String mAddress;

    public Building(JSONObject building) {
        try {
            this.mId = building.getInt("id");
            this.mName = building.getString("name");
            this.mAddress = building.getString("address");
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public Building(int id, String name, String address) {
        this.mId = id;
        this.mName = name;
        this.mAddress = address;
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

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        this.mAddress = address;
    }

    @Override
    public String toString() {
        return String.valueOf(this.mId);
    }
}
