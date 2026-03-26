#Requires -Version 5.1
# egs-engine CLI 一键构建测试脚本 (Windows)
# 用法: .\build-and-test.ps1 [测试命令参数]
# 或在 PowerShell 中: powershell -ExecutionPolicy Bypass -File .\build-and-test.ps1 -- --help

$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot
$EgsEngineDir = $ScriptDir
$KangoDir = Join-Path (Split-Path -Parent $ScriptDir) 'kango'
$JarSource = Join-Path $EgsEngineDir 'app\build\libs\app-all.jar'
$JarTarget = Join-Path $KangoDir 'app-all.jar'

function Write-TitleLine {
    param([string]$Text, [ConsoleColor]$Color = 'Yellow')
    Write-Host $Text -ForegroundColor $Color
}

Write-Host ''
Write-TitleLine '========================================'
Write-TitleLine '  egs-engine CLI 一键构建测试脚本'
Write-TitleLine '========================================'
Write-Host ''

Set-Location -LiteralPath $EgsEngineDir

# 步骤2: 构建 shadow jar
Write-TitleLine '[2/4] 构建 egs-engine shadow jar...'
$gradlew = Join-Path $EgsEngineDir 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradlew)) {
    Write-Host "✗ 未找到 Gradle Wrapper: $gradlew" -ForegroundColor Red
    exit 1
}

& $gradlew shadowJar --quiet 2>$null
if ($LASTEXITCODE -ne 0) {
    & $gradlew :app:shadowJar
    if ($LASTEXITCODE -ne 0) {
        Write-Host '✗ Gradle shadowJar 失败' -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

if (-not (Test-Path -LiteralPath $JarSource)) {
    Write-Host "✗ 构建失败: 未找到 $JarSource" -ForegroundColor Red
    exit 1
}
Write-Host "✓ 构建完成: $JarSource" -ForegroundColor Green
Write-Host ''

# 步骤3: 复制 jar 到 kango 目录
Write-TitleLine '[3/4] 复制 jar 到 kango 目录...'
if (-not (Test-Path -LiteralPath $KangoDir -PathType Container)) {
    Write-Host "✗ 目标目录不存在: $KangoDir" -ForegroundColor Red
    exit 1
}
Copy-Item -LiteralPath $JarSource -Destination $JarTarget -Force
Write-Host "✓ 复制完成: $JarTarget" -ForegroundColor Green
Write-Host ''

# 步骤4: 运行 CLI 测试命令
Write-TitleLine '[4/4] 运行 CLI 测试命令...'
Write-TitleLine '----------------------------------------'
Set-Location -LiteralPath $KangoDir

if ($args.Count -eq 0) {
    Write-TitleLine '运行: java -jar app-all.jar --help'
    & java -jar app-all.jar --help
} else {
    $cmdLine = 'java -jar app-all.jar ' + ($args -join ' ')
    Write-TitleLine "运行: $cmdLine"
    & java -jar app-all.jar @args
}

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-TitleLine '----------------------------------------'
Write-Host "✓ CLI 测试完成" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  构建和测试全部完成!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
