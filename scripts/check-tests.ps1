# Check script for closed-loop: runs backend tests and E2E, outputs machine-readable summary
# Java: use surefire reports or output
$javaReport = "backend/target/surefire-reports"
if (Test-Path $javaReport) {
    $xmlFiles = Get-ChildItem $javaReport -Filter TEST-*.xml
    $javaFailed = ($xmlFiles | ForEach-Object { [xml](Get-Content $_.FullName) } | Select-Xml -XPath 'testsuite/@failures' | ForEach-Object { [int]$_.Node.Value } | Measure-Object -Sum).Sum
    $javaErrors = ($xmlFiles | ForEach-Object { [xml](Get-Content $_.FullName) } | Select-Xml -XPath 'testsuite/@errors' | ForEach-Object { [int]$_.Node.Value } | Measure-Object -Sum).Sum
    $javaSkipped = ($xmlFiles | ForEach-Object { [xml](Get-Content $_.FullName) } | Select-Xml -XPath 'testsuite/@skipped' | ForEach-Object { [int]$_.Node.Value } | Measure-Object -Sum).Sum
    $javaTests = ($xmlFiles | ForEach-Object { [xml](Get-Content $_.FullName) } | Select-Xml -XPath 'testsuite/@tests' | ForEach-Object { [int]$_.Node.Value } | Measure-Object -Sum).Sum
    $javaPassed = $javaTests - $javaFailed - $javaErrors - $javaSkipped
} else {
    $javaPassed = 0
    $javaFailed = 0
}

# E2E
$e2eJson = npm run test:e2e --prefix tests/e2e -- --reporter=json 2>&1 | Out-String
$e2ePassed = 0
$e2eFailed = 0
try {
    $json = $e2eJson | ConvertFrom-Json
    if ($json -and $json.stats) {
        $e2ePassed = $json.stats.passed
        $e2eFailed = $json.stats.failed
    }
} catch {
    # ignore
}

$totalPassed = $javaPassed + $e2ePassed
$totalFailed = $javaFailed + $e2eFailed

$output = @{
    timestamp = Get-Date -Format o
    java = @{ passed = $javaPassed; failed = $javaFailed }
    e2e = @{ passed = $e2ePassed; failed = $e2eFailed }
    total = @{ passed = $totalPassed; failed = $totalFailed }
    pass = ($totalFailed -eq 0)
    summary = "Java: $javaPassed passed, $javaFailed failed; E2E: $e2ePassed passed, $e2eFailed failed"
} | ConvertTo-Json -Depth 3

Write-Output $output
$output | Out-File -Encoding utf8 test-check-report.json

if ($totalFailed -eq 0) { exit 0 } else { exit 1 }