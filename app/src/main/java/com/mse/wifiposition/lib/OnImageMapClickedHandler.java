package com.mse.wifiposition.lib;

import com.mse.wifiposition.ImageMap;

/**
 * Map tapped callback interface
 */
public interface OnImageMapClickedHandler {
    /**
     * Area with 'id' has been tapped
     * @param id
     */
    void onImageMapClicked(int id, ImageMap imageMap);
    /**
     * Info bubble associated with area 'id' has been tapped
     * @param id
     */
    void onBubbleClicked(int id);
}
