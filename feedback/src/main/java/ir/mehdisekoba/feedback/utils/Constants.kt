package ir.mehdisekoba.feedback.utils

const val PUBLIC_DOWNLOADS_URI = "content://downloads/public_downloads"
const val AUTHORITY_EXTERNAL_STORAGE = "com.android.externalstorage.documents"
const val AUTHORITY_DOWNLOADS = "com.android.providers.downloads.documents"
const val AUTHORITY_MEDIA = "com.android.providers.media.documents"

const val SCHEME_CONTENT = "content"
const val SCHEME_FILE = "file"

const val TYPE_PRIMARY = "primary"
const val SEPARATOR_COLON = ":"

const val TYPE_IMAGE = "image"
const val TYPE_VIDEO = "video"
const val TYPE_AUDIO = "audio"

const val QUERY_ID = "_id=?"

const val ERROR_INVALID_FILE_PATH = "Invalid file path:"
const val ERROR_FAILED_TO_LOAD_IMAGE = "Failed to load image:"
const val ERROR_UNABLE_TO_RESOLVE_IMAGE_PATH = "Unable to resolve image path"
const val KEY_WITH_INFO = "with_info"
const val KEY_EMAIL = "email"
const val REQUEST_APP_SETTINGS = 321