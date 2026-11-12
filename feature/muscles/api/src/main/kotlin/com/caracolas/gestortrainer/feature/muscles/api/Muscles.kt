package com.caracolas.gestortrainer.feature.muscles.api

import com.caracolas.gestortrainer.core.model.MuscleId
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class MuscleGroup { UPPER, CORE, LOWER }

data class Muscle(val id: MuscleId, val name: String, val group: MuscleGroup)

interface MuscleCatalog {
    fun all(): List<Muscle>
}

/**
 * Dibuja el cuerpo con los músculos seleccionados resaltados.
 * MVP: silueta 2D. FUTURO: un renderizador 3D implementará esta misma interfaz.
 */
interface MuscleMapRenderer {
    @Composable
    fun Render(selected: Set<MuscleId>, modifier: Modifier)

    /** Igual que [Render], pero tocar una zona del cuerpo activa/desactiva ese músculo. */
    @Composable
    fun RenderSelectable(selected: Set<MuscleId>, onToggle: (MuscleId) -> Unit, modifier: Modifier)
}
