package com.caracolas.gestortrainer.feature.groups.api

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.GroupId
import com.caracolas.gestortrainer.core.model.TrainerId
import kotlinx.coroutines.flow.Flow

/** RESERVADO. Flag.GROUP_CLASSES apagado en el MVP: la UI solo muestra "Próximamente". */
data class Group(val id: GroupId, val trainerId: TrainerId, val name: String, val members: List<ClientId>)

data class GroupDraft(val name: String, val discipline: String, val maxMembers: Int)

interface GroupClassService {
    suspend fun createGroup(trainerId: TrainerId, draft: GroupDraft): Result<GroupId>
    fun observeGroups(trainerId: TrainerId): Flow<List<Group>>
}
