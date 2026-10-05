package com.hanooot.notes.cloud

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The slice of the Drive v3 REST API this app needs, over plain HTTPS.
 *
 * google-api-client would do the same work, but it pulls in a large and
 * version-sensitive dependency tree for what amounts to four endpoints —
 * list, create folder, upload, download — so the requests are written out.
 *
 * Scope is `drive.file`: the app can only touch files it created itself, and
 * those files are visible in the user's own Drive, so a backup can be opened
 * or downloaded by hand without the app.
 */
object DriveClient {

    const val SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"

    /** The folder every backup lives in, as it appears in Drive. */
    private const val FOLDER_NAME = "Hanooot Notes Backup"
    private const val FOLDER_MIME = "application/vnd.google-apps.folder"

    private const val FILES = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"

    /**
     * Raised when Drive refuses the request; the message is shown to the user.
     *
     * @param recoveryIntent the prompt that fixes it, when Play Services
     *        supplies one. Granting Drive access is a second consent after
     *        sign-in, and dropping this intent leaves the user bouncing back
     *        to a sign-in that already succeeded.
     */
    class DriveException(
        message: String,
        val needsConsent: Boolean = false,
        val recoveryIntent: Intent? = null
    ) : IOException(message)

    /**
     * An OAuth access token for [account].
     *
     * Play Services caches these per account and scope, so this is cheap after
     * the first call. A [UserRecoverableAuthException] means consent was
     * revoked or never granted, which the UI turns back into a sign-in prompt.
     */
    private suspend fun token(context: Context, account: Account): String =
        withContext(Dispatchers.IO) {
            try {
                GoogleAuthUtil.getToken(context, account, SCOPE)
            } catch (e: UserRecoverableAuthException) {
                throw DriveException(
                    e.message ?: "Authorisation needed",
                    needsConsent = true,
                    recoveryIntent = e.intent
                )
            }
        }

    // ---- Requests ----

    private fun HttpURLConnection.readBody(): String =
        (if (responseCode in 200..299) inputStream else errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()

    private fun HttpURLConnection.failIfError(what: String) {
        if (responseCode !in 200..299) {
            val body = readBody()
            // Drive reports the real reason in the body; the status alone
            // ("403") is never enough to act on.
            val detail = runCatching {
                JSONObject(body).getJSONObject("error").getString("message")
            }.getOrDefault(body.take(200))
            throw DriveException("$what failed ($responseCode): $detail")
        }
    }

    private suspend fun get(context: Context, account: Account, url: String): String =
        withContext(Dispatchers.IO) {
            val bearer = token(context, account)
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $bearer")
                connectTimeout = 20_000
                readTimeout = 60_000
            }
            try {
                conn.failIfError("Drive request")
                conn.readBody()
            } finally {
                conn.disconnect()
            }
        }

    // ---- Folder ----

    /** The backup folder's id, creating it the first time. */
    private suspend fun folderId(context: Context, account: Account): String {
        val query = URLEncoder.encode(
            "name = '$FOLDER_NAME' and mimeType = '$FOLDER_MIME' and trashed = false",
            "UTF-8"
        )
        val found = get(context, account, "$FILES?q=$query&fields=files(id)&pageSize=1")
        val files = JSONObject(found).optJSONArray("files")
        if (files != null && files.length() > 0) {
            return files.getJSONObject(0).getString("id")
        }

        return withContext(Dispatchers.IO) {
            val bearer = token(context, account)
            val body = JSONObject()
                .put("name", FOLDER_NAME)
                .put("mimeType", FOLDER_MIME)
                .toString()
            val conn = (URL("$FILES?fields=id").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $bearer")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 20_000
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray()) }
                conn.failIfError("Creating the backup folder")
                JSONObject(conn.readBody()).getString("id")
            } finally {
                conn.disconnect()
            }
        }
    }

    /** The id of [name] inside the backup folder, or null if it isn't there. */
    private suspend fun fileId(
        context: Context,
        account: Account,
        name: String,
        parent: String
    ): String? {
        val query = URLEncoder.encode(
            "name = '$name' and '$parent' in parents and trashed = false",
            "UTF-8"
        )
        val found = get(context, account, "$FILES?q=$query&fields=files(id)&pageSize=1")
        val files = JSONObject(found).optJSONArray("files")
        return if (files != null && files.length() > 0) {
            files.getJSONObject(0).getString("id")
        } else null
    }

    // ---- Upload ----

    /**
     * Writes [bytes] to [name] in the backup folder, replacing what is there.
     *
     * Replacing rather than adding keeps one current backup instead of a pile
     * of dated copies, and keeps the Drive quota flat.
     */
    suspend fun upload(
        context: Context,
        account: Account,
        name: String,
        mime: String,
        bytes: ByteArray
    ): Unit = withContext(Dispatchers.IO) {
        val parent = folderId(context, account)
        val existing = fileId(context, account, name, parent)
        val bearer = token(context, account)

        // Multipart: the metadata and the content go in one request, which
        // keeps an upload to a single round trip.
        val boundary = "hanooot" + System.nanoTime()
        val metadata = JSONObject().put("name", name).apply {
            if (existing == null) put("parents", org.json.JSONArray().put(parent))
        }

        val url = if (existing == null) "$UPLOAD?uploadType=multipart"
                  else "$UPLOAD/$existing?uploadType=multipart"

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            // An update is a PATCH, which HttpURLConnection will not send
            // directly; the override header is how Google's API accepts it.
            if (existing != null) setRequestProperty("X-HTTP-Method-Override", "PATCH")
            doOutput = true
            setRequestProperty("Authorization", "Bearer $bearer")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            connectTimeout = 20_000
            readTimeout = 120_000
        }
        try {
            conn.outputStream.buffered().use { out ->
                out.write(
                    ("--$boundary\r\n" +
                            "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                            "$metadata\r\n" +
                            "--$boundary\r\n" +
                            "Content-Type: $mime\r\n\r\n").toByteArray()
                )
                out.write(bytes)
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }
            conn.failIfError("Upload")
        } finally {
            conn.disconnect()
        }
    }

    /** Uploads a local file, skipping it when a file of that name already exists. */
    suspend fun uploadFileIfNew(
        context: Context,
        account: Account,
        file: File,
        mime: String
    ): Boolean {
        val parent = folderId(context, account)
        if (fileId(context, account, file.name, parent) != null) return false
        upload(context, account, file.name, mime, file.readBytes())
        return true
    }

    // ---- Download ----

    /** The contents of [name], or null when there is no such file. */
    suspend fun download(context: Context, account: Account, name: String): ByteArray? {
        val parent = folderId(context, account)
        val id = fileId(context, account, name, parent) ?: return null
        return withContext(Dispatchers.IO) {
            val bearer = token(context, account)
            val conn = (URL("$FILES/$id?alt=media").openConnection() as HttpURLConnection)
                .apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $bearer")
                    connectTimeout = 20_000
                    readTimeout = 120_000
                }
            try {
                conn.failIfError("Download")
                conn.inputStream.use { it.readBytes() }
            } finally {
                conn.disconnect()
            }
        }
    }
}
