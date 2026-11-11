# integrations/viewer3d — FUTURO

Aquí irá el visor 3D / AR (Unity as a Library). **No forma parte del MVP** y no está incluido en `settings.gradle.kts`.

Cómo se conectará sin tocar el resto de la app:

1. Crear el módulo `:integrations:viewer3d` con el proyecto Unity exportado (`unityLibrary`).
2. Implementar `ExerciseMediaViewer` (de `feature:media:api`) en una clase `Unity3DViewer` que acepte `ExerciseMedia.Model3D`.
3. Registrarla en Hilt con `@Binds @IntoSet`, igual que `VideoMediaViewer`.
4. Encender `Flag.VIEWER_3D` (y `Flag.AR` para el modo AR con ARCore).

`MediaViewerRegistry` elegirá ese visor automáticamente para el contenido 3D.
