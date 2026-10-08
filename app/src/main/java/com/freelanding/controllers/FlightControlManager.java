package com.freelanding.controllers;

import android.util.Log;

import androidx.annotation.NonNull;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.products.Aircraft;

public class FlightControlManager {

    private static final String TAG = "FlightControlManager";

    private FlightController flightController;
    private boolean isConnected = false;
    private boolean virtualStickEnabled = false;

    // Singleton 模式（全局一个飞控控制器）
    private static FlightControlManager instance = null;

    public static FlightControlManager getInstance() {
        if (instance == null) {
            instance = new FlightControlManager();
        }
        return instance;
    }

    private FlightControlManager() {
        init();
    }

    /**
     * 初始化 FlightController
     */
    // FlightControlManager.java (init() 方法内部)

    public void init() {
        BaseProduct product = DJISDKManager.getInstance().getProduct();

        if (product == null) {
            Log.e(TAG, "DJI 产品未连接"); // 假设您有 Log 和 TAG 定义
            return;
        }

        // 检查产品类型是否是飞机 (Aircraft)
        if (product instanceof Aircraft) {
            // 只有是飞机时，才能安全地强制转换为 Aircraft，并获取 FlightController
            flightController = ((Aircraft) product).getFlightController();
        } else {
            // 如果是遥控器 (RemoteController) 或其他类型，则无法获取
            Log.e(TAG, "当前连接的不是飞行器，无法获取飞控。");
        }

        if (flightController == null) {
            Log.e(TAG, "FlightController 获取失败！");
            return;
        }

        isConnected = true;
        Log.d(TAG, "FlightController 初始化完成");

        // 绑定状态监听
        flightController.setStateCallback(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(FlightControllerState state) {
                // 可以用来实时读取高度/姿态/速度
                float altitude = state.getAircraftLocation().getAltitude();
                float pitch = (float)state.getAttitude().pitch;
                float roll = (float)state.getAttitude().roll;
                float yaw = (float)state.getAttitude().yaw;

                Log.d(TAG, "高度=" + altitude + "  Pitch=" + pitch
                        + "  Roll=" + roll + "  Yaw=" + yaw);
            }
        });
    }

    /**
     * 解锁电机
     */
    public void startMotors() {
        if (!checkConnection()) return;

        flightController.turnOnMotors(error -> {
            if (error == null) Log.d(TAG, "电机已解锁");
            else Log.e(TAG, "解锁电机失败: " + error.getDescription());
        });
    }

    /**
     * 上锁电机
     */
    public void stopMotors() {
        if (!checkConnection()) return;

        flightController.turnOffMotors(error -> {
            if (error == null) Log.d(TAG, "电机已关闭");
            else Log.e(TAG, "关闭电机失败: " + error.getDescription());
        });
    }

    /**
     * 起飞
     */
    public void takeOff() {
        if (!checkConnection()) return;

        flightController.startTakeoff(error -> {
            if (error == null) Log.d(TAG, "自动起飞成功");
            else Log.e(TAG, "自动起飞失败: " + error.getDescription());
        });
    }

    /**
     * 降落
     */
    public void land() {
        if (!checkConnection()) return;

        flightController.startLanding(error -> {
            if (error == null) Log.d(TAG, "自动降落成功");
            else Log.e(TAG, "降落失败: " + error.getDescription());
        });
    }

    /**
     * 启用虚拟摇杆（无人机接管控制）
     */
    public void enableVirtualStick() {
        if (!checkConnection()) return;

        flightController.setVirtualStickModeEnabled(true, error -> {
            if (error == null) {
                virtualStickEnabled = true;
                Log.d(TAG, "Virtual Stick enabled");

                // 设置控制模式
                flightController.setRollPitchControlMode(RollPitchControlMode.ANGLE);
                flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

            } else {
                Log.e(TAG, "启用虚拟摇杆失败: " + error.getDescription());
            }
        });
    }

    /**
     * 禁用虚拟摇杆
     */
    public void disableVirtualStick() {
        if (!checkConnection()) return;

        flightController.setVirtualStickModeEnabled(false, error -> {
            if (error == null) {
                virtualStickEnabled = false;
                Log.d(TAG, "Virtual Stick disabled");
            } else {
                Log.e(TAG, "关闭失败: " + error.getDescription());
            }
        });
    }

    /**
     * 姿态控制（PID 也会用这个接口）
     * @param pitch  前后（负 = 后退）
     * @param roll   左右（负 = 左）
     * @param yawRate 角速度（正 = 逆时针）
     * @param verticalSpeed 上下速度（正 = 上升）
     */
    public void sendAttitude(float pitch, float roll, float yawRate, float verticalSpeed) {
        if (!checkConnection()) return;
        if (!virtualStickEnabled) {
            Log.e(TAG, "Virtual Stick 未启用，无法发送控制指令");
            return;
        }

        FlightControlData data = new FlightControlData(pitch, roll, yawRate, verticalSpeed);
        flightController.sendVirtualStickFlightControlData(data, error -> {
            if (error != null) {
                Log.e(TAG, "发送控制指令失败: " + error.getDescription());
            }
        });
    }

    /**
     * 悬停（四通道全 0）
     */
    public void hover() {
        sendAttitude(0, 0, 0, 0);
    }

    /**
     * 检查是否连接
     */
    private boolean checkConnection() {
        if (!isConnected || flightController == null) {
            Log.e(TAG, "飞控未连接");
            return false;
        }
        return true;
    }
}
