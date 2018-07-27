package piuk.blockchain.kycdemo

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.blockchain.kyc.datamanagers.OnfidoDataManager
import com.blockchain.kyc.models.CheckResultAdapter
import com.blockchain.kyc.models.CheckStatusAdapter
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
import piuk.blockchain.kycdemo.utils.ApiInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import kotlinx.android.synthetic.main.activity_main.edit_text_first_name as editTextFirstName
import kotlinx.android.synthetic.main.activity_main.edit_text_last_name as editTextLastName

class MainActivity : AppCompatActivity() {

    // These will be injected in app
    private val moshi: Moshi = Moshi.Builder()
        .add(CheckResultAdapter())
        .add(CheckStatusAdapter())
        .build()
    private val moshiConverterFactory = MoshiConverterFactory.create(moshi)
    private val rxJava2CallAdapterFactory = RxJava2CallAdapterFactory.create()
    private val okHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(ApiInterceptor())
        .build()
    private val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl("https://api.onfido.com/")
        .addConverterFactory(moshiConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapterFactory)
        .build()

    private val onfido by lazy(LazyThreadSafetyMode.NONE) { OnfidoFactory.create(this).client }
    private val onfidoDataManager = OnfidoDataManager(OnfidoService(retrofit))
    private val apiKey = BuildConfig.ONFIDO_SANDBOX_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.plant(Timber.DebugTree())
    }

    fun launchKycFlow(view: View) {
        onfidoDataManager
            .createApplicant(
                editTextFirstName.text.toString(),
                editTextLastName.text.toString(),
                apiKey
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
            .withToken(apiKey)
            .withApplicant(applicantId)
            .withCustomFlow(kycFlowSteps)
            .build()
            .also { onfido.startActivityForResult(this, REQUEST_CODE_ONFIDO, it) }
    }

    private fun startOnfidoCheck(applicantId: String) {
        onfidoDataManager.createCheck(applicantId, apiKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    Toast.makeText(this, "KYC check creation success", LENGTH_LONG).show()
                },
                onError = {
                    Timber.e(it)
                    Toast.makeText(this, "KYC check creation failed", LENGTH_LONG).show()
                }
            )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ONFIDO) {
            onfido.handleActivityResult(resultCode, data, object : Onfido.OnfidoResultListener {
                override fun userCompleted(applicant: Applicant, captures: Captures) {
                    // Send result to backend, continue to exchange
                    Toast.makeText(
                        this@MainActivity,
                        "Document capture complete",
                        LENGTH_LONG
                    ).show()
                    startOnfidoCheck(applicant.id)
                }

                override fun userExited(exitCode: ExitCode, applicant: Applicant) {
                    // User left the sdk flow without completing it
                    Toast.makeText(
                        this@MainActivity,
                        "User exited document capture process",
                        LENGTH_LONG
                    ).show()
                }

                override fun onError(exception: OnfidoException, applicant: Applicant?) {
                    // An exception occurred during the flow
                    Timber.e(exception)
                    Toast.makeText(
                        this@MainActivity,
                        "Error in document capture process",
                        LENGTH_LONG
                    ).show()
                }
            })
        }
    }

    companion object {

        const val REQUEST_CODE_ONFIDO = 1337
    }
}
