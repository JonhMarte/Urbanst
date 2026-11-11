package com.caracolas.gestortrainer.feature.activities.impl

import com.caracolas.gestortrainer.feature.activities.api.ActivityDraft
import com.caracolas.gestortrainer.feature.activities.api.ActivityType
import com.caracolas.gestortrainer.feature.activities.api.ActivityValidator
import com.caracolas.gestortrainer.feature.activities.api.ValidationError
import javax.inject.Inject
import kotlin.time.Duration

class DefaultActivityValidator @Inject constructor() : ActivityValidator {
    override fun validate(draft: ActivityDraft): List<ValidationError> = buildList {
        if (draft.title.isBlank()) add(ValidationError.TITLE_REQUIRED)
        if (draft.description.isBlank()) add(ValidationError.DESCRIPTION_REQUIRED)
        if (draft.averageDuration <= Duration.ZERO) add(ValidationError.DURATION_REQUIRED)
        when (draft.type) {
            ActivityType.PHYSICAL ->
                if (draft.muscles.isEmpty()) add(ValidationError.MUSCLES_REQUIRED_FOR_PHYSICAL)
            ActivityType.NUTRITION ->
                if (draft.muscles.isNotEmpty()) add(ValidationError.MUSCLES_NOT_ALLOWED_FOR_NUTRITION)
        }
    }
}
