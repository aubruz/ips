package com.mse.ips.lib;

import org.json.JSONException;
import org.json.JSONObject;

public class Floor {
    private int mId;
    private String mName;
    private String mBlueprint;

    public Floor(JSONObject building) {
        try {
            this.mId = building.getInt("id");
            this.mName = building.getString("name");
            this.mBlueprint = building.getString("blueprint");
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
}
