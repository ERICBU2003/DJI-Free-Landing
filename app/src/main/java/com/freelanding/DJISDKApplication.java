package com.freelanding;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import dji.common.error.DJIError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class DJISDKApplication extends Application {

    private static final String TAG = "DJISDKApplication";
    private static Application app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        Log.d(TAG, "App start, begin DJI SDK registration…");

        DJISDKManager.getInstance().registerApp(
                this,
                new DJISDKManager.SDKManagerCallback() {
                    @Override
                    public void onRegister(DJIError error) {
                        if (error == null) {
                            Log.d(TAG, "DJI SDK Registration Success");
                            DJISDKManager.getInstance().startConnectionToProduct();
                        } else {
                            Log.e(TAG, "DJI SDK Registration Failed: " + error.getDescription());
                        }
                    }

                    @Override
                    public void onProductConnect(BaseProduct product) {
                        Log.d(TAG, "Product Connected: " + product);
                    }


                    @Override
                    public void onProductDisconnect() {
                        Log.d(TAG, "Product Disconnected");
                    }


                    @Override
                    public void onComponentChange(@NonNull BaseProduct.ComponentKey key,
                                                  @NonNull BaseComponent oldComponent,
                                                  @NonNull BaseComponent newComponent) {
                        Log.d(TAG, "Component Changed: " + key);
                    }

                    @Override
                    public void onInitProcess(DJISDKInitEvent event, int totalProcess) {
                        Log.d(TAG, "Init Process: " + event + " / " + totalProcess);
                    }


                    @Override
                    public void onDatabaseDownloadProgress(long current, long total) {
                        Log.d(TAG, "DB Download: " + current + "/" + total);
                    }
                    @Override
                    public void onProductChanged(BaseProduct product) {
                        Log.d(TAG, "Product changed: " + product);
                    }

                }
        );
    }

    public static Context getAppContext() {
        return app;
    }
}