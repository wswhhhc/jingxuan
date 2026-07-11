[CmdletBinding()]
param(
    [switch]$SkipInstall
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $projectRoot "backend"
$frontendDir = Join-Path $projectRoot "frontend"
$logDir = Join-Path $projectRoot "logs\dev"

New-Item -ItemType Directory -Path $logDir -Force | Out-Null

# 各阶段有意保持幂等：后续步骤失败时不自动停止已就绪的共享服务，修复问题后可直接重跑续上。

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#") -or -not $trimmed.Contains("=")) {
            continue
        }

        $parts = $trimmed.Split("=", 2)
        $name = $parts[0].Trim()
        $value = $parts[1].Trim().Trim('"').Trim("'")
        if (-not $name) {
            continue
        }

        $currentValue = [Environment]::GetEnvironmentVariable($name, "Process")
        if ([string]::IsNullOrWhiteSpace($currentValue)) {
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}

function Read-SecretPlainText {
    param([string]$Prompt)

    $secureValue = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        $plainValue = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        if ([string]::IsNullOrWhiteSpace($plainValue)) {
            throw "未提供必需的密码。"
        }
        return $plainValue
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Get-RequiredCommand {
    param(
        [string[]]$Names,
        [string]$InstallHint
    )

    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($null -ne $command) {
            return $command
        }
    }

    throw "缺少命令 $($Names -join '/')。$InstallHint"
}

function Test-TcpPort {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutMilliseconds = 800
    )

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connect = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $connect.AsyncWaitHandle.WaitOne($TimeoutMilliseconds)) {
            return $false
        }
        $client.EndConnect($connect)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Wait-TcpPort {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutSeconds = 60
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        if (Test-TcpPort -HostName $HostName -Port $Port) {
            return $true
        }
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $deadline)

    return $false
}

function Test-RedisPing {
    param(
        [string]$HostName = "127.0.0.1",
        [int]$Port = 6379
    )

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connect = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $connect.AsyncWaitHandle.WaitOne(1000)) {
            return $false
        }
        $client.EndConnect($connect)
        $client.ReceiveTimeout = 1500
        $client.SendTimeout = 1500

        $stream = $client.GetStream()
        $request = [Text.Encoding]::ASCII.GetBytes("*1`r`n`$4`r`nPING`r`n")
        $stream.Write($request, 0, $request.Length)
        $buffer = New-Object byte[] 64
        $count = $stream.Read($buffer, 0, $buffer.Length)
        if ($count -le 0) {
            return $false
        }

        $response = [Text.Encoding]::ASCII.GetString($buffer, 0, $count)
        return $response.StartsWith("+PONG")
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 120
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                return $true
            }
        }
        catch {
            # 服务启动期间连接失败是预期状态，继续轮询。
        }
        Start-Sleep -Seconds 1
    } while ([DateTime]::UtcNow -lt $deadline)

    return $false
}

function Get-ServiceExecutable {
    param([string]$ServiceName)

    $serviceInfo = Get-CimInstance Win32_Service -Filter "Name='$ServiceName'" -ErrorAction SilentlyContinue
    if ($null -eq $serviceInfo -or [string]::IsNullOrWhiteSpace($serviceInfo.PathName)) {
        return $null
    }

    $match = [regex]::Match($serviceInfo.PathName, '^\s*"([^"]+)"|^\s*([^\s]+)')
    if (-not $match.Success) {
        return $null
    }

    if ($match.Groups[1].Success) {
        return $match.Groups[1].Value
    }
    return $match.Groups[2].Value
}

function Find-RedisExecutable {
    $command = Get-Command "redis-server.exe" -ErrorAction SilentlyContinue
    if ($null -ne $command -and (Test-Path -LiteralPath $command.Source)) {
        return $command.Source
    }

    $serviceExe = Get-ServiceExecutable -ServiceName "Redis"
    if (-not [string]::IsNullOrWhiteSpace($serviceExe)) {
        if (Test-Path -LiteralPath $serviceExe) {
            return $serviceExe
        }

        # Windows 服务可能保留了升级前的旧路径，在原目录下查找唯一的新版程序。
        $serviceDir = Split-Path -Parent $serviceExe
        if (Test-Path -LiteralPath $serviceDir) {
            $candidates = @(Get-ChildItem -LiteralPath $serviceDir -Filter "redis-server.exe" -File -Recurse -ErrorAction SilentlyContinue)
            if ($candidates.Count -eq 1) {
                return $candidates[0].FullName
            }
            if ($candidates.Count -gt 1) {
                throw "Redis 服务路径已失效，且 $serviceDir 下发现多个 redis-server.exe，请先修复 Redis 服务配置。"
            }
        }
    }

    return $null
}

function Ensure-MySql {
    if (Test-TcpPort -HostName "127.0.0.1" -Port 3306) {
        Write-Ok "MySQL 已在 127.0.0.1:3306 运行"
        return
    }

    $service = Get-Service -Name "MySQL" -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        throw "未找到 MySQL Windows 服务，且 3306 端口未监听。"
    }

    Write-Step "启动 MySQL Windows 服务"
    Start-Service -Name $service.Name
    if (-not (Wait-TcpPort -HostName "127.0.0.1" -Port 3306 -TimeoutSeconds 30)) {
        throw "MySQL 服务已尝试启动，但 3306 端口仍未就绪。"
    }
    Write-Ok "MySQL 已启动"
}

function Ensure-Redis {
    if (Test-TcpPort -HostName "127.0.0.1" -Port 6379) {
        if (Test-RedisPing) {
            Write-Ok "Redis 已在 127.0.0.1:6379 运行（PING=PONG）"
            return
        }
        throw "6379 端口已监听，但 Redis PING 未通过；请检查端口占用或 Redis 鉴权配置。"
    }

    Write-Step "启动 Redis"
    $service = Get-Service -Name "Redis" -ErrorAction SilentlyContinue
    if ($null -ne $service) {
        try {
            Start-Service -Name $service.Name
        }
        catch {
            Write-Warn "无法直接启动 Redis Windows 服务，将改为启动本地 Redis 进程。"
        }
    }

    if (-not (Wait-TcpPort -HostName "127.0.0.1" -Port 6379 -TimeoutSeconds 8)) {
        $redisExe = Find-RedisExecutable
        if ([string]::IsNullOrWhiteSpace($redisExe) -or -not (Test-Path -LiteralPath $redisExe)) {
            throw "Redis 服务未能启动，也未找到 redis-server.exe。"
        }

        $redisDir = Split-Path -Parent $redisExe
        $arguments = @()
        foreach ($configName in @("redis.windows.conf", "redis.conf")) {
            $redisConfig = Join-Path $redisDir $configName
            if (Test-Path -LiteralPath $redisConfig) {
                # MSYS2 版 Redis 会错误转换 Windows 绝对路径；工作目录已指向 Redis 目录。
                $arguments += $configName
                break
            }
        }
        if ($arguments.Count -eq 0) {
            throw "已找到 redis-server.exe，但同目录下没有 redis.windows.conf 或 redis.conf。"
        }

        Start-Process `
            -FilePath $redisExe `
            -ArgumentList $arguments `
            -WorkingDirectory $redisDir `
            -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $logDir "redis.out.log") `
            -RedirectStandardError (Join-Path $logDir "redis.err.log")
    }

    if (-not (Wait-TcpPort -HostName "127.0.0.1" -Port 6379 -TimeoutSeconds 30)) {
        throw "Redis 启动失败，6379 端口未就绪。"
    }
    if (-not (Test-RedisPing)) {
        throw "Redis 已监听 6379，但 PING 未返回 PONG。"
    }
    Write-Ok "Redis 已启动（PING=PONG）"
}

function Get-MySqlExecutable {
    $command = Get-Command "mysql.exe" -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $serverExe = Get-ServiceExecutable -ServiceName "MySQL"
    if ([string]::IsNullOrWhiteSpace($serverExe)) {
        return $null
    }

    $candidate = Join-Path (Split-Path -Parent $serverExe) "mysql.exe"
    if (Test-Path -LiteralPath $candidate) {
        return $candidate
    }
    return $null
}

function Test-DatabaseAccess {
    param([string]$Password)

    $mysqlExe = Get-MySqlExecutable
    if ([string]::IsNullOrWhiteSpace($mysqlExe)) {
        Write-Warn "未找到 mysql.exe，跳过数据库凭据与结构预检。"
        return
    }

    $oldPassword = [Environment]::GetEnvironmentVariable("MYSQL_PWD", "Process")
    [Environment]::SetEnvironmentVariable("MYSQL_PWD", $Password, "Process")
    try {
        $databaseName = & $mysqlExe -h 127.0.0.1 -P 3306 -uroot --batch --skip-column-names -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='jingxuan';" 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "无法使用当前 DB_PASSWORD 连接本机 MySQL，请检查根目录 .env，或重新运行后安全输入密码。"
        }
        if (($databaseName | Out-String).Trim() -ne "jingxuan") {
            throw "本机 MySQL 中不存在 jingxuan 数据库，请先导入 sql 目录中的数据库脚本。"
        }

        $schemaQuery = @"
SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME)
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'jingxuan'
  AND (
    (TABLE_NAME = 'score_batch' AND COLUMN_NAME IN ('notice_title', 'notice_content'))
    OR (TABLE_NAME = 'sys_notice' AND COLUMN_NAME = 'target_scope')
    OR (TABLE_NAME = 'work_comment' AND COLUMN_NAME = 'guest_name')
  )
UNION ALL
SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'jingxuan'
  AND TABLE_NAME IN ('student_task', 'delete_request');
"@
        $presentItems = @(& $mysqlExe -h 127.0.0.1 -P 3306 -uroot --batch --skip-column-names -e $schemaQuery 2>$null)
        $requiredItems = @(
            "score_batch.notice_title",
            "score_batch.notice_content",
            "sys_notice.target_scope",
            "work_comment.guest_name",
            "student_task",
            "delete_request"
        )
        $missingItems = @($requiredItems | Where-Object { $_ -notin $presentItems })
        if ($missingItems.Count -gt 0) {
            Write-Warn "数据库仍缺少近期结构：$($missingItems -join ', ')。项目可启动，但对应新功能需先执行相关迁移。"
        }
        else {
            Write-Ok "数据库连接和近期核心结构检查通过"
        }
    }
    finally {
        [Environment]::SetEnvironmentVariable("MYSQL_PWD", $oldPassword, "Process")
    }
}

function Ensure-DevMailConfiguration {
    $mailUsername = [Environment]::GetEnvironmentVariable("MAIL_USERNAME", "Process")
    $mailPassword = [Environment]::GetEnvironmentVariable("MAIL_PASSWORD", "Process")
    if (-not [string]::IsNullOrWhiteSpace($mailUsername) -and -not [string]::IsNullOrWhiteSpace($mailPassword)) {
        return
    }

    # MailConfig 在凭据为空时不会注册 JavaMailSender，导致整个应用无法启动。
    # 开发环境使用不可投递的本地占位配置；需要验证码邮件时再在 .env 中填写真实配置。
    [Environment]::SetEnvironmentVariable("MAIL_HOST", "127.0.0.1", "Process")
    [Environment]::SetEnvironmentVariable("MAIL_PORT", "1025", "Process")
    [Environment]::SetEnvironmentVariable("MAIL_USERNAME", "dev-disabled@localhost", "Process")
    [Environment]::SetEnvironmentVariable("MAIL_PASSWORD", "dev-disabled", "Process")
    Write-Warn "未配置邮件凭据，已启用仅用于启动的开发占位配置；发送验证码功能暂不可用。"
}

function Show-LogTail {
    param([string]$Path)
    if (Test-Path -LiteralPath $Path) {
        Write-Host "--- $Path（最后 30 行）---" -ForegroundColor DarkYellow
        Get-Content -LiteralPath $Path -Tail 30
    }
}

Import-DotEnv -Path (Join-Path $projectRoot ".env")

$DbPassword = [Environment]::GetEnvironmentVariable("DB_PASSWORD", "Process")
if ([string]::IsNullOrWhiteSpace($DbPassword)) {
    $DbPassword = Read-SecretPlainText -Prompt "未检测到 .env/DB_PASSWORD，请输入本机 MySQL root 密码"
}
[Environment]::SetEnvironmentVariable("DB_PASSWORD", $DbPassword, "Process")

Write-Step "检查开发工具"
$maven = Get-RequiredCommand -Names @("mvn.cmd", "mvn") -InstallHint "请安装 Maven 3.9+ 并加入 PATH。"
$npm = Get-RequiredCommand -Names @("npm.cmd", "npm") -InstallHint "请安装 Node.js 20+ 并加入 PATH。"
Get-RequiredCommand -Names @("java.exe", "java") -InstallHint "请安装 JDK 17+ 并加入 PATH。" | Out-Null
Write-Ok "Java、Maven、Node/npm 均可用"

Write-Step "检查基础服务"
Ensure-MySql
Ensure-Redis
Test-DatabaseAccess -Password $DbPassword
Ensure-DevMailConfiguration

[Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_URL", "jdbc:mysql://127.0.0.1:3306/jingxuan?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowMultiQueries=true", "Process")
[Environment]::SetEnvironmentVariable("SPRING_DATA_REDIS_HOST", "127.0.0.1", "Process")
[Environment]::SetEnvironmentVariable("SPRING_DATA_REDIS_PORT", "6379", "Process")

$backendUrl = "http://127.0.0.1:8080"
$backendHealthUrl = "$backendUrl/public/works?pageNum=1&pageSize=1"
$backendOutLog = Join-Path $logDir "backend.out.log"
$backendErrLog = Join-Path $logDir "backend.err.log"

Write-Step "启动后端"
if (Test-TcpPort -HostName "127.0.0.1" -Port 8080) {
    if (-not (Wait-HttpOk -Url $backendHealthUrl -TimeoutSeconds 5)) {
        throw "8080 端口已被其他进程占用，且菁选后端健康检查未通过。"
    }
    Write-Ok "后端已在 8080 运行"
}
else {
    Start-Process `
        -FilePath $maven.Source `
        -ArgumentList @("-q", "spring-boot:run", "-Dspring-boot.run.profiles=dev") `
        -WorkingDirectory $backendDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput $backendOutLog `
        -RedirectStandardError $backendErrLog

    if (-not (Wait-HttpOk -Url $backendHealthUrl -TimeoutSeconds 180)) {
        Show-LogTail -Path $backendOutLog
        Show-LogTail -Path $backendErrLog
        throw "后端启动失败或健康检查超时。"
    }
    Write-Ok "后端已启动：$backendUrl"
}

Write-Step "准备前端依赖"
$viteCommandPath = Join-Path $frontendDir "node_modules\.bin\vite.cmd"
$packageJsonPath = Join-Path $frontendDir "package.json"
$packageLockPath = Join-Path $frontendDir "package-lock.json"
$dependencyStampPath = Join-Path $logDir "frontend-dependencies.sha256"
$fingerprintParts = @((Get-FileHash -LiteralPath $packageJsonPath -Algorithm SHA256).Hash)
if (Test-Path -LiteralPath $packageLockPath) {
    $fingerprintParts += (Get-FileHash -LiteralPath $packageLockPath -Algorithm SHA256).Hash
}
$dependencyFingerprint = $fingerprintParts -join ":"
$savedDependencyFingerprint = if (Test-Path -LiteralPath $dependencyStampPath) {
    (Get-Content -LiteralPath $dependencyStampPath -Raw).Trim()
}
else {
    ""
}
$frontendDependenciesReady = (Test-Path -LiteralPath $viteCommandPath) -and
    ($savedDependencyFingerprint -eq $dependencyFingerprint)

if (-not $frontendDependenciesReady -and $SkipInstall) {
    Write-Warn "前端依赖缺失或配置已变化，但已按参数跳过安装。"
}
elseif (-not $frontendDependenciesReady) {
    Push-Location $frontendDir
    try {
        if (Test-Path -LiteralPath $packageLockPath) {
            & $npm.Source ci --registry=https://registry.npmjs.org
        }
        else {
            & $npm.Source install --registry=https://registry.npmjs.org
        }
        if ($LASTEXITCODE -ne 0) {
            throw "前端依赖安装失败。"
        }
        Set-Content -LiteralPath $dependencyStampPath -Value $dependencyFingerprint
        Write-Ok "前端依赖安装完成"
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Ok "前端依赖与 package/lock 指纹一致"
}

$frontendUrl = "http://127.0.0.1:5173/jingxuan/"
$frontendOutLog = Join-Path $logDir "frontend.out.log"
$frontendErrLog = Join-Path $logDir "frontend.err.log"

Write-Step "启动前端"
if (Test-TcpPort -HostName "127.0.0.1" -Port 5173) {
    if (-not (Wait-HttpOk -Url $frontendUrl -TimeoutSeconds 5)) {
        throw "5173 端口已被其他进程占用，且菁选前端健康检查未通过。"
    }
    Write-Ok "前端已在 5173 运行"
}
else {
    Start-Process `
        -FilePath $npm.Source `
        -ArgumentList @("run", "dev", "--", "--host", "127.0.0.1") `
        -WorkingDirectory $frontendDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput $frontendOutLog `
        -RedirectStandardError $frontendErrLog

    if (-not (Wait-HttpOk -Url $frontendUrl -TimeoutSeconds 120)) {
        Show-LogTail -Path $frontendOutLog
        Show-LogTail -Path $frontendErrLog
        throw "前端启动失败或健康检查超时。"
    }
    Write-Ok "前端已启动：$frontendUrl"
}

Write-Host "`n菁选项目启动完成" -ForegroundColor Green
Write-Host "前端：$frontendUrl"
Write-Host "后端：$backendUrl"
Write-Host "接口文档：$backendUrl/doc.html"
Write-Host "日志目录：$logDir"
