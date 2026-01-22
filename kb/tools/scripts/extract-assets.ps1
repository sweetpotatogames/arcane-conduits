# Hytale Asset Extraction Script
# Usage: .\extract-assets.ps1 [-AssetsZip <path>] [-OutputDir <path>]

param(
    [string]$AssetsZip = "D:\games\hytale-downloader\latest-extract\Assets.zip",
    [string]$OutputDir = "D:\source\hytale\assets\vanilla"
)

if (-not (Test-Path $AssetsZip)) {
    Write-Error "Assets.zip not found at: $AssetsZip"
    exit 1
}

Write-Host "Extracting assets from $AssetsZip..."
Write-Host "Output directory: $OutputDir"
Write-Host "This may take several minutes (file is ~1.5GB)..."

Expand-Archive -Path $AssetsZip -DestinationPath $OutputDir -Force

Write-Host "Extraction complete!"

# Display asset summary
$BlockCount = (Get-ChildItem -Path "$OutputDir\*\Blocks" -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
$ItemCount = (Get-ChildItem -Path "$OutputDir\*\Items" -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
$ModelCount = (Get-ChildItem -Path "$OutputDir" -Recurse -Filter "*.hym" -ErrorAction SilentlyContinue | Measure-Object).Count
$TextureCount = (Get-ChildItem -Path "$OutputDir" -Recurse -Filter "*.png" -ErrorAction SilentlyContinue | Measure-Object).Count

Write-Host ""
Write-Host "Asset Summary:"
Write-Host "  Blocks: $BlockCount files"
Write-Host "  Items: $ItemCount files"
Write-Host "  Models: $ModelCount .hym files"
Write-Host "  Textures: $TextureCount .png files"
