package com.mse.ips.listener;

import com.mse.ips.view.MapView;

/**
 * Map tapped callback interface
 */
public interface OnMapViewClickListener {
    /**
     * Area with 'id' has been tapped
     * @param id int
     * @param imageMap ImageMap
     */
    void onImageMapClicked(int id, MapView imageMap);

    /**
     * Info bubble associated with area 'id' has been tapped
     * @param id int
     */
    void onBubbleClicked(int id);

    /**
     * The screen has been tapped tapped
     * @param x float
     * @param y float
     */
    void onScreenTapped(float x, float y);
}
