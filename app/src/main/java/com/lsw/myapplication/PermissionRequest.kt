package com.lsw.myapplication

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

internal object RequestCode {
    const val BLUETOOTH_PERMS_NEEDED_CODE = 100
}

fun requestAllPermissions(self: MainActivity)
{
    var permissions = arrayListOf<String>();

    do {
        permissions.clear()

        /*if (ActivityCompat.checkSelfPermission(self, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);*/
        if (ActivityCompat.checkSelfPermission(self, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        if (ActivityCompat.checkSelfPermission(self, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (permissions.size > 0) {
            ActivityCompat.requestPermissions(self, permissions.toTypedArray(), RequestCode.BLUETOOTH_PERMS_NEEDED_CODE)
        }
    } while(permissions.size > 0)

}