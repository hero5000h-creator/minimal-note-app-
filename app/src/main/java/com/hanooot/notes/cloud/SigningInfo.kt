package com.hanooot.notes.cloud

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * The package name and certificate fingerprint of the running build.
 *
 * Google Sign-In fails with DEVELOPER_ERROR when these two do not match an
 * Android OAuth client in the Cloud console, and the failure says nothing
 * about which half is wrong. Reading them from the installed APK makes the
 * comparison something the user can do on the spot, instead of trusting that
 * the value someone wrote down is the value that shipped.
 */
object SigningInfo {

    /** SHA-1 of the signing certificate, colon-separated upper-case hex. */
    fun sha1(context: Context): String = runCatching {
        val pm = context.packageManager
        val name = context.packageName

        // Modern APKs are signed with scheme v2/v3 only, which the older
        // GET_SIGNATURES flag still reports, but the newer API is correct
        // about rotated keys.
        val certificates: Array<out android.content.pm.Signature> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(
                    name, PackageManager.GET_SIGNING_CERTIFICATES
                )
                info.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(name, PackageManager.GET_SIGNATURES).signatures
                    ?: emptyArray()
            }

        val der = certificates.firstOrNull()?.toByteArray()
            ?: return "unavailable"

        MessageDigest.getInstance("SHA-1").digest(der)
            .joinToString(":") { "%02X".format(it) }
    }.getOrDefault("unavailable")

    fun packageName(context: Context): String = context.packageName
}
