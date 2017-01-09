package com.mse.ips.listener;

import com.mse.ips.lib.Point;

/**
 * Map tapped callback interface
 */
public interface OnMapViewClickListener {
    /**
     * A point has been selected
     * @param point Point
     */
    void onPointSelected(Point point);

    /**
     * The screen has been tapped, we return the coordinates x and y of the touch
     * @param x float
     * @param y float
     */
    void onScreenTapped(float x, float y);
}
