package com.mse.ips.listener;

import com.mse.ips.lib.Point;
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
     * Info bubble associated with area 'id' has been tapped
     * @param point Point
     */
    void onPointSelected(Point point);

    /**
     * The screen has been tapped tapped
     * @param x float
     * @param y float
     */
    void onScreenTapped(float x, float y);
}
