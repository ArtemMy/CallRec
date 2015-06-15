package com.artem.callrec;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

/*
 * Created by 805268 on 27.04.2015.
 */
public class DeviceAdminDemo extends DeviceAdminReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    public void onEnabled(Context context, Intent intent) {
    };

    public void onDisabled(Context context, Intent intent) {
    };
}