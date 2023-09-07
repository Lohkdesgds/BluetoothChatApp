package com.lsw.nearbychat

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

internal object RequestCode {
    const val BLUETOOTH_PERMS_NEEDED_CODE = 100
}

fun requestAllPermissions(self: MainActivity)
{
    /*ActivityCompat.requestPermissions(self,arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.ACCESS_WIFI_STATE
    ), RequestCode.BLUETOOTH_PERMS_NEEDED_CODE)*/


    var permissions = arrayListOf<String>();

    if (ActivityCompat.checkSelfPermission(self, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
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
    if (ActivityCompat.checkSelfPermission(self, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
        permissions.add(Manifest.permission.BLUETOOTH_SCAN);

    if (permissions.size > 0) {
        ActivityCompat.requestPermissions(self, permissions.toTypedArray(), RequestCode.BLUETOOTH_PERMS_NEEDED_CODE)
        //sleep(250)
    }

}