$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$py = Join-Path $root ".venv\Scripts\python.exe"
if (-not (Test-Path $py)) {
  Write-Error "venv not found: $py"
  exit 1
}
Push-Location $root
try {
  & $py -m ingest.pipeline --root $root --full @args
}
finally {
  Pop-Location
}
