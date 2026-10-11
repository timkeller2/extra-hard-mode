#Requires -Version 5.1
<#
.SYNOPSIS
  Downloads the latest Tougher jar into a Minecraft mods folder.

.DESCRIPTION
  Reads the rolling GitHub release and replaces tougher*.jar and
  extrahardmode*.jar. Other mods are left in place. Close Minecraft first.

.PARAMETER ModsDir
  One mods folder to update. When omitted, the official
  %APPDATA%\.minecraft\mods folder is used, plus any Prism, MultiMC,
  Modrinth, CurseForge, or ATLauncher mods folder that already has Tougher.
#>
param(
    [string]$ModsDir
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
} catch {
}

$Api = 'https://api.github.com/repos/timkeller2/extra-hard-mode/releases/latest'
$Headers = @{
    'User-Agent' = 'Tougher-Updater'
    'Accept'     = 'application/vnd.github+json'
}

function Get-Sha256([string]$Path) {
    return (Get-FileHash -Algorithm SHA256 -Path $Path).Hash.ToLowerInvariant()
}

function Test-JarFile([string]$Path) {
    $stream = [System.IO.File]::Open($Path, 'Open', 'Read', 'Read')
    try {
        $magic = New-Object byte[] 4
        $read = $stream.Read($magic, 0, 4)
        return $read -eq 4 -and $magic[0] -eq 0x50 -and $magic[1] -eq 0x4B
    } finally {
        $stream.Dispose()
    }
}

function Get-TougherAsset($Release) {
    $assets = @($Release.assets)
    $versioned = $assets | Where-Object {
        $_.name -like 'tougher-*.jar' -and $_.name -notlike '*sources*'
    } | Select-Object -First 1
    if ($versioned) {
        return $versioned
    }
    return $assets | Where-Object { $_.name -eq 'tougher.jar' } | Select-Object -First 1
}

function Get-OldJars([string]$Folder) {
    return @(Get-ChildItem -Path $Folder -File -ErrorAction SilentlyContinue | Where-Object {
        $_.Name -like 'tougher*.jar' -or $_.Name -like 'extrahardmode*.jar'
    })
}

function Add-ModsDir($Set, [string]$Folder) {
    if (-not $Folder) {
        return
    }
    if (-not (Test-Path -LiteralPath $Folder)) {
        New-Item -ItemType Directory -Path $Folder | Out-Null
    }
    $full = (Resolve-Path -LiteralPath $Folder).Path
    if (-not $Set.ContainsKey($full)) {
        $Set[$full] = $true
    }
}

function Find-ModsDirs {
    $set = @{}
    $minecraft = Join-Path $env:APPDATA '.minecraft'
    if (Test-Path -LiteralPath $minecraft) {
        Add-ModsDir $set (Join-Path $minecraft 'mods')
    }
    $roots = @(
        (Join-Path $env:APPDATA 'PrismLauncher'),
        (Join-Path $env:APPDATA '.prismlauncher'),
        (Join-Path $env:APPDATA 'MultiMC'),
        (Join-Path $env:APPDATA 'ModrinthApp'),
        (Join-Path $env:APPDATA 'com.modrinth.app'),
        (Join-Path $env:USERPROFILE 'curseforge\minecraft\Instances'),
        (Join-Path $env:APPDATA 'ATLauncher')
    )
    foreach ($root in $roots) {
        if (-not (Test-Path -LiteralPath $root)) {
            continue
        }
        $modsFolders = Get-ChildItem -Path $root -Directory -Recurse -Filter mods -Depth 6 -ErrorAction SilentlyContinue
        foreach ($folder in @($modsFolders)) {
            $old = Get-OldJars $folder.FullName
            if ($old.Count -gt 0) {
                Add-ModsDir $set $folder.FullName
            }
        }
    }
    return @($set.Keys)
}

function Install-Into([string]$Folder, [string]$VerifiedJar, [string]$DestName, [string]$ExpectedHash) {
    $dest = Join-Path $Folder $DestName
    $old = Get-OldJars $Folder
    $current = $false
    foreach ($file in $old) {
        if ($file.Name -eq $DestName -and $ExpectedHash -and (Get-Sha256 $file.FullName) -eq $ExpectedHash) {
            $current = $true
        }
    }
    if (-not $current) {
        try {
            Copy-Item -LiteralPath $VerifiedJar -Destination $dest -Force
        } catch {
            throw "Could not write $dest. Close Minecraft and run this again."
        }
    }
    $old = Get-OldJars $Folder
    foreach ($file in $old) {
        if ($file.FullName -eq $dest) {
            continue
        }
        try {
            Remove-Item -LiteralPath $file.FullName -Force
        } catch {
            throw "Could not remove $($file.Name). Close Minecraft and run this again."
        }
    }
    if ($current) {
        Write-Host "Already up to date: $dest"
    } else {
        Write-Host "Updated: $dest"
    }
}

$release = Invoke-RestMethod -Uri $Api -Headers $Headers
$asset = Get-TougherAsset $release
if (-not $asset -or -not $asset.browser_download_url) {
    throw 'The latest release has no Tougher jar.'
}
$expected = ''
if ($asset.digest -and $asset.digest.ToString().StartsWith('sha256:')) {
    $expected = $asset.digest.ToString().Substring(7).ToLowerInvariant()
}

if ($ModsDir) {
    if (-not (Test-Path -LiteralPath $ModsDir)) {
        New-Item -ItemType Directory -Path $ModsDir | Out-Null
    }
    $targets = @((Resolve-Path -LiteralPath $ModsDir).Path)
} else {
    $targets = @(Find-ModsDirs)
}
if ($targets.Count -eq 0) {
    throw "No Minecraft mods folder found. Install Minecraft, or run this again with -ModsDir `"C:\path\to\mods`"."
}

$temp = Join-Path ([System.IO.Path]::GetTempPath()) ('tougher-download-' + [guid]::NewGuid().ToString('n') + '.jar')
try {
    Write-Host "Downloading $($release.name)"
    Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $temp -UseBasicParsing -Headers $Headers
    if (-not (Test-JarFile $temp)) {
        throw 'The download is not a jar.'
    }
    $actual = Get-Sha256 $temp
    if ($expected -and $actual -ne $expected) {
        throw "Downloaded jar did not match the published checksum."
    }
    foreach ($folder in $targets) {
        Install-Into $folder $temp $asset.name $expected
    }
    Write-Host 'Restart Minecraft to load the new jar. Fabric API stays installed.'
} finally {
    if (Test-Path -LiteralPath $temp) {
        Remove-Item -LiteralPath $temp -Force -ErrorAction SilentlyContinue
    }
}
