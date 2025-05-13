package ir.mehdisekoba.feedback

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import ir.mehdisekoba.feedback.components.DeviceInfoUtil
import ir.mehdisekoba.feedback.components.Utils
import ir.mehdisekoba.feedback.databinding.FeedbackLayoutBinding
import ir.mehdisekoba.feedback.utils.KEY_EMAIL
import ir.mehdisekoba.feedback.utils.KEY_WITH_INFO
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader


/**
 * Activity for submitting feedback, including suggestions, bugs, or other issues.
 *
 * This activity allows users to:
 * - Provide a title and description for their feedback.
 * - Categorize their feedback (Suggestion, Bug, Other) using a tab layout.
 * - Optionally include system information and application logs.
 * - Attach up to 3 images.
 * - Send the feedback via email.
 *
 * It handles necessary permissions for accessing device information and logs,
 * and provides UI for image attachment and removal.
 */
class FeedbackActivity : AppCompatActivity() {
    private lateinit var binding: FeedbackLayoutBinding
    private var emailId: String? = null
    private var withInfo: Boolean = false
    private var deviceInfo: String? = null
    private var logToString: String = ""
    private var feedbackType: String = ""

    // Image handling
    private val imageUris = mutableListOf<Uri?>(null, null, null) // Store up to 3 image URIs
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleImageSelection(it) }
        }
    private val phoneStatePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                loadDeviceInfo()
            } else {
                if (!shouldShowRequestPermissionRationale(android.Manifest.permission.READ_PHONE_STATE)) {
                    showPermissionDialog(getString(R.string.phone_state_permission_message)) { goToSettings() }
                } else {
                    Toast.makeText(this, R.string.phone_state_permission_denied, Toast.LENGTH_SHORT)
                        .show()
                    deviceInfo = "Device info unavailable: Permission denied"
                    updateLegalText()
                }
            }
        }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (hasPhoneStatePermission()) {
                loadDeviceInfo()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FeedbackLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setupImageAttachment()
    }

    private fun init() {
        binding.apply {
            submitSuggestion.setOnClickListener { submitFeedback() }
            emailId = intent.getStringExtra(KEY_EMAIL)
            withInfo = intent.getBooleanExtra(KEY_WITH_INFO, false)
            setupTabLayout()
            Utils.log(
                this@FeedbackActivity,
                "FeedbackActivity initialized at ${System.currentTimeMillis()}"
            )
            if (hasPhoneStatePermission()) {
                loadDeviceInfo()
            } else {
                phoneStatePermissionLauncher.launch(android.Manifest.permission.READ_PHONE_STATE)
            }

            submitSuggestion.isEnabled = false

            updateSubmitButtonState()
            edtTitle.onTextChange { updateSubmitButtonState() }
            edtDesc.onTextChange { updateSubmitButtonState() }
        }
    }
    private fun setupTabLayout() {
        binding.apply {
            with(feedbackTabs) {
                addTab(newTab().setText(R.string.suggestion))
                addTab(newTab().setText(R.string.bug))
                addTab(newTab().setText(R.string.other))
                getTabAt(0)?.select()
            }
            feedbackType = getString(R.string.suggestion)
            feedbackTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    feedbackType = tab?.text.toString()
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
    }

    private fun setupImageAttachment() {
        binding.apply {
            addImageButton.setOnClickListener {
                val imageCount = imageUris.count { it != null }
                if (imageCount < 3) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    Toast.makeText(
                        this@FeedbackActivity,
                        "You can only attach up to 3 images.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }

            // Remove image listeners
            removeImage1.setOnClickListener { removeImage(0) }
            removeImage2.setOnClickListener { removeImage(1) }
            removeImage3.setOnClickListener { removeImage(2) }
        }
    }

    private fun handleImageSelection(uri: Uri) {
        val index = imageUris.indexOfFirst { it == null }
        if (index == -1) return

        imageUris[index] = uri
        updateImageViews()
    }

    private fun removeImage(index: Int) {
        imageUris[index] = null
        updateImageViews()
    }

    private fun updateImageViews() {
        binding.apply {
            // Image 1
            if (imageUris[0] != null) {
                imageContainer1.visibility = View.VISIBLE
                attachedImage1.setImageURI(imageUris[0])
            } else {
                imageContainer1.visibility = View.GONE
            }

            // Image 2
            if (imageUris[1] != null) {
                imageContainer2.visibility = View.VISIBLE
                attachedImage2.setImageURI(imageUris[1])
            } else {
                imageContainer2.visibility = View.GONE
            }

            // Image 3
            if (imageUris[2] != null) {
                imageContainer3.visibility = View.VISIBLE
                attachedImage3.setImageURI(imageUris[2])
            } else {
                imageContainer3.visibility = View.GONE
            }

            // Show/hide add button based on image count
            addImageButton.visibility =
                if (imageUris.count { it != null } < 3) View.VISIBLE else View.GONE
        }
    }

    private fun loadDeviceInfo() {
        deviceInfo = try {
            DeviceInfoUtil(this).getAllDeviceInfo(false)
        } catch (e: Exception) {
            "Error retrieving device info: ${e.message}"
        }
        logToString = readLogsFromFile()
        updateLegalText()
    }

    private fun readLogsFromFile(): String {
        val logFile = File(filesDir, "logs.txt")
        return try {
            if (!logFile.exists()) {
                logFile.createNewFile()
                "No logs available"
            } else {
                BufferedReader(InputStreamReader(openFileInput("logs.txt"))).use { reader ->
                    reader.readText().ifEmpty { "No logs available" }
                }
            }
        } catch (e: IOException) {
            "Error retrieving logs: ${e.message}"
        }
    }

    private fun updateLegalText() {
        if (!withInfo) {
            binding.infoLegal.visibility = View.GONE
            return
        }
        val finalLegal = SpannableStringBuilder().apply {
            append(getString(R.string.info_fedback_legal_start))
            val deviceInfoText = getString(R.string.info_fedback_legal_system_info)
            val deviceInfoSpannable = SpannableString(deviceInfoText).apply {
                setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            showInfoDialog(R.string.info_fedback_legal_system_info, deviceInfo)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.isUnderlineText = true
                            ds.color = ContextCompat.getColor(
                                this@FeedbackActivity,
                                android.R.color.holo_blue_light
                            )
                        }
                    }, 0, deviceInfoText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            append(deviceInfoSpannable)
            append(getString(R.string.info_fedback_legal_and))
            val systemLogText = getString(R.string.info_fedback_legal_log_data)
            val systemLogSpannable = SpannableString(systemLogText).apply {
                setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            showInfoDialog(R.string.info_fedback_legal_log_data, logToString)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.isUnderlineText = true
                            ds.color = ContextCompat.getColor(
                                this@FeedbackActivity,
                                android.R.color.holo_blue_light
                            )
                        }
                    }, 0, systemLogText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            append(systemLogSpannable)
            append(
                getString(
                    R.string.info_fedback_legal_will_be_sent,
                    getString(R.string.app_name)
                )
            )
        }
        with(binding.infoLegal) {
            text = finalLegal
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }
    }

    private fun showInfoDialog(titleResId: Int, message: String?) {
        MaterialAlertDialogBuilder(this)
            .setTitle(titleResId)
            .setMessage(message)
            .setPositiveButton(R.string.Ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun hasPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionDialog(message: String, okListener: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton(R.string.Ok) { _, _ -> okListener() }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun goToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        settingsLauncher.launch(intent)
    }

    private fun submitFeedback() {
        binding.apply {
            val title = edtTitle.text.toString().trim()
            val description = edtDesc.text.toString().trim()
            when {
                title.isEmpty() -> edtTitle.error = getString(R.string.please_write)
                description.isEmpty() -> edtDesc.error =
                    getString(R.string.please_write_description)

                else -> {
                    sendEmail(title, description)
                    finish()
                }
            }
        }
    }

    private fun sendEmail(title: String, description: String) {
        if (emailId == null) {
            Toast.makeText(this, R.string.email_not_set, Toast.LENGTH_SHORT).show()
            return
        }
        val emailBody = """
            MsFeedback Type: $feedbackType
            Title: $title
            Description: $description
        """.trimIndent()
        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailId))
            putExtra(Intent.EXTRA_SUBJECT, "MsFeedback Type: $feedbackType")
            putExtra(Intent.EXTRA_TEXT, emailBody)
            val uris = mutableListOf<Uri>()
            // Add device info and logs
            if (withInfo) {
                deviceInfo?.let {
                    createFileFromString(
                        it,
                        getString(R.string.file_name_device_info)
                    )?.let { uri ->
                        uris.add(uri)
                    }
                }
                createFileFromString(
                    logToString,
                    getString(R.string.file_name_device_log)
                )?.let { uri ->
                    uris.add(uri)
                }
            }
            // Add images
            imageUris.filterNotNull().forEach { uri ->
                uris.add(uri)
            }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(
                Utils.createEmailOnlyChooserIntent(
                    this,
                    emailIntent,
                    getString(R.string.send_feedback_two)
                )
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_email_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFileFromString(text: String, name: String): Uri? {
        val file = File(externalCacheDir, name)
        return try {
            BufferedWriter(FileWriter(file, false)).use { it.write(text) }
            val uri = FileProvider.getUriForFile(this, "ir.mehdisekoba.feedback.provider", file)
            file.deleteOnExit()
            uri
        } catch (_: IOException) {
            Toast.makeText(this, R.string.file_creation_failed, Toast.LENGTH_SHORT).show()
            null
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "FileProvider configuration error", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun updateSubmitButtonState() {
        binding.apply {
            val title = edtTitle.text.toString().trim().isNotEmpty()
            val description = edtDesc.text.toString().trim().isNotEmpty()
            submitSuggestion.isEnabled = title && description
        }
    }

    private inline fun EditText.onTextChange(crossinline listener: (String) -> Unit) {
        this.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    listener(charSequence.toString())
                }
                override fun afterTextChanged(p0: Editable?) {}
            }
        )
    }
}