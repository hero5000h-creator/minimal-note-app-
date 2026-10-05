package com.hanooot.notes.cloud

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.hanooot.notes.data.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Signing in with a Google account and keeping a copy of everything in Drive.
 *
 * The account is Play Services' to remember, not this app's: nothing about the
 * user is stored here beyond the time of the last successful backup.
 */
class BackupManager(private val context: Context, private val repo: NotesRepository) {

    /** What a backup or restore produced, for the message shown afterwards. */
    sealed class Result {
        data class Ok(val notes: Int, val memos: Int) : Result()
        object NoBackup : Result()
        /** Consent was revoked or never given; the UI sends them back to sign-in. */
        data class NeedsSignIn(val intent: Intent?) : Result()
        data class Failed(val reason: String) : Result()
    }

    companion object {
        /** Asks for the account's address and permission to use its own Drive files. */
        fun signInOptions(): GoogleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
                .build()

        fun client(context: Context): GoogleSignInClient =
            GoogleSignIn.getClient(context, signInOptions())

        /** The signed-in account, or null. Play Services survives reinstalls. */
        fun account(context: Context): GoogleSignInAccount? =
            GoogleSignIn.getLastSignedInAccount(context)
                ?.takeIf { GoogleSignIn.hasPermissions(it, *signInOptions().scopeArray) }
    }

    // ---- Backup ----

    suspend fun backUp(): Result {
        val signedIn = account(context)?.account
            ?: return Result.NeedsSignIn(null)

        return try {
            val notes = repo.allNotes()
            val categories = repo.allCategories()

            DriveClient.upload(
                context, signedIn,
                BackupPayload.FILE_NAME, BackupPayload.MIME,
                BackupPayload.encode(notes, categories)
            )

            // Recordings never change once made, so one that is already up
            // there is skipped. That keeps a routine backup to a single
            // upload even when there are hours of audio.
            var uploaded = 0
            notes.flatMap { it.memos }.forEach { memo ->
                val file = File(memo.path)
                if (file.exists()) {
                    val isNew = DriveClient.uploadFileIfNew(
                        context, signedIn, file, "audio/mp4"
                    )
                    if (isNew) uploaded++
                }
            }
            Result.Ok(notes.size, uploaded)
        } catch (e: DriveClient.DriveException) {
            if (e.needsConsent) Result.NeedsSignIn(null)
            else Result.Failed(e.message.orEmpty())
        } catch (e: Exception) {
            Result.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    // ---- Restore ----

    /**
     * Replaces what is on the phone with what is in Drive.
     *
     * Replacing rather than merging is deliberate: ids would collide, and a
     * half-merged set of notes is harder to reason about than a clean copy of
     * a known-good backup. The caller confirms first.
     */
    suspend fun restore(): Result {
        val signedIn = account(context)?.account
            ?: return Result.NeedsSignIn(null)

        return try {
            val bytes = DriveClient.download(context, signedIn, BackupPayload.FILE_NAME)
                ?: return Result.NoBackup

            val memoDir = repo.memoDir()
            val decoded = BackupPayload.decode(bytes, memoDir.absolutePath)

            // Audio first: a note that points at a missing file would show an
            // empty player, so the files are in place before the notes are.
            var restoredAudio = 0
            withContext(Dispatchers.IO) {
                decoded.memoFiles.forEach { name ->
                    val target = File(memoDir, name)
                    if (target.exists()) return@forEach
                    val audio = runCatching {
                        DriveClient.download(context, signedIn, name)
                    }.getOrNull() ?: return@forEach
                    target.writeBytes(audio)
                    restoredAudio++
                }
            }

            repo.replaceAll(decoded.notes, decoded.categories)
            Result.Ok(decoded.notes.size, restoredAudio)
        } catch (e: DriveClient.DriveException) {
            if (e.needsConsent) Result.NeedsSignIn(null)
            else Result.Failed(e.message.orEmpty())
        } catch (e: Exception) {
            Result.Failed(e.message ?: e.javaClass.simpleName)
        }
    }
}
