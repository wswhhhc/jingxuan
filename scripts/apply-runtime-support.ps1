param(
    [string]$MysqlExe = "D:\java\Mysqls\mysql-8.4.5-winx64\bin\mysql.exe",
    [string]$Database = "jingxuan",
    [string]$Username = "root",
    [string]$Password = "252629"
)

$ErrorActionPreference = "Stop"

function Invoke-MysqlCommand {
    param([string]$Sql)

    $Sql | & $MysqlExe "-u$Username" "-p$Password" $Database
}

function Get-ScalarResult {
    param([string]$Sql)

    $result = $Sql | & $MysqlExe "-N" "-B" "-u$Username" "-p$Password" $Database
    return ($result | Select-Object -First 1).ToString().Trim()
}

Write-Host "Applying runtime support schema to $Database ..."

$portTypeExists = Get-ScalarResult @"
SELECT COUNT(*)
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = '$Database'
  AND TABLE_NAME = 'port_manage'
  AND COLUMN_NAME = 'port_type';
"@

if ($portTypeExists -eq '0') {
    Invoke-MysqlCommand @"
ALTER TABLE port_manage
ADD COLUMN port_type VARCHAR(20) NOT NULL DEFAULT 'backend' COMMENT 'port type backend/frontend';
"@
    Write-Host "Added port_manage.port_type"
} else {
    Write-Host "port_manage.port_type already exists"
}

$workRuntimeExists = Get-ScalarResult @"
SELECT COUNT(*)
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = '$Database'
  AND TABLE_NAME = 'work_runtime';
"@

if ($workRuntimeExists -eq '0') {
    Invoke-MysqlCommand @"
CREATE TABLE work_runtime (
    id BIGINT NOT NULL,
    work_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    runtime_type VARCHAR(30) NOT NULL DEFAULT 'windows_process',
    project_path VARCHAR(500) NOT NULL,
    manifest_path VARCHAR(500) DEFAULT NULL,
    backend_port INT DEFAULT NULL,
    frontend_port INT DEFAULT NULL,
    backend_pid BIGINT DEFAULT NULL,
    frontend_pid BIGINT DEFAULT NULL,
    preview_url VARCHAR(500) DEFAULT NULL,
    mysql_schema VARCHAR(100) DEFAULT NULL,
    redis_db INT DEFAULT NULL,
    prepare_time DATETIME DEFAULT NULL,
    start_time DATETIME DEFAULT NULL,
    stop_time DATETIME DEFAULT NULL,
    last_access_time DATETIME DEFAULT NULL,
    error_message VARCHAR(1000) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_id (work_id),
    KEY idx_status (status)
);
"@
    Write-Host "Created work_runtime"
} else {
    Write-Host "work_runtime already exists"
}

Write-Host "Runtime support schema applied."
