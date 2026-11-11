package com.caracolas.gestortrainer.feature.profile.impl

import com.caracolas.gestortrainer.feature.profile.api.PersonalData
import com.caracolas.gestortrainer.feature.profile.api.PrivateBodyData
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidationError
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidator
import javax.inject.Inject

class DefaultProfileValidator @Inject constructor() : ProfileValidator {
    override fun validate(personal: PersonalData): List<ProfileValidationError> = buildList {
        if (personal.fullName.isBlank()) add(ProfileValidationError.FULL_NAME_REQUIRED)
        if (personal.nationality.isBlank()) add(ProfileValidationError.NATIONALITY_REQUIRED)
        val age = personal.age
        if (age == null) {
            add(ProfileValidationError.AGE_REQUIRED)
        } else if (age < 18 && !personal.guardianConsentGiven) {
            add(ProfileValidationError.MINOR_REQUIRES_GUARDIAN_CONSENT)
        }
    }

    override fun validate(body: PrivateBodyData): List<ProfileValidationError> = buildList {
        if (!body.consentGiven) add(ProfileValidationError.BODY_DATA_REQUIRES_CONSENT)
        if (body.weightKg <= 0) add(ProfileValidationError.INVALID_WEIGHT)
        if (body.heightCm <= 0) add(ProfileValidationError.INVALID_HEIGHT)
    }
}
