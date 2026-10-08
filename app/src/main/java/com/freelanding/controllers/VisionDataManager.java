package com.freelanding.controllers;

import android.util.Log;

/**
 * Vision Data Manager
 *
 * 统一管理 Vision System 的输出：
 *  - centerX, centerY（像素重心）
 *  - target altitude or depth
 *  - valid (是否检测到目标)
 *  - timestamp
 *
 * 这是整个 FreeLanding Pipeline 的数据枢纽
 * 由 VisionProcessor 更新，由 PID 控制模块和 FlightControlManager 读取
 */
public class VisionDataManager {

    private static final String TAG = "VisionDataManager";

    // Singleton
    private static VisionDataManager instance;

    public static VisionDataManager getInstance() {
        if (instance == null) {
            instance = new VisionDataManager();
        }
        return instance;
    }

    // ==============================
    // Vision 数据
    // ==============================
    private float centerX = 0f;      // 重心 X（-1~1 标准化 或 像素）
    private float centerY = 0f;      // 重心 Y
    private float targetAltitude = 0f;  // Vision 推断高度（可选）
    private boolean valid = false;   // 是否检测到降落点

    private long lastUpdateTime = 0; // ms

    // 数据锁
    private final Object lock = new Object();


    /**
     * VisionProcessor 调用：更新数据
     */
    public void updateVisionData(float cx,
                                 float cy,
                                 boolean isValid,
                                 float altitude) {

        synchronized (lock) {
            this.centerX = cx;
            this.centerY = cy;
            this.valid = isValid;
            this.targetAltitude = altitude;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        Log.d(TAG, "[Vision] cx=" + cx + " cy=" + cy
                + " valid=" + isValid + " alt=" + altitude);
    }


    // ========================
    // Getter（线程安全）
    // ========================

    public float getCenterX() {
        synchronized (lock) {
            return centerX;
        }
    }

    public float getCenterY() {
        synchronized (lock) {
            return centerY;
        }
    }

    public float getTargetAltitude() {
        synchronized (lock) {
            return targetAltitude;
        }
    }

    public boolean isValid() {
        synchronized (lock) {
            return valid;
        }
    }

    public long getLastUpdateTime() {
        synchronized (lock) {
            return lastUpdateTime;
        }
    }

    /**
     * Vision 数据是否超时
     * 如果超过 0.3 秒未更新，就认为 Vision 已失效
     */
    public boolean isDataTimeout() {
        long now = System.currentTimeMillis();

        synchronized (lock) {
            return (now - lastUpdateTime) > 300; // 0.3 秒
        }
    }

    /**
     * 重置所有数据
     */
    public void reset() {
        synchronized (lock) {
            centerX = 0;
            centerY = 0;
            targetAltitude = 0;
            valid = false;
            lastUpdateTime = 0;
        }
        Log.d(TAG, "[Vision] reset");
    }
}
