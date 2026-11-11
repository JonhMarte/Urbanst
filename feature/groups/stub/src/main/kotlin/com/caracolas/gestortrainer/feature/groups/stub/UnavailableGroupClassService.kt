package com.caracolas.gestortrainer.feature.groups.stub

import com.caracolas.gestortrainer.core.model.GroupId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.groups.api.Group
import com.caracolas.gestortrainer.feature.groups.api.GroupClassService
import com.caracolas.gestortrainer.feature.groups.api.GroupDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Implementación vacía mientras Flag.GROUP_CLASSES esté apagado. */
class UnavailableGroupClassService @Inject constructor() : GroupClassService {
    override suspend fun createGroup(trainerId: TrainerId, draft: GroupDraft): Result<GroupId> =
        Result.failure(UnsupportedOperationException("Clases grupales: próximamente"))

    override fun observeGroups(trainerId: TrainerId): Flow<List<Group>> = flowOf(emptyList())
}
