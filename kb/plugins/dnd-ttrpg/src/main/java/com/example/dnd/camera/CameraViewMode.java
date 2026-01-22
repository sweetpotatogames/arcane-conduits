package com.example.dnd.camera;

/**
 * Represents the different camera view modes available in the D&D plugin.
 */
public enum CameraViewMode {
    /**
     * Standard top-down view with pitch=-90° (straight down), distance=25
     */
    TOPDOWN,

    /**
     * Angled isometric view with pitch=-45°, distance=20
     */
    ISOMETRIC,

    /**
     * User-adjusted custom view with manually set pitch, yaw, distance, and pan
     */
    CUSTOM,

    /**
     * First-person view (isFirstPerson=true)
     */
    FIRST_PERSON
}
