# tests_api.ps1 - Matrice de tests Auth + Evenements (PowerShell 5.1 ou 7)
# Lancer le backend (mvn exec:java), puis :
#   powershell -ExecutionPolicy Bypass -File .\tests_api.ps1
# Produit : affichage colore (a capturer), resultats_tests.csv, tests_observes.tex
# Fichier volontairement en ASCII (pas d'accents) pour eviter les problemes d'encodage de PowerShell 5.1.

# =====================  CONFIG : A ADAPTER  =====================
$Base   = "http://localhost:8080"
$Origin = "http://localhost:5173"

# Compte admin deja present en base (cree avec un hash genere par TON code)
# Admin account already in the DB. Credentials come from .env.test (never committed).
if (Test-Path ".\.env.test") {
    Get-Content ".\.env.test" | ForEach-Object {
        if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)$') {
            Set-Item -Path "env:$($matches[1])" -Value $matches[2].Trim().Trim('"')
        }
    }
}
$AdminEmail = $env:ADMIN_EMAIL
$AdminPass  = $env:ADMIN_PASS
if (-not $AdminEmail -or -not $AdminPass) {
    Write-Host "ADMIN_EMAIL / ADMIN_PASS missing. Copy .env.example to .env.test and fill it." -ForegroundColor Red
    exit 1
}

$Pass = "Test1234!"

# Noms des champs JSON de TES DTO (a ajuster selon tes classes Request/Response)
$ChampToken = "token"   # champ du token dans la reponse du login
$ChampId    = "id"      # champ de l'identifiant dans un evenement renvoye par GET /events

function CorpsRegister($email, $role, $entreprise = $null) {
    $c = [ordered]@{ nom = "Test"; prenom = "Auto"; email = $email; motDePasse = $Pass; telephone = "20123456"; role = $role }
    if ($entreprise) { $c["nomEntreprise"] = $entreprise }
    return $c
}
$EvBon = [ordered]@{ titre = "Salon Test"; description = "Evenement de test"; dateDebut = "2026-12-01"; dateFin = "2026-12-03"; lieu = "Tunis" }
# =================================================================

$s = Get-Random -Maximum 99999
$VisiteurEmail = "visiteur$s@test.tn"
$ExposantEmail = "exposant$s@test.tn"
$A4Email       = "pirate$s@test.tn"
$A7Email       = "sansentreprise$s@test.tn"
$BloqueEmail   = "bloque$s@test.tn"

$Resultats = New-Object System.Collections.ArrayList

function Appel {
    param([string]$Methode, [string]$Chemin, $Corps = $null, [string]$Token = $null, [hashtable]$Entetes = @{})
    $h = @{}
    foreach ($k in $Entetes.Keys) { $h[$k] = $Entetes[$k] }
    if ($Token) { $h["Authorization"] = "Bearer $Token" }
    $p = @{ Uri = "$Base$Chemin"; Method = $Methode; UseBasicParsing = $true; Headers = $h }
    if ($null -ne $Corps) {
        if ($Corps -is [string]) { $json = $Corps } else { $json = $Corps | ConvertTo-Json -Depth 5 }
        $p["Body"] = [System.Text.Encoding]::UTF8.GetBytes($json)
        $p["ContentType"] = "application/json; charset=utf-8"
    }
    try {
        $r = Invoke-WebRequest @p
        return [pscustomobject]@{ Code = [int]$r.StatusCode; Corps = [string]$r.Content; Entetes = $r.Headers }
    } catch {
        $resp = $_.Exception.Response
        $code = 0; $corps = ""; $ent = $null
        if ($resp) {
            $code = [int]$resp.StatusCode
            if ($_.ErrorDetails -and $_.ErrorDetails.Message) { $corps = $_.ErrorDetails.Message }
            else { try { $sr = New-Object System.IO.StreamReader($resp.GetResponseStream()); $corps = $sr.ReadToEnd() } catch { } }
            $ent = $resp.Headers
        } else { $corps = $_.Exception.Message }
        return [pscustomobject]@{ Code = $code; Corps = $corps; Entetes = $ent }
    }
}

function Entete($rep, $nom) {
    $e = $rep.Entetes
    if ($null -eq $e) { return "" }
    try {
        if ($e.GetType().Name -eq "HttpResponseHeaders") {
            $v = $null
            if ($e.TryGetValues($nom, [ref]$v)) { return ($v -join ",") } else { return "" }
        }
        $v = $e[$nom]
        if ($v -is [array]) { return ($v -join ",") } else { return [string]$v }
    } catch { return "" }
}

function Fuite($rep) { return ($rep.Corps -match '(?i)mot_?de_?passe|hash|password') }

function Verifier($Id, $Scenario, [int[]]$Attendu, $Rep, [bool]$Cond = $true, [string]$Extra = "") {
    $ok = ($Attendu -contains $Rep.Code) -and $Cond
    if ($ok) { $statut = "OK"; $couleur = "Green" } else { $statut = "KO"; $couleur = "Red" }
    Write-Host ("[{0}] {1} -> {2} (attendu {3}) {4} {5}" -f $Id, $Scenario, $Rep.Code, ($Attendu -join "/"), $statut, $Extra) -ForegroundColor $couleur
    $c = $Rep.Corps
    if ($c.Length -gt 220) { $c = $c.Substring(0, 220) + "..." }
    Write-Host ("      " + $c) -ForegroundColor DarkGray
    [void]$Resultats.Add([pscustomobject]@{ Id = $Id; Scenario = $Scenario; Attendu = ($Attendu -join " ou "); Observe = $Rep.Code; Statut = $statut })
}

function Token($rep) { try { return ($rep.Corps | ConvertFrom-Json).$ChampToken } catch { return $null } }

function Section($t) { Write-Host ""; Write-Host "=== $t ===" -ForegroundColor Cyan }

# ---------- Prerequis : token admin ----------
$repAdmin = Appel "POST" "/auth/login" ([ordered]@{ email = $AdminEmail; motDePasse = $AdminPass })
$TokAdmin = Token $repAdmin
if (-not $TokAdmin) {
    Write-Host "Connexion admin impossible (code $($repAdmin.Code)). Verifie `$AdminEmail, `$AdminPass et `$ChampToken." -ForegroundColor Red
    exit 1
}

# ---------- A. Inscription ----------
Section "A. Inscription (POST /auth/register)"
$r = Appel "POST" "/auth/register" (CorpsRegister $VisiteurEmail "VISITEUR")
Verifier "A1" "Inscription visiteur valide" @(201) $r (-not (Fuite $r)) "(pas de hash dans la reponse)"

$r = Appel "POST" "/auth/register" (CorpsRegister $ExposantEmail "EXPOSANT" "Entreprise Test")
Verifier "A2" "Inscription exposant valide" @(201) $r (-not (Fuite $r))

$r = Appel "POST" "/auth/register" (CorpsRegister $VisiteurEmail "VISITEUR")
Verifier "A3" "Email deja utilise" @(400, 409) $r

$r = Appel "POST" "/auth/register" (CorpsRegister $A4Email "ADMINISTRATEUR")
Verifier "A4" "Inscription avec role ADMINISTRATEUR" @(400, 403) $r $true "(si 201 : verifier le role en base)"

$r = Appel "POST" "/auth/register" ([ordered]@{ nom = "X"; prenom = "Y"; email = "pas-un-email"; motDePasse = ""; role = "VISITEUR" })
Verifier "A5" "Email invalide et mot de passe vide" @(400) $r

$r = Appel "POST" "/auth/register" "{ ceci n'est pas du json"
Verifier "A6" "JSON mal forme" @(400) $r

$r = Appel "POST" "/auth/register" (CorpsRegister $A7Email "EXPOSANT")
Verifier "A7" "Exposant sans nom d'entreprise (rollback)" @(400) $r $true "(puis SQL : COUNT = 0)"

# ---------- B. Connexion ----------
Section "B. Connexion (POST /auth/login)"
$rB1 = Appel "POST" "/auth/login" ([ordered]@{ email = $VisiteurEmail; motDePasse = $Pass })
Verifier "B1" "Identifiants corrects (visiteur)" @(200) $rB1 ((-not (Fuite $rB1)) -and [bool](Token $rB1))
$TokVisiteur = Token $rB1

$rB2 = Appel "POST" "/auth/login" ([ordered]@{ email = $VisiteurEmail; motDePasse = "mauvais-mdp" })
Verifier "B2" "Mauvais mot de passe" @(401) $rB2

$rB3 = Appel "POST" "/auth/login" ([ordered]@{ email = "inconnu$s@test.tn"; motDePasse = "mauvais-mdp" })
Verifier "B3" "Email inconnu (meme message que B2)" @(401) $rB3 ($rB2.Corps -eq $rB3.Corps) "(messages identiques : $($rB2.Corps -eq $rB3.Corps))"

[void](Appel "POST" "/auth/register" (CorpsRegister $BloqueEmail "VISITEUR"))
Write-Host ""
Write-Host "B4 : execute ce SQL (phpMyAdmin) puis appuie sur Entree :" -ForegroundColor Yellow
Write-Host "     UPDATE utilisateur SET est_bloque = TRUE WHERE email = '$BloqueEmail';" -ForegroundColor Yellow
[void](Read-Host)
$r = Appel "POST" "/auth/login" ([ordered]@{ email = $BloqueEmail; motDePasse = $Pass })
Verifier "B4" "Compte bloque" @(403) $r

$r = Appel "POST" "/auth/login" "{}"
Verifier "B5" "Corps de connexion vide" @(400) $r

$rE = Appel "POST" "/auth/login" ([ordered]@{ email = $ExposantEmail; motDePasse = $Pass })
$TokExposant = Token $rE

# ---------- C. Validite du token ----------
Section "C. Validite du token (POST /events)"
$r = Appel "POST" "/events" $EvBon
Verifier "C1" "Sans header Authorization" @(401) $r

$r = Appel "POST" "/events" $EvBon "abc123"
Verifier "C2" "Token invalide (abc123)" @(401) $r

$i = $TokAdmin.Length - 5
if ($TokAdmin[$i] -eq 'A') { $nc = 'B' } else { $nc = 'A' }
$TokFalsifie = $TokAdmin.Substring(0, $i) + $nc + $TokAdmin.Substring($i + 1)
$r = Appel "POST" "/events" $EvBon $TokFalsifie
Verifier "C3" "Signature du token modifiee" @(401) $r

Write-Host "[C4] Token expire -> MANUEL : reduis temporairement la duree de vie du JWT, reconnecte-toi, attends, puis rejoue POST /events (attendu 401)." -ForegroundColor Yellow
[void]$Resultats.Add([pscustomobject]@{ Id = "C4"; Scenario = "Token expire"; Attendu = "401"; Observe = "manuel"; Statut = "MANUEL" })

$r = Appel "POST" "/events" $EvBon $null @{ Authorization = $TokAdmin }
Verifier "C5" "Authorization sans le mot Bearer" @(401) $r

# ---------- D / E. Droits par role et erreurs ----------
Section "D/E. Droits par role et routes evenements"
$r = Appel "POST" "/events" $EvBon $TokAdmin
Verifier "D1" "Creation evenement (admin)" @(201) $r

$liste = $null
try { $liste = (Appel "GET" "/events").Corps | ConvertFrom-Json } catch { }
$IdEv = $null
if ($liste) { $IdEv = ($liste | Select-Object -Last 1).$ChampId }
Write-Host "      (id de l'evenement de test : $IdEv)" -ForegroundColor DarkGray
if (-not $IdEv) { Write-Host "Impossible de recuperer l'id d'un evenement : adapte `$ChampId. Les tests D4-D7 seront KO." -ForegroundColor Red }

$r = Appel "GET" "/events"
Verifier "E1" "Liste des evenements sans token" @(200) $r

$r = Appel "GET" "/events/$IdEv"
Verifier "E2" "Evenement existant" @(200) $r

$r = Appel "POST" "/events" $EvBon $TokVisiteur
Verifier "D2" "Creation evenement (visiteur)" @(403) $r

$r = Appel "POST" "/events" $EvBon $TokExposant
Verifier "D3" "Creation evenement (exposant)" @(403) $r

$r = Appel "PUT" "/events/$IdEv" $EvBon $TokAdmin
Verifier "D4" "Modification evenement (admin)" @(200) $r

$r = Appel "PUT" "/events/$IdEv" $EvBon $TokVisiteur
Verifier "D5" "Modification evenement (visiteur)" @(403) $r

$ev8 = [ordered]@{}
foreach ($k in $EvBon.Keys) { $ev8[$k] = $EvBon[$k] }
$ev8["role"] = "ADMINISTRATEUR"
$r = Appel "POST" "/events" $ev8 $TokVisiteur
Verifier "D8" "Visiteur + role ADMINISTRATEUR dans le body" @(403) $r

$r = Appel "DELETE" "/events/$IdEv" $null $TokVisiteur
Verifier "D7" "Suppression evenement (visiteur)" @(403) $r

$r = Appel "DELETE" "/events/$IdEv" $null $TokAdmin
Verifier "D6" "Suppression evenement (admin)" @(204) $r

$r = Appel "GET" "/events/999999"
Verifier "E3" "Evenement inexistant" @(404) $r

$r = Appel "GET" "/events/abc"
Verifier "E4" "Identifiant non numerique" @(400) $r

$ev5 = [ordered]@{}
foreach ($k in $EvBon.Keys) { $ev5[$k] = $EvBon[$k] }
$ev5["dateFin"] = "2026-11-01"
$r = Appel "POST" "/events" $ev5 $TokAdmin
Verifier "E5" "date_fin anterieure a date_debut" @(400) $r

$r = Appel "PATCH" "/events" $EvBon $TokAdmin
Verifier "E6" "Methode non supportee (PATCH)" @(400, 405) $r

$r = Appel "GET" "/xyz"
Verifier "E7" "Route inconnue" @(404) $r

# ---------- F. CORS ----------
Section "F. CORS"
$r = Appel "OPTIONS" "/events" $null $null @{ Origin = $Origin; "Access-Control-Request-Method" = "POST"; "Access-Control-Request-Headers" = "authorization,content-type" }
$ao = Entete $r "Access-Control-Allow-Origin"
$ah = Entete $r "Access-Control-Allow-Headers"
Verifier "F1" "Preflight OPTIONS /events" @(200, 204) $r (($ao -ne "") -and ($ah -match "(?i)authorization")) "(Allow-Origin='$ao' Allow-Headers='$ah')"

$r = Appel "POST" "/events" $EvBon $null @{ Origin = $Origin }
$ao = Entete $r "Access-Control-Allow-Origin"
Verifier "F2" "Headers CORS presents sur une erreur 401" @(401) $r ($ao -ne "") "(Allow-Origin='$ao')"

# ---------- G. Verifications SQL ----------
Section "G. Verifications en base"
$mysql = "C:\xampp\mysql\bin\mysql.exe"
$requetes = @(
    "SELECT email, LEFT(mot_de_passe_hash, 25) AS debut_hash FROM utilisateur ORDER BY id DESC LIMIT 4;",
    "SELECT COUNT(*) AS nb_lignes_A7 FROM utilisateur WHERE email = '$A7Email';",
    "SELECT email, role FROM utilisateur WHERE email = '$A4Email';"
)
foreach ($q in $requetes) {
    Write-Host "SQL> $q" -ForegroundColor Yellow
    if (Test-Path $mysql) { & $mysql -u root gestion_salon -e $q } else { Write-Host "     (mysql.exe introuvable : execute cette requete dans phpMyAdmin)" -ForegroundColor DarkYellow }
}

# ---------- Synthese + exports ----------
Section "Synthese"
$Resultats | Format-Table Id, Scenario, Attendu, Observe, Statut -AutoSize
$ko = @($Resultats | Where-Object { $_.Statut -eq "KO" }).Count
Write-Host "$ko test(s) KO sur $($Resultats.Count)" -ForegroundColor $(if ($ko -eq 0) { "Green" } else { "Red" })

$Resultats | Export-Csv -Path ".\resultats_tests.csv" -NoTypeInformation -Encoding UTF8

function EchapTex($t) { return ($t -replace '_', '\_' -replace '&', '\&' -replace '%', '\%' -replace '#', '\#') }
$lignes = foreach ($x in $Resultats) {
    "{0} & {1} & {2} & {3} \\ \hline" -f $x.Id, (EchapTex $x.Scenario), $x.Attendu, $x.Observe
}
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "tests_observes.tex"), (($lignes -join "`r`n") + "`r`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host "Fichiers produits : resultats_tests.csv et tests_observes.tex" -ForegroundColor Cyan
