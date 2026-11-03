param([Parameter(Mandatory=$true)][string]$Apk,[Parameter(Mandatory=$true)][int]$VersionCode)
$ErrorActionPreference='Stop'
$env:GCM_INTERACTIVE='never'; $env:GIT_TERMINAL_PROMPT='0'
$credential=@('protocol=https','host=github.com','') | git credential fill
$token=($credential | Where-Object { $_.StartsWith('password=') }).Substring(9)
if (-not $token) {throw 'GitHub credentials unavailable'}
$headers=@{Authorization="Bearer $token";Accept='application/vnd.github+json';'X-GitHub-Api-Version'='2022-11-28'}
$repo='andreevvadia-design/TelegrammClaud'
$tag="claud-$VersionCode"
$body=@{tag_name=$tag;target_commitish='main';name="claud mode $VersionCode";draft=$true;prerelease=$false;body='Adds in-app release checking and APK signature verification. Removes Tinker. Includes user-controlled background messaging mode. Android 7+ ARM64. Device validation remains necessary.'} | ConvertTo-Json
$r=Invoke-RestMethod -Method Post -Uri "https://api.github.com/repos/$repo/releases" -Headers $headers -ContentType 'application/json' -Body $body
$asset="claud-mode-arm64-$VersionCode.apk"
$upload=$r.upload_url.Split('{')[0]+"?name=$asset"
Invoke-RestMethod -Method Post -Uri $upload -Headers $headers -ContentType 'application/vnd.android.package-archive' -InFile (Resolve-Path -LiteralPath $Apk).Path | Select-Object name,size
Invoke-RestMethod -Method Patch -Uri "https://api.github.com/repos/$repo/releases/$($r.id)" -Headers $headers -ContentType 'application/json' -Body '{"draft":false}' | Select-Object html_url,tag_name
$token=$null;$credential=$null;$headers=$null
