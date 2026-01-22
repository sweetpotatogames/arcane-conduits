# Hytale Server Decompilation Script
# Usage: .\decompile.ps1 [-ServerJar <path>] [-OutputDir <path>]

param(
    [string]$ServerJar = "D:\games\hytale-downloader\latest-extract\Server\HytaleServer.jar",
    [string]$OutputDir = "D:\source\hytale\tools\decompiled-server"
)

$VineflowerJar = Join-Path $PSScriptRoot "..\vineflower-1.11.2.jar"

if (-not (Test-Path $VineflowerJar)) {
    Write-Host "Downloading Vineflower..."
    $VineflowerUrl = "https://github.com/Vineflower/vineflower/releases/download/1.11.2/vineflower-1.11.2.jar"
    Invoke-WebRequest -Uri $VineflowerUrl -OutFile $VineflowerJar
}

if (-not (Test-Path $ServerJar)) {
    Write-Error "Server JAR not found at: $ServerJar"
    exit 1
}

Write-Host "Decompiling $ServerJar to $OutputDir..."
Write-Host "This may take several minutes..."

# Vineflower options:
# -dgs=1  : Decompile generic signatures
# -hdc=0  : Hide default constructor
# -asc=1  : ASCII string characters
# -udv=1  : Use debug var names

java -jar $VineflowerJar -dgs=1 -hdc=0 -asc=1 -udv=1 $ServerJar $OutputDir

Write-Host "Decompilation complete!"
Write-Host "Output: $OutputDir"
