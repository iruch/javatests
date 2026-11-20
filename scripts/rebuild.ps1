# Полная пересборка приложения «Java-тесты для Study Hub» в Docker (Windows).
# Запуск: двойной щелчок по scripts\rebuild.cmd или .\scripts\rebuild.cmd из папки проекта.
#   -Yes     не спрашивать подтверждение;
#   -DryRun  только показать, что будет сделано, ничего не удаляя.
param(
    [switch]$Yes,
    [switch]$DryRun
)

# Docker пишет прогресс в stderr; при 'Stop' Windows PowerShell принял бы его за ошибку.
$ErrorActionPreference = 'Continue'
Set-Location (Split-Path -Parent $PSScriptRoot)

$AppUrl = 'http://localhost:8097'
$Title = 'Java-тесты для Study Hub'
$Question = 'Приложение будет полностью удалено и заново пересобрано, все данные прошлых тестов будут удалены. Продолжить?'

function Stop-WithError([string]$Message) {
    Write-Host ''
    Write-Host "Ошибка: $Message" -ForegroundColor Red
    Read-Host 'Нажмите Enter, чтобы закрыть окно' | Out-Null
    exit 1
}

function Confirm-Rebuild {
    Add-Type -AssemblyName System.Windows.Forms, System.Drawing
    $form = New-Object System.Windows.Forms.Form
    $form.Text = $Title
    $form.StartPosition = 'CenterScreen'
    $form.FormBorderStyle = 'FixedDialog'
    $form.MaximizeBox = $false
    $form.MinimizeBox = $false
    $form.TopMost = $true
    $form.ClientSize = New-Object System.Drawing.Size(440, 150)
    $form.Font = New-Object System.Drawing.Font('Segoe UI', 10)

    $label = New-Object System.Windows.Forms.Label
    $label.Text = $Question
    $label.Location = New-Object System.Drawing.Point(20, 18)
    $label.Size = New-Object System.Drawing.Size(400, 70)
    $form.Controls.Add($label)

    $yesButton = New-Object System.Windows.Forms.Button
    $yesButton.Text = 'Да'
    $yesButton.Location = New-Object System.Drawing.Point(236, 102)
    $yesButton.Size = New-Object System.Drawing.Size(90, 30)
    $yesButton.DialogResult = [System.Windows.Forms.DialogResult]::Yes
    $form.Controls.Add($yesButton)

    $noButton = New-Object System.Windows.Forms.Button
    $noButton.Text = 'Нет'
    $noButton.Location = New-Object System.Drawing.Point(334, 102)
    $noButton.Size = New-Object System.Drawing.Size(90, 30)
    $noButton.DialogResult = [System.Windows.Forms.DialogResult]::No
    $form.Controls.Add($noButton)

    # Enter и Esc означают «Нет»: удалить данные можно только явным нажатием «Да».
    $form.AcceptButton = $noButton
    $form.CancelButton = $noButton
    $form.Add_Shown({ $noButton.Focus() })
    return $form.ShowDialog() -eq [System.Windows.Forms.DialogResult]::Yes
}

function Invoke-Step([string]$Description, [string[]]$Arguments) {
    Write-Host $Description -ForegroundColor Cyan
    if ($DryRun) {
        Write-Host "  [dry-run] docker $($Arguments -join ' ')"
        return 0
    }
    # Out-Host keeps docker's output on screen instead of in the function's return value,
    # otherwise the caller would get "output lines + exit code" and treat it as a failure.
    & docker @Arguments | Out-Host
    return [int]$LASTEXITCODE
}

if (-not $Yes -and -not (Confirm-Rebuild)) {
    exit 0
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Stop-WithError 'Docker не найден. Установите Docker Desktop и повторите.'
}
& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Stop-WithError 'Docker не запущен. Запустите Docker Desktop, дождитесь готовности и повторите.'
}

$code = Invoke-Step '1/4 Удаляю контейнеры, образы и базу данных приложения...' @('compose', 'down', '--volumes', '--rmi', 'all', '--remove-orphans')
if ($code -ne 0) {
    Write-Host 'Часть ресурсов удалить не удалось (например, образ используется другим контейнером). Продолжаю.' -ForegroundColor Yellow
}

$code = Invoke-Step '2/4 Собираю и запускаю приложение заново...' @('compose', 'up', '-d', '--build')
if ($code -ne 0) {
    Stop-WithError 'docker compose up завершился с ошибкой. Подробности выше.'
}

Write-Host '3/4 Жду, пока приложение запустится...' -ForegroundColor Cyan
$ready = $false
for ($i = 0; $i -lt 90 -and -not $ready; $i++) {
    try {
        Invoke-WebRequest -Uri $AppUrl -UseBasicParsing -TimeoutSec 3 | Out-Null
        $ready = $true
    } catch {
        Start-Sleep -Seconds 2
    }
}
if (-not $ready) {
    Stop-WithError "Приложение не ответило за 3 минуты. Логи: docker compose logs app"
}

Write-Host "4/4 Открываю $AppUrl" -ForegroundColor Cyan
if ($DryRun) {
    Write-Host "  [dry-run] Start-Process $AppUrl"
} else {
    Start-Process $AppUrl
}
exit 0
