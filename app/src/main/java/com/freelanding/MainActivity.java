package com.freelanding;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import dji.common.flightcontroller.GPSSignalLevel;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.codec.DJICodecManager;

import com.freelanding.controllers.FlightControlManager;
import com.freelanding.controllers.VideoFeedManager;
import com.freelanding.controllers.VisionDataManager;
import com.freelanding.MapFragment;

import com.freelanding.utils.UIUtils;

public class MainActivity extends AppCompatActivity {

    // UI 控件
    private SurfaceView videoSurface;
    private TextView txtConnection, txtBattery, txtGPS;
    private TextView txtCx, txtCy, txtValid;

    private Button btnTakeoff, btnLand, btnHover, btnVS, btnMap;

    // DJI
    private FlightController flightController;
    private BaseProduct product;

    // 【新增成员变量】用于处理视频解码
    private DJICodecManager mCodecManager = null;

    // 状态
    private boolean virtualStickEnabled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        initDJI();
        initVideo();
        initButtons();
        startStatusUpdateLoop();
        startVisionUpdateLoop();
    }

    // ============================================================
    //  UI INIT
    // ============================================================
    private void initUI() {

        videoSurface = findViewById(R.id.videoSurface);

        txtConnection = findViewById(R.id.txtConnection);
        txtBattery = findViewById(R.id.txtBattery);
        txtGPS = findViewById(R.id.txtGPS);

        txtCx = findViewById(R.id.txtCx);
        txtCy = findViewById(R.id.txtCy);
        txtValid = findViewById(R.id.txtValid);

        btnTakeoff = findViewById(R.id.btnTakeoff);
        btnLand = findViewById(R.id.btnLand);
        btnHover = findViewById(R.id.btnHover);
        btnVS = findViewById(R.id.btnVS);
        btnMap = findViewById(R.id.btnMap);
    }

    // ============================================================
    //  DJI INIT
    // ============================================================
    private void initDJI() {

        product = DJISDKManager.getInstance().getProduct();

        if (product == null || !product.isConnected()) {
            txtConnection.setText("Disconnected");
            return;
        }

        txtConnection.setText("Connected");

        if (product instanceof Aircraft) {
            flightController = ((Aircraft) product).getFlightController();

            // 飞控状态监听
            flightController.setStateCallback(state -> {
                UIUtils.runOnUiThread(() -> {
                    // GPS
                    GPSSignalLevel gps = state.getGPSSignalLevel();
                    txtGPS.setText("GPS: " + gps.value());

                    // 未来你可以加入高度/航向显示
                });
            });
        }

        // 电池监听
        if (product.getBattery() != null) {
            product.getBattery().setStateCallback(batteryState -> {
                UIUtils.runOnUiThread(() -> {
                    int percent = batteryState.getChargeRemainingInPercent();
                    txtBattery.setText("Battery: " + percent + "%");
                });
            });
        }
    }

    // ============================================================
    //  VIDEO INIT 【已修改】
    // ============================================================
    private void initVideo() {

        videoSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // 【核心修改】使用 DJICodecManager 来处理 SurfaceView 的 SurfaceHolder
                if (mCodecManager == null) {
                    mCodecManager = new DJICodecManager(
                            MainActivity.this, // Context
                            holder,             // SurfaceHolder
                            videoSurface.getWidth(), // 宽度
                            videoSurface.getHeight() // 高度
                    );
                }
                // 原有的 VideoFeedManager.getInstance().bindSurface(...) 调用已删除
                // 因为它期望的是 SurfaceTexture，与 SurfaceView 不兼容
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // 当 Surface 尺寸改变时，通知解码器
                if (mCodecManager != null) {
                    mCodecManager.onSurfaceSizeChanged(width, height, 0);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // 销毁解码器
                if (mCodecManager != null) {
                    mCodecManager.destroyCodec();
                    mCodecManager = null;
                }
            }
        });

        // 设置视频帧回调 → Vision System
        VideoFeedManager.getInstance().setVideoCallback((yuv, w, h) -> {
            // 你之后会在这里把 yuv → OpenCV → VisionProcessor
            // 当前先不做图像处理
        });
    }

    // ============================================================
    //  BUTTON INIT
    // ============================================================
    private void initButtons() {

        btnTakeoff.setOnClickListener(v -> {
            UIUtils.toast(this, "Taking off…");
            FlightControlManager.getInstance().takeOff();
        });

        btnLand.setOnClickListener(v -> {
            UIUtils.toast(this, "Landing…");
            FlightControlManager.getInstance().land();
        });

        btnHover.setOnClickListener(v -> {
            UIUtils.toast(this, "Hover");
            FlightControlManager.getInstance().hover();
        });

        btnVS.setOnClickListener(v -> {
            if (!virtualStickEnabled) {
                FlightControlManager.getInstance().enableVirtualStick();
                virtualStickEnabled = true;
                btnVS.setText("VS OFF");
                UIUtils.toast(this, "Virtual Stick Enabled");
            } else {
                FlightControlManager.getInstance().disableVirtualStick();
                virtualStickEnabled = false;
                btnVS.setText("VS ON");
                UIUtils.toast(this, "Virtual Stick Disabled");
            }
        });

        btnMap.setOnClickListener(v -> {
            // 切换到地图 Fragment
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.rootLayout, new com.freelanding.MapFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    // ============================================================
    //  状态栏自动刷新
    // ============================================================
    private void startStatusUpdateLoop() {
        new Thread(() -> {
            while (true) {
                UIUtils.runOnUiThread(() -> {

                    if (product == null || !product.isConnected()) {
                        txtConnection.setText("Disconnected");
                    } else {
                        txtConnection.setText("Connected");
                    }
                });

                try { Thread.sleep(1000); } catch (Exception ignored) {}
            }
        }).start();
    }

    // ============================================================
    //  Vision 自动刷新
    // ============================================================
    private void startVisionUpdateLoop() {
        new Thread(() -> {
            while (true) {

                float cx = VisionDataManager.getInstance().getCenterX();
                float cy = VisionDataManager.getInstance().getCenterY();
                boolean valid = VisionDataManager.getInstance().isValid();

                UIUtils.runOnUiThread(() -> {
                    txtCx.setText("cx: " + cx);
                    txtCy.setText("cy: " + cy);
                    txtValid.setText("valid: " + valid);
                });

                try { Thread.sleep(100); } catch (Exception ignored) {}
            }
        }).start();
    }
}