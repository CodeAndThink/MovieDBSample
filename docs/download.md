# Simulated downloads

Download is a separate feature slice. Existing movie-detail retrieval is unchanged.

- Domain: `DownloadState`, `DownloadStatus`, `DownloadRepository`, and `StartDownload` / `ObserveDownload` / `CancelDownload` use cases. No Android or WorkManager types cross this boundary.
- Data: `DownloadRepositoryImpl` maps WorkInfo into domain state, starts unique work per movie with KEEP, and cancels it. `SimulatedDownloadWorker` advances 0–100% over approximately 20 seconds without reading/writing a downloaded file or making network requests.
- Presentation: `DownloadViewModel` receives use cases and exposes state. Detail dispatches DownloadEvent and renders progress/cancel/completion. Notification runtime permission belongs to the Fragment.
- DI: `DownloadModule` binds the repository interface to its implementation.

WorkManager owns the foreground service (dataSync); no parallel custom service is started. The ongoing notification has progress, a cancellation action, and a deep link to Detail. It disappears when work terminates; Detail retains the terminal state through WorkManager's database. The worker itself can simulate progress without notification permission, but the current Detail UI starts work only when PermissionManager returns allGranted (POST_NOTIFICATIONS on API 33+). Denying permission in this UI therefore does not start a new simulation.

Tests cover state observation/recreation, duplicate starts, cancellation, foreground notification presence, and a complete 20-second worker run on the emulator. This is an educational simulation, not a real media download implementation.
