# Runtime Demo Package

This folder is a historical sample package that was used while evaluating an automatic project runtime feature for Jingxuan.

The current platform no longer starts uploaded student projects automatically. Online preview now works by storing a deployed server URL (`previewUrl`) on a featured, published work and opening that URL from the public work detail page.

Structure:

- `backend/` Spring Boot demo application
- `frontend/` Vite + Vue demo frontend
- `sql/init.sql` sample schema file

To create a zip for archival or manual testing on Windows PowerShell:

```powershell
Compress-Archive -Path .\samples\runtime-demo-package\* -DestinationPath .\outputs\runtime-demo-package.zip -Force
```

Do not use this folder as evidence that `/api/runtime/*`, `work_runtime`, or `port_manage` are supported in the current application. Those were removed from the active runtime path.
