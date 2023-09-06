package com.lsw.nearbychat

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.lang.Thread.sleep

internal object RequestCode {
    const val BLUETOOTH_PERMS_NEEDED_CODE = 100
}

fun requestAllPermissions(self: MainActivity)
{
    var permissions = arrayListOf<String>();

    /*if (ActivityCompat.checkSelfPermission(self, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT);*/
    if (ActivityCompat.checkSelfPermission(self, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
    if (ActivityCompat.checkSelfPermission(self, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
    if (ActivityCompat.checkSelfPermission(self, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
        permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
    if (ActivityCompat.checkSelfPermission(self, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED)
        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
    if (ActivityCompat.checkSelfPermission(self, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);

    if (permissions.size > 0) {
        ActivityCompat.requestPermissions(self, permissions.toTypedArray(), RequestCode.BLUETOOTH_PERMS_NEEDED_CODE)
        sleep(250)
    }

}