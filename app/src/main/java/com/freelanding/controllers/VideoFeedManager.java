// File: app/src/main/java/com/freelanding/controllers/VideoFeedManager.java
package com.freelanding.controllers;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.TextureView;

import dji.common.camera.SettingsDefinitions;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.products.Handheld;                     // ← 必须有这行
import dji.sdk.sdkmanager.DJISDKManager;
import android.util.Log;

public class VideoFeedManager implements TextureView.SurfaceTextureListener {

    private static final String TAG = "VideoFeedManager";

    private TextureView textureView;
    private DJICodecManager codecManager;

    // Inspire 2 只有主摄像机这一路（ZENMUSE X5S/X7）
    private VideoFeeder.VideoFeed primaryVideoFeed;

    public VideoFeedManager(TextureView textureView) {
        this.textureView = textureView;
        this.textureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "Surface 创建成功: " + width + "x" + height);

        if (codecManager == null) {
            codecManager = new DJICodecManager(textureView.getContext(), surface, width, height);
        }

        // 关键：Inspire 2 必须用 PhysicalSource.HANDHELD_CAMERA（老版 API）
        primaryVideoFeed = VideoFeeder.getInstance().provideTranscodedVideoFeed(
                VideoFeeder.PhysicalSource.HANDHELD_CAMERA);

        primaryVideoFeed.setCallback(new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (codecManager != null) {
                    codecManager.sendDataToDecoder(videoBuffer, size,
                            VideoFeeder.PhysicalSource.HANDHELD_CAMERA.getIndex());
                }
            }
        });

        Log.i(TAG, "Inspire 2 主视频流已成功绑定");
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager = new DJICodecManager(textureView.getContext(), surface, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager.destroy();
            codecManager = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // 不需要处理
    }

    public void onResume() {
        // Activity onResume 时重新绑定（防止切后台画面花屏）
        if (textureView != null && textureView.isAvailable() && codecManager == null) {
            onSurfaceTextureAvailable(textureView.getSurfaceTexture(),
                    textureView.getWidth(), textureView.getHeight());
        }
    }

    public void onPause() {
        if (codecManager != null) {
            codecManager.cleanSurface();
        }
    }

    public void release() {
        if (primaryVideoFeed != null) {
            primaryVideoFeed.setCallback(null);
        }
        if (codecManager != null) {
            codecManager.destroy();
            codecManager = null;
        }
    }
}