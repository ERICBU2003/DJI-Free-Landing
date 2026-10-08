package com.freelanding.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

public class UIUtils {

    private static final String TAG = "UIUtils";
    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 在主线程执行
     */
    public static void runOnUiThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    /**
     * 安全 Toast
     */
    public static void showToast(final Context context, final String msg) {
        if (context == null) return;

        runOnUiThread(() ->
                Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * dp → px
     */
    public static int dpToPx(Context context, float dp) {
        if (context == null) return (int) dp;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (dp * metrics.density + 0.5f);
    }

    /**
     * px → dp
     */
    public static int pxToDp(Context context, float px) {
        if (context == null) return (int) px;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (px / metrics.density + 0.5f);
    }

    /**
     * 获取屏幕宽度 px
     */
    public static int getScreenWidth(Activity activity) {
        if (activity == null) return 0;
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    /**
     * 获取屏幕高度 px
     */
    public static int getScreenHeight(Activity activity) {
        if (activity == null) return 0;
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    /**
     * 安全 Debug Log
     */
    public static void log(String msg) {
        if (msg == null) msg = "null";
        Log.d(TAG, msg);
    }

    /**
     * 安全 Error Log
     */
    public static void logError(String msg) {
        if (msg == null) msg = "null";
        Log.e(TAG, msg);
    }

    /**
     * 延迟执行（UI 线程）
     */
    public static void postDelayed(Runnable runnable, long delayMillis) {
        mainHandler.postDelayed(runnable, delayMillis);
    }

    /**
     * 显示短 Toast（简写）
     */
    public static void toast(Context context, String text) {
        showToast(context, text);
    }
}
