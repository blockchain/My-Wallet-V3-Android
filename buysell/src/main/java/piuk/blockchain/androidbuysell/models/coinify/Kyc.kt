package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson

/**
 * Wraps a [returnUrl] which is the URL to be triggered when the KYC process is
 * complete.
 */
data class KycResponse(
    // Identifier for the KYC review
    val id: Int,
    // KYC review state	State of the KYC review
    val state: ReviewState,
    // URL to return to when kyc flow is completed (this is only necessary in redirect mode).
    val returnUrl: String,
    // URL to redirect the user to in order to perform the KYC review
    val redirectUrl: String,
    // Reference to the external KYC review, this ID can be used in embedded mode.
    val externalId: String,
    // The time when the bank account was last updated, in ISO 8601.
    val updateTime: String,
    // Timestamp for when this bank account was first created, in ISO 8601.
    val createTime: String
)

sealed class ReviewState {

    // (Starting state) KYC review has been initiated, but is waiting for action from the end-user.
    object Pending : ReviewState()

    // (Final state) KYC review was rejected
    object Rejected : ReviewState()

    // (Final state) KYC review failed due to an error with the provider
    object Failed : ReviewState()

    // (Final state) KYC review expired before it was completed
    object Expired : ReviewState()

    // (Final state) KYC review completed successfully. (This state is also used if the KYC review was manual and accepted)
    object Completed : ReviewState()

    // (Intermediate state) KYC is awaiting manual review
    object Reviewing : ReviewState()

    // (Intermediate state) Trader needs to upload more documents
    object DocumentsRequested : ReviewState()

    fun isEndState(): Boolean =
        (this === Rejected || this === Failed || this === Expired || this === Completed)

    fun isProcessing(): Boolean = this === Reviewing
}

@Suppress("unused")
class ReviewStateAdapter {

    @FromJson
    fun fromJson(input: String): ReviewState = when (input) {
        PENDING -> ReviewState.Pending
        REJECTED -> ReviewState.Rejected
        FAILED -> ReviewState.Failed
        EXPIRED -> ReviewState.Expired
        COMPLETED -> ReviewState.Completed
        REVIEWING -> ReviewState.Reviewing
        DOCUMENTS_REQUESTED -> ReviewState.DocumentsRequested
        else -> throw JsonDataException("Unknown review state $input, unsupported data type")
    }

    @ToJson
    fun toJson(reviewState: ReviewState): String = when (reviewState) {
        ReviewState.Pending -> PENDING
        ReviewState.Rejected -> REJECTED
        ReviewState.Failed -> FAILED
        ReviewState.Expired -> EXPIRED
        ReviewState.Completed -> COMPLETED
        ReviewState.Reviewing -> REVIEWING
        ReviewState.DocumentsRequested -> DOCUMENTS_REQUESTED
    }

    private companion object {
        private const val PENDING = "pending"
        private const val REJECTED = "rejected"
        private const val FAILED = "failed"
        private const val EXPIRED = "expired"
        private const val COMPLETED = "completed"
        private const val REVIEWING = "reviewing"
        private const val DOCUMENTS_REQUESTED = "documentsRequested"
    }
}