package ir.mehdisekoba.feedback

import android.content.Context
import android.content.Intent

class MsFeedback private constructor(
    private val context: Context,
    private val emailId: String?,
    private val withSystemInfo: Boolean
) {

    class Builder(private val context: Context) {
        private var emailId: String? = null
        private var withSystemInfo: Boolean = false

        fun withEmail(email: String) = apply { this.emailId = email }

        fun withSystemInfo() = apply { this.withSystemInfo = true }

        fun build() = MsFeedback(context, emailId, withSystemInfo)
    }

    fun start() {
        Intent(context, FeedbackActivity::class.java).apply {
            putExtra("email", emailId)
            putExtra("with_info", withSystemInfo)
            context.startActivity(this)
        }
    }
}