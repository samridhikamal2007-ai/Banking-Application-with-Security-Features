$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$defaultMvn = "$env:USERPROFILE\tools\apache-maven-3.9.6\bin\mvn.cmd"

if (Test-Path $defaultMvn) {
    $mvnCmd = $defaultMvn
}
elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    $mvnCmd = "mvn"
}
else {
    Write-Error "Maven not found. Install Maven or update the script to use your Maven path."
    exit 1
}

Push-Location $scriptDir
& $mvnCmd -U -DskipTests javafx:run
Pop-Location
