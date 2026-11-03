param([string]$JavaHome, [string]$SdkRoot, [string]$Abi = "arm64-v8a")
$ErrorActionPreference = 'Stop'
if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
if ($SdkRoot) { $env:ANDROID_HOME = $SdkRoot }
if (-not $env:JAVA_HOME) { throw 'Set -JavaHome to JDK 17.' }
Push-Location $PSScriptRoot
try {
    & "$env:JAVA_HOME/bin/java.exe" -classpath "$PSScriptRoot/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain ':TMessagesProj_App:assembleAfatDebug' "-PclaudAbi=$Abi" --console=plain --max-workers=4
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
    Get-ChildItem -LiteralPath "$PSScriptRoot/TMessagesProj_App/build/outputs/apk" -Filter '*.apk' -Recurse | Select-Object FullName, Length
} finally { Pop-Location }
