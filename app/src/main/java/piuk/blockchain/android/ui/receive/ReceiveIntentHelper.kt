package piuk.blockchain.android.ui.receive

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v4.content.FileProvider
import android.util.Pair
import android.webkit.MimeTypeMap
import com.crashlytics.android.answers.ShareEvent
import info.blockchain.wallet.util.FormatsUtil
import org.bitcoinj.uri.BitcoinURI
import piuk.blockchain.android.R
import piuk.blockchain.android.util.BitcoinLinkGenerator
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

class ReceiveIntentHelper(private val context: Context, private val appUtil: AppUtil) {

    internal fun getIntentDataList(uri: String, bitmap: Bitmap): List<SendPaymentCodeData>? {

        val file = getQrFile()
        val outputStream = getFileOutputStream(file)

        if (outputStream != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream)

            try {
                outputStream.close()
            } catch (e: IOException) {
                Timber.e(e)
                return null
            }

            val dataList = ArrayList<SendPaymentCodeData>()
            val packageManager = appUtil.packageManager
            val mime = MimeTypeMap.getSingleton()
            val ext = file.name.substring(file.name.lastIndexOf(".") + 1)
            val type = mime.getMimeTypeFromExtension(ext)

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply { setupIntentForImage(type, file) }

            when {
                uri.startsWith("bitcoincash") -> emailIntent.apply { setupIntentForEmailBch(uri) }
                uri.startsWith("bitcoin") -> emailIntent.apply { setupIntentForEmailBtc(uri) }
                FormatsUtil.isValidEthereumAddress(uri) -> emailIntent.apply {
                    setupIntentForEmailEth(
                        uri
                    )
                }
                else -> throw IllegalArgumentException("Unknown URI $uri")
            }

            val imageIntent = Intent().apply { setupIntentForImage(type, file) }

            val intentHashMap = HashMap<String, Pair<ResolveInfo, Intent>>()

            val emailResolveInfo = packageManager.queryIntentActivities(emailIntent, 0)
            addResolveInfoToMap(emailIntent, intentHashMap, emailResolveInfo)

            val imageResolveInfo = packageManager.queryIntentActivities(imageIntent, 0)
            addResolveInfoToMap(imageIntent, intentHashMap, imageResolveInfo)

            val it = intentHashMap.entries.iterator()
            while (it.hasNext()) {
                val pair = it.next().value
                val resolveInfo = pair.first
                val context = resolveInfo.activityInfo.packageName
                val packageClassName = resolveInfo.activityInfo.name
                val label = resolveInfo.loadLabel(packageManager)
                val icon = resolveInfo.loadIcon(packageManager)

                val intent = pair.second
                intent.setClassName(context, packageClassName)

                dataList.add(SendPaymentCodeData(label.toString(), icon, intent))

                it.remove()
            }

            Logging.logShare(
                ShareEvent()
                    .putContentName("QR Code + URI")
            )

            return dataList
        } else {
            return null
        }
    }

    @SuppressLint("SetWorldReadable")
    private fun getQrFile(): File {
        val strFileName = appUtil.receiveQRFilename
        val file = File(strFileName)
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        file.setReadable(true, false)
        return file
    }

    private fun getFileOutputStream(file: File): FileOutputStream? {
        return try {
            FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            Timber.e(e)
            null
        }
    }

    /**
     * Prevents apps being added to the list twice, as it's confusing for users. Full email Intent
     * takes priority.
     */
    private fun addResolveInfoToMap(
        intent: Intent,
        intentHashMap: HashMap<String, Pair<ResolveInfo, Intent>>,
        resolveInfo: List<ResolveInfo>
    ) {
        resolveInfo
            .filterNot { intentHashMap.containsKey(it.activityInfo.name) }
            .forEach { intentHashMap[it.activityInfo.name] = Pair(it, Intent(intent)) }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Intent Extension functions
    // /////////////////////////////////////////////////////////////////////////
    private fun Intent.setupIntentForEmailBtc(uri: String) {
        val addressUri = BitcoinURI(uri)
        val amount = if (addressUri.amount != null) " " + addressUri.amount.toPlainString() else ""
        val address =
            if (addressUri.address != null) {
                addressUri.address!!.toString()
            } else {
                context.getString(R.string.email_request_body_fallback)
            }
        val body = String.format(context.getString(R.string.email_request_body), amount, address)

        putExtra(Intent.EXTRA_TEXT, "$body\n\n ${BitcoinLinkGenerator.getLink(addressUri)}")
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_request_subject))
    }

    private fun Intent.setupIntentForEmailEth(uri: String) {
        val address = uri.removePrefix("ethereum:")
        val body = String.format(context.getString(R.string.email_request_body_eth), address)

        putExtra(Intent.EXTRA_TEXT, body)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_request_subject_eth))
    }

    private fun Intent.setupIntentForEmailBch(uri: String) {
        val address = uri.removePrefix("bitcoincash:")
        val body = String.format(context.getString(R.string.email_request_body_bch), address)

        putExtra(Intent.EXTRA_TEXT, body)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_request_subject_bch))
    }

    private fun Intent.setupIntentForImage(type: String?, file: File) {
        action = Intent.ACTION_SEND
        this.type = type

        if (AndroidUtils.is23orHigher()) {
            val uriForFile =
                FileProvider.getUriForFile(context, "${context.packageName}.fileProvider", file)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, uriForFile)
        } else {
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }
}

internal class SendPaymentCodeData(val title: String, val logo: Drawable, val intent: Intent)
