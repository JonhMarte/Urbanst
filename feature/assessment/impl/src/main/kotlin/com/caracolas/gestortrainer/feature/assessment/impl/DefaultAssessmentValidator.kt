package com.caracolas.gestortrainer.feature.assessment.impl

import com.caracolas.gestortrainer.feature.assessment.api.AssessmentError
import com.caracolas.gestortrainer.feature.assessment.api.AssessmentValidator
import com.caracolas.gestortrainer.feature.assessment.api.ClientAssessment
import javax.inject.Inject

class DefaultAssessmentValidator @Inject constructor() : AssessmentValidator {
    override fun validate(assessment: ClientAssessment): List<AssessmentError> {
        val errors = mutableListOf<AssessmentError>()

        if (assessment.activityFrequency !in 0..10) {
            errors += AssessmentError.ACTIVITY_FREQUENCY_OUT_OF_RANGE
        }
        assessment.daysPerWeek?.let { if (it !in 1..6) errors += AssessmentError.DAYS_PER_WEEK_OUT_OF_RANGE }
        if (assessment.psychomotorIssue) {
            val details = assessment.psychomotorDetails
            if (details.isNullOrBlank()) {
                errors += AssessmentError.PSYCHOMOTOR_DETAILS_REQUIRED
            } else if (details.length > 500) {
                errors += AssessmentError.PSYCHOMOTOR_DETAILS_TOO_LONG
            }
        }
        if (!EMAIL_REGEX.matches(assessment.email)) {
            errors += AssessmentError.INVALID_EMAIL
        }
        return errors
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

/** Si la frecuencia es >= 4, los obstáculos no aplican y se vacían al guardar. */
fun ClientAssessment.normalized(): ClientAssessment =
    if (activityFrequency >= 4) copy(obstacles = emptySet()) else this
