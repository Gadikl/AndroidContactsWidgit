package com.gadi.contactgrid

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle

/**
 * Invisible trampoline launched by a widget tap.
 * Checks CALL_PHONE at tap time: calls directly if allowed, otherwise opens the dialer
 * with the number filled in.
 */
class CallActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val number = intent.getStringExtra(EXTRA_NUMBER)
        if (!number.isNullOrBlank()) {
            val uri = Uri.fromParts("tel", number, null)
            val canCall = checkSelfPermission(Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED
            try {
                startActivity(Intent(if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri))
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Intent.ACTION_DIAL, uri))
                } catch (e: Exception) {
                    // no dialer on device
                }
            }
        }
        finish()
    }

    companion object {
        const val EXTRA_NUMBER = "number"
    }
}
