$base = "http://localhost:8080"
$jar = New-Object System.Net.CookieJar
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$session.Cookies = $jar

# Login
$loginBody = "username=admin&password=admin"
$r = Invoke-WebRequest -Uri "$base/login" -Method POST -Body $loginBody -ContentType "application/x-www-form-urlencoded" -WebSession $session -MaximumRedirection 5
Write-Host "Login status:" $r.StatusCode

$me = Invoke-RestMethod -Uri "$base/api/users/me" -WebSession $session
Write-Host "User:" $me.username "mode:" $me.businessMode "rate:" $me.defaultHourlyRate

# Ensure freelance mode + rate
$settings = @{ businessMode = "FREELANCE"; defaultHourlyRate = 25 } | ConvertTo-Json
Invoke-RestMethod -Uri "$base/api/users/me/settings" -Method PATCH -Body $settings -ContentType "application/json" -WebSession $session | Out-Null

# Client
$clients = Invoke-RestMethod -Uri "$base/api/clients?page=0&size=10" -WebSession $session
$clientList = if ($clients.content) { $clients.content } else { $clients }
if (-not $clientList -or $clientList.Count -eq 0) {
    $c = @{ firstName = "Drom"; lastName = "Project"; phone = "+15550001234"; email = "drom@example.com" } | ConvertTo-Json
    $client = Invoke-RestMethod -Uri "$base/api/clients" -Method POST -Body $c -ContentType "application/json" -WebSession $session
    $clientId = $client.id
    Write-Host "Created client:" $clientId
} else {
    $clientId = $clientList[0].id
    Write-Host "Using client:" $clientId
}

# Book job
$when = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
$job = @{
    clientId = $clientId
    jobTitle = "Tatalance freelance smoke test"
    notes = "UI redesign"
    pickupDateTime = $when
    pickupLocation = "Remote"
    dropoffLocation = "Remote"
    pricingMode = "HOURLY"
    hourlyRate = 25
} | ConvertTo-Json
$ride = Invoke-RestMethod -Uri "$base/api/rides" -Method POST -Body $job -ContentType "application/json" -WebSession $session
Write-Host "Booked job:" $ride.id "status:" $ride.status

# Start timer
$ride = Invoke-RestMethod -Uri "$base/api/rides/$($ride.id)/start" -Method POST -Body "{}" -ContentType "application/json" -WebSession $session
Write-Host "After start:" $ride.status

Start-Sleep -Seconds 2

$timer = Invoke-RestMethod -Uri "$base/api/rides/$($ride.id)/timer" -WebSession $session
Write-Host "Timer running:" $timer.running "seconds:" $timer.workedSeconds "billable:" $timer.billableAmount

# Pause
$ride = Invoke-RestMethod -Uri "$base/api/rides/$($ride.id)/timer/pause" -Method POST -Body "{}" -ContentType "application/json" -WebSession $session
Write-Host "After pause:" $ride.status "segments:" $ride.workSegments.Count

# Resume
$ride = Invoke-RestMethod -Uri "$base/api/rides/$($ride.id)/timer/resume" -Method POST -Body "{}" -ContentType "application/json" -WebSession $session
Write-Host "After resume:" $ride.status "segments:" $ride.workSegments.Count

Start-Sleep -Seconds 1

# Complete
$ride = Invoke-RestMethod -Uri "$base/api/rides/$($ride.id)/complete" -Method POST -Body "{}" -ContentType "application/json" -WebSession $session
Write-Host "Completed billable:" $ride.billableAmount "durationMin:" $ride.durationMinutes

# Invoice
$inv = Invoke-RestMethod -Uri "$base/api/invoices" -Method POST -Body (@{ rideId = $ride.id } | ConvertTo-Json) -ContentType "application/json" -WebSession $session
Write-Host "Invoice:" $inv.invoiceNumber "total:" $inv.total

# Static pages
$fh = Invoke-WebRequest -Uri "$base/freelance.html" -WebSession $session
Write-Host "freelance.html:" $fh.StatusCode "length:" $fh.Content.Length

Write-Host "SMOKE OK"