package piuk.blockchain.kycdemo

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.blockchain.kyc.datamanagers.OnfidoDataManager
import com.blockchain.kyc.services.OnfidoService
import com.onfido.android.sdk.capture.ExitCode
import com.onfido.android.sdk.capture.Onfido
import com.onfido.android.sdk.capture.OnfidoConfig
import com.onfido.android.sdk.capture.OnfidoFactory
import com.onfido.android.sdk.capture.errors.OnfidoException
import com.onfido.android.sdk.capture.ui.camera.face.FaceCaptureStep
import com.onfido.android.sdk.capture.ui.camera.face.FaceCaptureVariant
import com.onfido.android.sdk.capture.ui.options.FlowStep
import com.onfido.android.sdk.capture.upload.Captures
import com.onfido.api.client.data.Applicant
import com.squareup.moshi.Moshi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import piuk.blockchain.kyc.BuildConfig
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import kotlinx.android.synthetic.main.activity_main.edit_text_first_name as editTextFirstName
import kotlinx.android.synthetic.main.activity_main.edit_text_last_name as editTextLastName

class MainActivity : AppCompatActivity() {

    private val onfido by lazy(LazyThreadSafetyMode.NONE) { OnfidoFactory.create(this).client }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun launchKycFlow(view: View) {
        // These will be injected in app
        val moshi: Moshi = Moshi.Builder().build()
        val moshiConverterFactory = MoshiConverterFactory.create(moshi)
        val rxJava2CallAdapterFactory = RxJava2CallAdapterFactory.create()
        val okHttpClient = OkHttpClient.Builder()
            .build()
        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("https://api.onfido.com/")
            .addConverterFactory(moshiConverterFactory)
            .addCallAdapterFactory(rxJava2CallAdapterFactory)
            .build()

        OnfidoDataManager(OnfidoService(retrofit))
            .createApplicant(
                editTextFirstName.text.toString(),
                editTextLastName.text.toString(),
                BuildConfig.ONFIDO_SANDBOX_KEY
            )
            .map { it.id }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { startOnfidoFlow(it) },
                onError = {
                    Timber.e(it)
                    Toast.makeText(this, "KYC applicant creation failed", LENGTH_LONG).show()
                }
            )
    }

    private fun startOnfidoFlow(applicantId: String) {
        // We only require document capture and video face capture
        val kycFlowSteps = arrayOf(
            FlowStep.CAPTURE_DOCUMENT,
            FaceCaptureStep(FaceCaptureVariant.VIDEO)
        )

        OnfidoConfig.builder()
            .withToken(BuildConfig.ONFIDO_SANDBOX_KEY)
            .withApplicant(applicantId)
            .withCustomFlow(kycFlowSteps)
            .build()
            .also { onfido.startActivityForResult(this, REQUEST_CODE_ONFIDO, it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ONFIDO) {
            onfido.handleActivityResult(resultCode, data, object : Onfido.OnfidoResultListener {
                override fun userCompleted(applicant: Applicant, captures: Captures) {
                    // Send result to backend, continue to exchange
                    Toast.makeText(this@MainActivity, "KYC process complete", LENGTH_LONG).show()
                }

                override fun userExited(exitCode: ExitCode, applicant: Applicant) {
                    // User left the sdk flow without completing it
                    Toast.makeText(this@MainActivity, "User exited KYC process", LENGTH_LONG).show()
                }

                override fun onError(exception: OnfidoException, applicant: Applicant?) {
                    // An exception occurred during the flow
                    Timber.e(exception)
                    Toast.makeText(this@MainActivity, "Error in KYC process", LENGTH_LONG).show()
                }
            })
        }
    }

    companion object {

        const val REQUEST_CODE_ONFIDO = 1337
    }
}
