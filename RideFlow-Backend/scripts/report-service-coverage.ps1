$jacocoPath = Join-Path $PSScriptRoot "..\\target\\site\\jacoco\\jacoco.xml"

if (-not (Test-Path $jacocoPath)) {
    Write-Error "JaCoCo report not found at $jacocoPath. Run './mvnw test' first."
    exit 1
}

$xml = [xml](Get-Content $jacocoPath)
$package = $xml.report.package | Where-Object { $_.name -eq "com/rideflow/demo/service/impl" }

if ($null -eq $package) {
    Write-Error "Package com/rideflow/demo/service/impl was not found in JaCoCo report."
    exit 1
}

$rows = @()
$totalMissed = 0
$totalCovered = 0

foreach ($class in $package.class) {
    $counter = $class.counter | Where-Object { $_.type -eq "INSTRUCTION" }
    if ($null -eq $counter) {
        continue
    }

    $missed = [int]$counter.missed
    $covered = [int]$counter.covered
    $totalMissed += $missed
    $totalCovered += $covered

    $rows += [pscustomobject]@{
        Class = $class.name
        Coverage = if (($missed + $covered) -eq 0) {
            0
        } else {
            [math]::Round(($covered * 100.0) / ($missed + $covered), 2)
        }
    }
}

$rows | Sort-Object Coverage | Format-Table -AutoSize
$total = [math]::Round(($totalCovered * 100.0) / ($totalCovered + $totalMissed), 2)
Write-Host ""
Write-Host "Service layer instruction coverage: $total%"
