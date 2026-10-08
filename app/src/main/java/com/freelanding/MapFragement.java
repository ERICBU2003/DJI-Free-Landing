package com.freelanding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dji.common.flightcontroller.LocationCoordinate3D;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.ux.widget.map.MapWidget;

import com.eric.djiapp.R;

public class MapFragment extends Fragment {

    private MapWidget mapWidget;
    private FlightController flightController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.map_fragment, container, false);
        mapWidget = root.findViewById(R.id.djiMap);

        initFlightController();
        return root;
    }

    private void initFlightController() {
        if (DJISDKManager.getInstance().getProduct() == null) return;

        try {
            flightController = DJISDKManager.getInstance()
                    .getProduct().getFlightController();
        } catch (Exception ignored) {}

        if (flightController == null) return;

        // ★ 飞机状态监听 → 更新地图上的飞机位置
        flightController.setStateCallback(state -> {

            double lat = state.getAircraftLocation().getLatitude();
            double lng = state.getAircraftLocation().getLongitude();
            float altitude = state.getAircraftLocation().getAltitude();

            if (lat == 0 || lng == 0) return; // GPS 无效

            // 更新地图飞机位置（UXSDK 内置）
            mapWidget.updateDroneLocation(new LocationCoordinate3D(lat, lng, altitude));
        });
    }
}
