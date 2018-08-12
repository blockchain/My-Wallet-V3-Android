package com.blockchain.kyc.models.onfido

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson

data class ApplicantRequest(
    @field:Json(name = "first_name") val firstName: String,
    @field:Json(name = "last_name") val lastName: String
)

data class ApplicantResponse(
    val id: String,
    @field:Json(name = "created_at") val createdAt: String,
    val sandbox: Boolean,
    @field:Json(name = "first_name") val firstName: String,
    @field:Json(name = "last_name") val lastName: String,
    val country: String
)

enum class CheckStatus(val value: String) {
    /**
     * Applicant has not yet submitted the Applicant Form, either because they have not started filling
     * the form out or because they have started but have not finished.
     */
    AwaitingApplicant("awaiting_applicant"),
    /**
     *  Onfido has made a request to one of its data providers and we are waiting on their reply.
     */
    AwaitingData("awaiting_data"),
    /**
     * Report is going through manual review.
     */
    AwaitingApproval("awaiting_approval"),
    /**
     * Report is done.
     */
    Complete("complete"),
    /**
     * Report has been cancelled.
     */
    Withdrawn("withdrawn"),
    /**
     * Report is paused until you, i.e. the client, switch it on manually. Special case used by clients
     * who wants to collect data and run the reports when they want and not immediately.
     */
    Paused("paused"),
    /**
     * Special status for conditional checks: When you, i.e. the client, request two reports and one of
     * them is conditional on the other, then the status will be complete if the condition is met or cancelled if not.
     */
    Cancelled("cancelled"),
    /**
     * Insufficient/inconsistent information is provided by the applicant, and the report has been bounced back
     * for further information.
     */
    Reopened("reopened"),

    /**
     * Sometimes, reports take time to process, and their results do not return instantly. In this case,
     * the check will return with an in_progress status.
     */
    InProgress("in_progress");

    override fun toString(): String = value
}

class CheckStatusAdapter {
    @FromJson
    fun fromJson(data: String): CheckStatus = when (data) {
        "awaiting_applicant" -> CheckStatus.AwaitingApplicant
        "awaiting_data" -> CheckStatus.AwaitingData
        "awaiting_approval" -> CheckStatus.AwaitingApproval
        "complete" -> CheckStatus.Complete
        "withdrawn" -> CheckStatus.Withdrawn
        "paused" -> CheckStatus.Paused
        "cancelled" -> CheckStatus.Cancelled
        "reopened" -> CheckStatus.Reopened
        "in_progress" -> CheckStatus.InProgress
        else -> throw JsonDataException("Unknown CheckStatus $data, unsupported data type")
    }

    @ToJson
    fun toJson(status: CheckStatus) = status.toString()
}

enum class CheckResult(val value: String) {
    /**
     * If all underlying verifications pass, the overall result will be clear.
     */
    Clear("clear"),
    /**
     * If the report has returned information that needs to be evaluated, the overall result will be consider.
     */
    Consider("consider"),
    /**
     * Identity report (standard variant) only - this is returned if the applicant fails an identity check.
     * This indicates there is no identity match for this applicant on any of the databases searched.
     */
    Unidentified("unidentified");

    override fun toString(): String = value
}

class CheckResultAdapter {
    @FromJson
    fun fromJson(data: String): CheckResult = when (data) {
        "clear" -> CheckResult.Clear
        "consider" -> CheckResult.Consider
        "unidentified" -> CheckResult.Unidentified
        else -> throw JsonDataException("Unknown CheckResult $data, unsupported data type")
    }

    @ToJson
    fun toJson(result: CheckResult) = result.toString()
}