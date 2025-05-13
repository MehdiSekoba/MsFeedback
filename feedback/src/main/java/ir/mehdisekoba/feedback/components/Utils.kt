package ir.mehdisekoba.feedback.components

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import ir.mehdisekoba.feedback.utils.AUTHORITY_DOWNLOADS
import ir.mehdisekoba.feedback.utils.AUTHORITY_EXTERNAL_STORAGE
import ir.mehdisekoba.feedback.utils.AUTHORITY_MEDIA
import ir.mehdisekoba.feedback.utils.ERROR_FAILED_TO_LOAD_IMAGE
import ir.mehdisekoba.feedback.utils.ERROR_INVALID_FILE_PATH
import ir.mehdisekoba.feedback.utils.ERROR_UNABLE_TO_RESOLVE_IMAGE_PATH
import ir.mehdisekoba.feedback.utils.PUBLIC_DOWNLOADS_URI
import ir.mehdisekoba.feedback.utils.QUERY_ID
import ir.mehdisekoba.feedback.utils.SCHEME_CONTENT
import ir.mehdisekoba.feedback.utils.SCHEME_FILE
import ir.mehdisekoba.feedback.utils.SEPARATOR_COLON
import ir.mehdisekoba.feedback.utils.TYPE_AUDIO
import ir.mehdisekoba.feedback.utils.TYPE_IMAGE
import ir.mehdisekoba.feedback.utils.TYPE_PRIMARY
import ir.mehdisekoba.feedback.utils.TYPE_VIDEO
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Utility object for common operations like file handling, image processing, and intent creation.
 */

/**
 * Utility object for common operations like file handling, image processing, and intent creation.
 */
/**
 * Utility object for common operations like file handling, image processing, and intent creation.
 */
object Utils {

    /**
     * Retrieves the file path from a given URI.
     * Note: For Android 10+ with Scoped Storage, prefer using ContentResolver directly or SAF.
     * @param context The application context.
     * @param uri The URI to resolve.
     * @return The file path, or null if the path cannot be resolved.
     */
    fun getPath(context: Context, uri: Uri): String? = when {
        DocumentsContract.isDocumentUri(context, uri) -> {
            when {
                hasAuthority(uri, AUTHORITY_EXTERNAL_STORAGE) -> handleExternalStorage(uri)
                hasAuthority(uri, AUTHORITY_DOWNLOADS) -> handleDownloads(context, uri)
                hasAuthority(uri, AUTHORITY_MEDIA) -> handleMedia(context, uri)
                else -> null
            }
        }
        uri.scheme.equals(SCHEME_CONTENT, ignoreCase = true) -> queryDataColumn(context, uri, null, null)
        uri.scheme.equals(SCHEME_FILE, ignoreCase = true) -> uri.path
        else -> null
    }

    /**
     * Handles external storage document URIs.
     * @param uri The document URI.
     * @return The file path, or null if not resolved.
     */
    private fun handleExternalStorage(uri: Uri): String? {
        return try {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(SEPARATOR_COLON)
            if (split[0].equals(TYPE_PRIMARY, ignoreCase = true)) {
                "${Environment.getExternalStorageDirectory()}/${split[1]}"
            } else {
                null // TODO: Handle non-primary volumes
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Handles download document URIs.
     * @param context The application context.
     * @param uri The document URI.
     * @return The file path, or null if not resolved.
     */
    private fun handleDownloads(context: Context, uri: Uri): String? {
        return try {
            val id = DocumentsContract.getDocumentId(uri)
            val contentUri = ContentUris.withAppendedId(
                PUBLIC_DOWNLOADS_URI.toUri(),
                id.toLongOrNull() ?: return null
            )
            queryDataColumn(context, contentUri, null, null)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Handles media document URIs.
     * @param context The application context.
     * @param uri The document URI.
     * @return The file path, or null if not resolved.
     */
    private fun handleMedia(context: Context, uri: Uri): String? {
        return try {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            val contentUri = when (split[0]) {
                TYPE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> null
            }
            queryDataColumn(context, contentUri, QUERY_ID, arrayOf(split[1]))
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Queries the data column from a content URI.
     * @param context The application context.
     * @param uri The content URI to query.
     * @param selection The selection clause.
     * @param selectionArgs The selection arguments.
     * @return The data column value, or null if not found.
     */
    private fun queryDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        uri ?: return null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Checks if the URI has the specified authority.
     * @param uri The URI to check.
     * @param authority The authority to compare.
     * @return True if the URI has the specified authority, false otherwise.
     */
    private fun hasAuthority(uri: Uri, authority: String): Boolean = authority == uri.authority

    /**
     * Decodes a bitmap from a file with sampling to reduce memory usage.
     * @param path The file path of the image.
     * @param reqWidth The required width.
     * @param reqHeight The required height.
     * @return The sampled bitmap.
     * @throws IllegalArgumentException If the file path is invalid.
     */
    fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options) ?: throw IllegalArgumentException("${ERROR_INVALID_FILE_PATH}:${path}")
    }

    /**
     * Calculates the sample size for bitmap decoding.
     * @param options The bitmap options containing dimensions.
     * @param reqWidth The required width.
     * @param reqHeight The required height.
     * @return The sample size.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Compresses a bitmap to a JPEG byte array.
     * @param bitmap The bitmap to compress.
     * @param quality The compression quality (0-100).
     * @return The compressed byte array.
     */
    fun compressBitmap(bitmap: Bitmap, quality: Int = 80): ByteArray =
        ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }

    /**
     * Loads and displays an image from a URI into an ImageView with sampling.
     * @param context The application context.
     * @param uri The image URI.
     * @param imageView The ImageView to display the image.
     * @param reqWidth The required width (default: 600).
     * @param reqHeight The required height (default: 600).
     */
    fun loadImageToImageView(
        context: Context,
        uri: Uri,
        imageView: ImageView,
        reqWidth: Int = 600,
        reqHeight: Int = 600
    ) {
        val path = getPath(context, uri)
        if (path != null) {
            try {
                val bitmap = decodeSampledBitmap(path, reqWidth, reqHeight)
                imageView.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                Toast.makeText(context, ERROR_FAILED_TO_LOAD_IMAGE, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, ERROR_UNABLE_TO_RESOLVE_IMAGE_PATH, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Creates an intent chooser for email apps only.
     * @param context The application context.
     * @param source The source intent.
     * @param chooserTitle The title for the chooser dialog.
     * @return The chooser intent.
     */
    fun createEmailOnlyChooserIntent(context: Context, source: Intent, chooserTitle: CharSequence): Intent {
        val intents = mutableListOf<Intent>()
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "info@domain.com", null))
        val resolveInfos = context.packageManager.queryIntentActivities(emailIntent, 0)

        for (info in resolveInfos) {
            val target = Intent(source)
            target.setPackage(info.activityInfo.packageName)
            intents.add(target)
        }

        return if (intents.isNotEmpty()) {
            val chooser = Intent.createChooser(intents.removeAt(0), chooserTitle)
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            chooser
        } else {
            Intent.createChooser(source, chooserTitle)
        }
    }
    fun log(context: Context, message: String) {
        try {
            val fileOutputStream = context.openFileOutput("logs.txt", Context.MODE_APPEND)
            fileOutputStream.write("$message\n".toByteArray())
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace() // Optionally handle the error differently
        }
    }
}