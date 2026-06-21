# Install drom-flow Claude skills as Grok project skills (.grok/skills/<name>/SKILL.md)
param(
    [string]$DromFlowHome = "C:\Users\drom\IdeaProjects\drom-flow",
    [string]$TargetRepo = "C:\Users\drom\IdeaProjects\tatalance"
)

$ErrorActionPreference = "Stop"

$primarySrc = Join-Path $DromFlowHome ".claude\skills"
$templateSrc = Join-Path $DromFlowHome "template\.claude\skills"
$destRoot = Join-Path $TargetRepo ".grok\skills"

function Get-SkillNames {
    $names = @{}
    foreach ($root in @($primarySrc, $templateSrc)) {
        if (-not (Test-Path $root)) { continue }
        Get-ChildItem $root -Directory | ForEach-Object { $names[$_.Name] = $true }
    }
    $names.Keys | Sort-Object
}

function Get-SkillSourceDir([string]$Name) {
    $primary = Join-Path $primarySrc $Name
    if (Test-Path $primary) { return $primary }
    $template = Join-Path $templateSrc $Name
    if (Test-Path $template) { return $template }
    return $null
}

function Convert-ToGrokSkillMd([string]$SourceFile, [string]$SkillName) {
    $raw = [System.IO.File]::ReadAllText($SourceFile, [System.Text.UTF8Encoding]::new($false))
    if ($raw -notmatch '(?s)^---\r?\n(.*?)\r?\n---\r?\n(.*)$') {
        throw "Missing YAML frontmatter in $SourceFile"
    }
    $front = $Matches[1]
    $body = $Matches[2].TrimStart()

    $name = $SkillName
    $description = $null
    foreach ($line in ($front -split '\r?\n')) {
        if ($line -match '^name:\s*(.+)$') { $name = $Matches[1].Trim() }
        if ($line -match '^description:\s*(.+)$') { $description = $Matches[1].Trim() }
    }
    if (-not $description) { $description = "drom-flow skill: $name" }

    $slash = "/$name"
    if ($description -notmatch [regex]::Escape($slash)) {
        $description = "$description. Use when the user runs $slash or asks for $name workflow help."
    }

    return @"
---
name: $name
description: >
  $description
metadata:
  short-description: "drom-flow: $name"
  source: drom-flow
---

$body
"@
}

$installed = @()
$skipped = @()

New-Item -ItemType Directory -Force -Path $destRoot | Out-Null

foreach ($skill in Get-SkillNames) {
    $srcDir = Get-SkillSourceDir $skill
    if (-not $srcDir) {
        $skipped += "$skill (no source dir)"
        continue
    }

    $mainFile = Join-Path $srcDir "$skill.md"
    if (-not (Test-Path $mainFile)) {
        $skipped += "$skill (missing $skill.md)"
        continue
    }

    $destDir = Join-Path $destRoot $skill
    if (Test-Path $destDir) {
        Remove-Item $destDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $destDir | Out-Null

    Get-ChildItem $srcDir -Recurse -File | ForEach-Object {
        $rel = $_.FullName.Substring($srcDir.Length).TrimStart('\')
        if ($rel -eq "$skill.md") { return }
        $out = Join-Path $destDir $rel
        $outParent = Split-Path $out -Parent
        if (-not (Test-Path $outParent)) {
            New-Item -ItemType Directory -Force -Path $outParent | Out-Null
        }
        Copy-Item $_.FullName $out -Force
    }

    $grokMd = Convert-ToGrokSkillMd $mainFile $skill
    $skillPath = Join-Path $destDir "SKILL.md"
    [System.IO.File]::WriteAllText($skillPath, $grokMd, [System.Text.UTF8Encoding]::new($false))
    $installed += $skill
}

Write-Output "Installed $($installed.Count) Grok skills to $destRoot"
$installed | ForEach-Object { Write-Output "  + $_" }
if ($skipped.Count -gt 0) {
    Write-Output "Skipped:"
    $skipped | ForEach-Object { Write-Output "  - $_" }
}