package github.daisukikaffuchino.rebootnya.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.Spanned
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import github.daisukikaffuchino.rebootnya.R
import java.util.Locale

fun getAppVer(context: Context): String {
    val manager = context.packageManager
    try {
        val info = manager.getPackageInfo(context.packageName, 0)
        val versionName = info.versionName
        val versionCode = info.longVersionCode
        return "$versionName ($versionCode)"
    } catch (e: PackageManager.NameNotFoundException) {
        e.fillInStackTrace()
    }
    return "err"
}

fun CharSequence.toHtml(flags: Int = 0): Spanned {
    return HtmlCompat.fromHtml(this.toString(), flags)
}

fun openUrlLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, R.string.no_app_found_open_link, Toast.LENGTH_SHORT).show()
    }
}

fun sendEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf("konohatamira@outlook.com"))
        putExtra(Intent.EXTRA_SUBJECT, "RebootNya Feedback")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, R.string.no_app_found_open_link, Toast.LENGTH_SHORT).show()
    }
}

fun isSamsung(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
    val brand = Build.BRAND.lowercase(Locale.ROOT)
    return manufacturer.contains("samsung") || brand.contains("samsung")
}
