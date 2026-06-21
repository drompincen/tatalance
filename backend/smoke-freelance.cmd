@echo off
setlocal EnableDelayedExpansion
set BASE=http://localhost:8080
set AUTH=admin:admin

echo === me ===
curl.exe -s -u %AUTH% %BASE%/api/users/me
echo.

curl.exe -s -u %AUTH% -X PATCH %BASE%/api/users/me/settings -H "Content-Type: application/json" -d "{\"businessMode\":\"FREELANCE\",\"defaultHourlyRate\":25}" >nul

for /f "delims=" %%i in ('powershell -NoProfile -Command "(Get-Date).ToString('HHmmss')"') do set TAIL=%%i
set PHONE=+1555%TAIL%

echo === create client phone %PHONE% ===
curl.exe -s -u %AUTH% -X POST %BASE%/api/clients -H "Content-Type: application/json" -d "{\"firstName\":\"Drom\",\"lastName\":\"Project\",\"phone\":\"%PHONE%\"}" > %TEMP%\client.json
type %TEMP%\client.json
echo.

for /f "tokens=2 delims=:," %%a in ('findstr /C:"\"id\"" %TEMP%\client.json') do set CID=%%~a
set CID=!CID:"=!
set CID=!CID: =!

for /f "delims=" %%i in ('powershell -NoProfile -Command "(Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffZ')"') do set WHEN=%%i

echo === book job client !CID! ===
curl.exe -s -u %AUTH% -X POST %BASE%/api/rides -H "Content-Type: application/json" -d "{\"clientId\":\"!CID!\",\"jobTitle\":\"Tatalance smoke\",\"pickupDateTime\":\"!WHEN!\",\"pickupLocation\":\"Remote\",\"dropoffLocation\":\"Remote\",\"pricingMode\":\"HOURLY\",\"hourlyRate\":25}" > %TEMP%\ride.json
type %TEMP%\ride.json
echo.

for /f "tokens=2 delims=:," %%a in ('findstr /C:"\"id\"" %TEMP%\ride.json') do set RID=%%~a
set RID=!RID:"=!
set RID=!RID: =!

echo === start !RID! ===
curl.exe -s -u %AUTH% -X POST %BASE%/api/rides/!RID!/start -H "Content-Type: application/json" -d "{}"
echo.
timeout /t 2 /nobreak >nul

echo === timer ===
curl.exe -s -u %AUTH% %BASE%/api/rides/!RID!/timer
echo.

echo === pause ===
curl.exe -s -u %AUTH% -X POST %BASE%/api/rides/!RID!/timer/pause -H "Content-Type: application/json" -d "{}"
echo.

echo === resume ===
curl.exe -s -u %AUTH% -X POST %BASE%/api/rides/!RID!/timer/resume -H "Content-Type: application/json" -d "{}"
echo.
timeout /t 1 /nobreak >nul

echo === complete ===
curl.exe -s -u %AUTH% -X POST %BASE%/api/rides/!RID!/complete -H "Content-Type: application/json" -d "{}"
echo.

echo === invoice ===
curl.exe -s -u %AUTH% -X POST %BASE%/api/invoices -H "Content-Type: application/json" -d "{\"rideId\":\"!RID!\"}"
echo.

curl.exe -s -o NUL -w "freelance.html HTTP %%{http_code}\n" -u %AUTH% %BASE%/freelance.html
echo SMOKE OK
exit /b 0