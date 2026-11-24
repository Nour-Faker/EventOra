# demo-data.ps1 - Remplit EventOra avec des donnees de demonstration pour la presentation.
#
# A lancer UNE FOIS sur une base vide (apres import de database.sql et SeedDev), backend demarre :
#   powershell -ExecutionPolicy Bypass -File .\demo-data.ps1 -AdminPassword "votre SEED_PASSWORD"
# Options : -Base http://localhost:8081  (si le backend tourne sur un autre port)
#
# Cree : 7 categories, 6 evenements (dont 1 deja termine), 72 stands, 12 types de billets,
# 20 sessions (ateliers, conferences, concours), 9 exposants avec des demandes validees,
# refusees et en attente, 12 visiteurs avec 33 billets, des entrees deja scannees et des
# inscriptions aux sessions. Situations preparees pour la demonstration :
#   - "Atelier poterie de Nabeul" : 11 inscrits sur 12, la derniere place se prend en direct ;
#   - billet "Early bird" du Tunisia Game Fest : epuise ;
#   - "Salon de l'Etudiant 2026" : evenement termine, reservation impossible ;
#   - categorie "Divers" : utilisee par aucun stand, pour montrer une suppression autorisee.
# Tous les comptes de demonstration ont le mot de passe : Demo2026!
# Fichier ASCII (sans accents) pour PowerShell 5.1.

param(
    [string]$Base = "http://localhost:8080",
    [string]$AdminPassword = $env:SEED_PASSWORD
)

$ErrorActionPreference = "Stop"
$DemoPassword = "Demo2026!"

if (-not $AdminPassword) {
    Write-Host "Mot de passe administrateur manquant : ajoutez -AdminPassword `"votre SEED_PASSWORD`"." -ForegroundColor Red
    exit 1
}

function Api([string]$Method, [string]$Path, $Body = $null, [string]$Token = $null) {
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Method = $Method; Uri = "$Base$Path"; Headers = $headers }
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 5
        $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($json)
        $params["ContentType"] = "application/json; charset=utf-8"
    }
    try {
        return Invoke-RestMethod @params
    } catch {
        $detail = $_.ErrorDetails.Message
        if (-not $detail) { $detail = $_.Exception.Message }
        Write-Host "ERREUR $Method $Path -> $detail" -ForegroundColor Red
        exit 1
    }
}

# --- Verifications ---------------------------------------------------------
try { Invoke-RestMethod -Uri "$Base/" | Out-Null }
catch { Write-Host "Backend injoignable sur $Base. Lancez Main dans IntelliJ." -ForegroundColor Red; exit 1 }

$admin = Api POST "/auth/login" @{ email = "admin@salon.tn"; motDePasse = $AdminPassword }
$agent = Api POST "/auth/login" @{ email = "agent@salon.tn"; motDePasse = $AdminPassword }
$A = $admin.token

# Appel direct : renvoyee par une fonction, une liste vide [] arrive comme un seul objet en PowerShell 5.1
$events = Invoke-RestMethod -Uri "$Base/events"
$existingCount = @($events | ForEach-Object { $_ }).Count
if ($existingCount -gt 0) {
    Write-Host "La base contient deja des evenements : ce script se lance sur une base vide." -ForegroundColor Yellow
    Write-Host "Reimportez database.sql, relancez SeedDev puis relancez ce script." -ForegroundColor Yellow
    exit 1
}
Write-Host "Creation des donnees de demonstration..." -ForegroundColor Cyan

# --- Categories ---------------------------------------------------------------
$cats = @{}
foreach ($c in @(
    @("Technologie", "Startups, intelligence artificielle, robotique"),
    @("Alimentation", "Produits du terroir et gastronomie"),
    @("Artisanat", "Createurs, ceramique, textile"),
    @("Mode et design", "Stylistes et designers"),
    @("Jeux video", "Studios, e-sport et materiel de jeu"),
    @("Education", "Ecoles, universites et formations"),
    @("Divers", "Categorie libre, utilisee par aucun stand"))) {
    $cats[$c[0]] = (Api POST "/categories" @{ nom = $c[0]; description = $c[1] } $A).id
}

# --- Evenements -----------------------------------------------------------------
function NewEvent($titre, $description, $debut, $fin, $lieu) {
    return Api POST "/events" @{ titre = $titre; description = $description; dateDebut = $debut; dateFin = $fin; lieu = $lieu } $A
}
$e1 = NewEvent "Salon de l'Innovation Tunis 2027" "Le rendez-vous des startups et de la tech tunisienne : conferences, demonstrations et rencontres professionnelles." "2027-03-10" "2027-03-12" "Parc des expositions du Kram, Tunis"
$e4 = NewEvent "Tunisia Game Fest 2027" "Le festival du jeu video : tournois e-sport, studios independants et ateliers de creation." "2027-04-17" "2027-04-18" "Cite de la Culture, Tunis"
$e2 = NewEvent "Festival des Saveurs" "Trois jours de gastronomie, de chefs et de produits locaux au bord de la mer." "2027-05-21" "2027-05-23" "Sousse"
$e3 = NewEvent "Expo Artisanat Mediterranee" "Ceramique, textile et savoir-faire des artisans de la Mediterranee." "2027-07-08" "2027-07-10" "Hammamet"
$e5 = NewEvent "Salon Mode et Design Tunis" "Jeunes createurs, defiles et ateliers de mode durable." "2027-09-24" "2027-09-26" "Palais des congres, Tunis"
$e6 = NewEvent "Salon de l'Etudiant 2026" "Ecoles, universites et conseillers d'orientation pour les bacheliers." "2026-10-15" "2026-10-17" "Parc des expositions du Kram, Tunis"

# --- Stands : 12 par evenement, 3 rangees de 4 (lettre, categorie, prix de base) --------
$layout = @{
    ($e1.id) = @(@("A", "Technologie", 900), @("B", "Alimentation", 600), @("C", "Artisanat", 450))
    ($e4.id) = @(@("A", "Jeux video", 800), @("B", "Technologie", 700), @("C", "Alimentation", 400))
    ($e2.id) = @(@("A", "Alimentation", 650), @("B", "Alimentation", 500), @("C", "Artisanat", 350))
    ($e3.id) = @(@("A", "Artisanat", 500), @("B", "Artisanat", 400), @("C", "Mode et design", 450))
    ($e5.id) = @(@("A", "Mode et design", 850), @("B", "Mode et design", 650), @("C", "Artisanat", 400))
    ($e6.id) = @(@("A", "Education", 300), @("B", "Technologie", 350), @("C", "Alimentation", 250))
}
$stand = @{}   # cle "idEvenement-numero"
foreach ($ev in @($e1, $e4, $e2, $e3, $e5, $e6)) {
    foreach ($row in $layout[$ev.id]) {
        foreach ($i in 1..4) {
            $s = Api POST "/events/$($ev.id)/stands" @{ categorieId = $cats[$row[1]]; numero = "$($row[0])$i"
                emplacement = "Hall $($row[0]), allee $i"; superficie = 9 + 3 * ($i % 3); prix = $row[2] + 50 * $i } $A
            $stand["$($ev.id)-$($row[0])$i"] = $s.id
        }
    }
}

# --- Types de billets -------------------------------------------------------------
$types = @{}
foreach ($t in @(
    @($e1, "Standard", 20, 300), @($e1, "VIP", 75, 40), @($e1, "Etudiant", 10, 150),
    @($e4, "Early bird", 20, 5), @($e4, "Pass week-end", 30, 250), @($e4, "Joueur tournoi", 15, 64),
    @($e2, "Pass 3 jours", 35, 200), @($e2, "Journee", 15, 400),
    @($e3, "Entree", 10, 250),
    @($e5, "Entree", 12, 400), @($e5, "Pass defile", 40, 120),
    @($e6, "Entree gratuite", 0, 500))) {
    $types["$($t[0].id)-$($t[1])"] = (Api POST "/events/$($t[0].id)/ticket-types" @{ nom = $t[1]; prix = $t[2]; quota = $t[3] } $A).id
}

# --- Sessions : ateliers, conferences, concours ---------------------------------------
$sessions = @{}   # cle "nom court"
function NewSession($key, $ev, $titre, $description, $debut, $fin, $capacite) {
    $sessions[$key] = (Api POST "/events/$($ev.id)/sessions" @{ titre = $titre; description = $description
        heureDebut = $debut; heureFin = $fin; capaciteMax = $capacite } $A).id
}
NewSession "keynote"  $e1 "Keynote : l'IA au service des entreprises" "Ouverture officielle du salon" "2027-03-10T10:00:00" "2027-03-10T11:00:00" 200
NewSession "robot"    $e1 "Atelier robotique" "Construire et programmer un robot en 90 minutes" "2027-03-10T14:00:00" "2027-03-10T15:30:00" 30
NewSession "table"    $e1 "Table ronde : financer sa startup" "Investisseurs et fondateurs tunisiens" "2027-03-11T11:00:00" "2027-03-11T12:30:00" 80
NewSession "pitch"    $e1 "Pitch des startups" "Dix startups, cinq minutes chacune" "2027-03-11T16:00:00" "2027-03-11T18:00:00" 120
NewSession "cyber"    $e1 "Atelier cybersecurite : proteger son entreprise" "Exercices pratiques de detection d'attaques" "2027-03-12T10:00:00" "2027-03-12T12:00:00" 25

NewSession "fifa"     $e4 "Tournoi FIFA" "Phases finales du tournoi, sur grand ecran" "2027-04-17T14:00:00" "2027-04-17T18:00:00" 64
NewSession "gamedev"  $e4 "Atelier creation de jeu video" "Creer un premier jeu en deux heures" "2027-04-17T10:00:00" "2027-04-17T12:00:00" 25
NewSession "devs"     $e4 "Rencontre avec des studios tunisiens" "Parcours, metiers et recrutement" "2027-04-18T11:00:00" "2027-04-18T12:30:00" 80

NewSession "patisserie" $e2 "Masterclass patisserie tunisienne" "Makroudh, baklawa et kaak warka avec un chef" "2027-05-21T15:00:00" "2027-05-21T17:00:00" 20
NewSession "huile"    $e2 "Degustation d'huiles d'olive" "Reconnaitre les varietes tunisiennes" "2027-05-22T11:00:00" "2027-05-22T12:00:00" 40
NewSession "couscous" $e2 "Concours du meilleur couscous" "Dix chefs, un jury, le public vote" "2027-05-23T12:00:00" "2027-05-23T14:30:00" 150

NewSession "poterie"  $e3 "Atelier poterie de Nabeul" "Faconner et decorer sa propre piece" "2027-07-08T10:00:00" "2027-07-08T12:00:00" 12
NewSession "tissage"  $e3 "Demonstration de tissage du kilim" "Les gestes des tisserandes de Kairouan" "2027-07-09T15:00:00" "2027-07-09T16:00:00" 25
NewSession "numerique" $e3 "Conference : l'artisanat a l'ere du numerique" "Vendre ses creations en ligne" "2027-07-10T11:00:00" "2027-07-10T12:30:00" 100

NewSession "defile"   $e5 "Defile des jeunes createurs" "Collections de fin d'etudes" "2027-09-25T19:00:00" "2027-09-25T21:00:00" 300
NewSession "upcycling" $e5 "Atelier upcycling" "Transformer un vetement ancien en piece unique" "2027-09-24T14:00:00" "2027-09-24T16:00:00" 15
NewSession "durable"  $e5 "Conference : la mode durable" "Matieres, production locale et recyclage" "2027-09-26T11:00:00" "2027-09-26T12:00:00" 120

NewSession "orientation" $e6 "Choisir son ecole d'ingenieurs" "Conseils d'orientation et temoignages" "2026-10-15T10:00:00" "2026-10-15T11:30:00" 200
NewSession "cv"       $e6 "Atelier CV et lettre de motivation" "Relecture personnalisee par des recruteurs" "2026-10-16T14:00:00" "2026-10-16T16:00:00" 30
NewSession "bourses"  $e6 "Bourses et etudes a l'etranger" "Programmes d'echange et financements" "2026-10-17T11:00:00" "2026-10-17T12:00:00" 150

# --- Comptes ---------------------------------------------------------------------
function Register($prenom, $nom, $email, $role, $entreprise) {
    $body = @{ nom = $nom; prenom = $prenom; email = $email; motDePasse = $DemoPassword; role = $role }
    if ($entreprise) { $body["nomEntreprise"] = $entreprise }
    $r = Api POST "/auth/register" $body
    if (-not $r.token) { $r = Api POST "/auth/login" @{ email = $email; motDePasse = $DemoPassword } }
    return $r
}
$expo = @{}
foreach ($x in @(
    @("TechCorp", "Amine", "Gharbi", "amine@techcorp.tn"), @("BioFood", "Lina", "Haddad", "lina@biofood.tn"),
    @("Artisana", "Leila", "Ben Salem", "leila@artisana.tn"), @("Robotix", "Karim", "Mansour", "karim@robotix.tn"),
    @("DesignLab", "Sarra", "Mejri", "sarra@designlab.tn"), @("PixelForge", "Hedi", "Saidi", "hedi@pixelforge.tn"),
    @("ChefMaison", "Rim", "Chaabane", "rim@chefmaison.tn"), @("AtelierBleu", "Yasmine", "Ferchichi", "yasmine@atelierbleu.tn"),
    @("EduNext", "Walid", "Gharsalli", "walid@edunext.tn"))) {
    $expo[$x[0]] = Register $x[1] $x[2] $x[3] "EXPOSANT" $x[0]
}

$visitors = @()
foreach ($v in @(
    @("Sami", "Ben Ali", "sami.benali@mail.tn"), @("Ines", "Trabelsi", "ines.trabelsi@mail.tn"),
    @("Yassine", "Jaziri", "yassine.jaziri@mail.tn"), @("Mariem", "Kchaou", "mariem.kchaou@mail.tn"),
    @("Omar", "Dridi", "omar.dridi@mail.tn"), @("Nour", "Ayari", "nour.ayari@mail.tn"),
    @("Aziz", "Hammami", "aziz.hammami@mail.tn"), @("Salma", "Rekik", "salma.rekik@mail.tn"),
    @("Khalil", "Zouari", "khalil.zouari@mail.tn"), @("Meriem", "Ben Amor", "meriem.benamor@mail.tn"),
    @("Fares", "Jlassi", "fares.jlassi@mail.tn"), @("Lina", "Mabrouk", "lina.mabrouk@mail.tn"))) {
    $visitors += Register $v[0] $v[1] $v[2] "VISITEUR" $null
}

# --- Demandes de stand ---------------------------------------------------------------
function Ask($company, $ev, $num) { return (Api POST "/stand-requests" @{ standId = $stand["$($ev.id)-$num"] } $expo[$company].token).id }
function Decide($id, $decision, $motif) {
    $body = @{ decision = $decision }
    if ($motif) { $body["motif"] = $motif }
    Api PUT "/stand-requests/$id" $body $A | Out-Null
}
# Salon de l'Innovation (utilise par le deroule principal de la demonstration)
$r = Ask "TechCorp" $e1 "A1"; Ask "Robotix" $e1 "A1" | Out-Null; Decide $r "VALIDEE"     # Robotix sur A1 : refusee automatiquement
$r = Ask "BioFood" $e1 "B1"; Decide $r "VALIDEE"
$r = Ask "DesignLab" $e1 "C3"; Decide $r "REFUSEE" "Stand reserve aux artisans de la ceramique."
Ask "Artisana" $e1 "C2" | Out-Null      # en attente : a valider en direct
Ask "Robotix" $e1 "A2" | Out-Null       # en attente : a refuser en direct
Ask "TechCorp" $e1 "A3" | Out-Null      # en attente
# Autres evenements
$r = Ask "PixelForge" $e4 "A1"; Decide $r "VALIDEE"
Ask "PixelForge" $e4 "A2" | Out-Null    # en attente
$r = Ask "Robotix" $e4 "B1"; Decide $r "VALIDEE"
$r = Ask "ChefMaison" $e2 "A1"; Ask "BioFood" $e2 "A1" | Out-Null; Decide $r "VALIDEE"   # BioFood sur A1 : refusee automatiquement
$r = Ask "BioFood" $e2 "A2"; Decide $r "VALIDEE"
$r = Ask "Artisana" $e3 "A1"; Decide $r "VALIDEE"
$r = Ask "AtelierBleu" $e3 "C1"; Decide $r "VALIDEE"
Ask "AtelierBleu" $e5 "A1" | Out-Null   # en attente
$r = Ask "DesignLab" $e5 "A2"; Decide $r "VALIDEE"
$r = Ask "EduNext" $e6 "A1"; Decide $r "VALIDEE"

# --- Billets ----------------------------------------------------------------------
function Buy($visitorIndex, $ev, $type) { return Api POST "/tickets" @{ typeBilletId = $types["$($ev.id)-$type"] } $visitors[$visitorIndex].token }
# Salon de l'Innovation : un billet pour les 6 premiers visiteurs (meme scenario que la version precedente)
$plan = @("VIP", "Standard", "Standard", "Etudiant", "Standard", "VIP")
$tickets = @()
for ($i = 0; $i -lt 6; $i++) { $tickets += Buy $i $e1 $plan[$i] }
# Tunisia Game Fest : les 5 billets "Early bird" sont vendus (type epuise)
foreach ($i in @(3, 4, 6, 8, 9)) { Buy $i $e4 "Early bird" | Out-Null }
foreach ($i in @(10, 11)) { Buy $i $e4 "Pass week-end" | Out-Null }
Buy 7 $e4 "Joueur tournoi" | Out-Null
# Festival des Saveurs
foreach ($i in @(0, 1, 2)) { Buy $i $e2 "Pass 3 jours" | Out-Null }
foreach ($i in @(6, 7)) { Buy $i $e2 "Journee" | Out-Null }
# Expo Artisanat : 11 visiteurs (tous sauf Sami), pour remplir l'atelier poterie a 11 sur 12
foreach ($i in 1..11) { Buy $i $e3 "Entree" | Out-Null }
# Salon Mode et Design
foreach ($i in @(0, 9)) { Buy $i $e5 "Entree" | Out-Null }
Buy 6 $e5 "Pass defile" | Out-Null

# Entrees deja scannees (Salon de l'Innovation)
foreach ($t in $tickets[1..3]) { Api POST "/checkin" @{ code = $t.codeQr } $agent.token | Out-Null }

# --- Inscriptions aux sessions (billet de l'evenement obligatoire) -------------------------
function Join($visitorIndex, $key) { Api POST "/sessions/$($sessions[$key])/registration" $null $visitors[$visitorIndex].token | Out-Null }
$plans = @(
    @("keynote", @(0, 1, 2, 3, 5)), @("robot", @(0, 2, 5)), @("table", @(1, 4, 5)), @("pitch", @(0, 3)), @("cyber", @(1, 4)),
    @("fifa", @(3, 4, 7, 8)), @("gamedev", @(6, 9, 10)), @("devs", @(11, 3)),
    @("patisserie", @(0, 1, 6)), @("huile", @(0, 2, 7)), @("couscous", @(0, 1, 2, 6, 7)),
    @("poterie", @(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)), @("tissage", @(1, 5, 8)), @("numerique", @(10, 11, 2)),
    @("defile", @(0, 9, 6)), @("upcycling", @(6)), @("durable", @(0, 9)))
foreach ($p in $plans) { foreach ($i in $p[1]) { Join $i $p[0] } }

# --- Resume -------------------------------------------------------------------------
$summary = @"
EventOra - comptes de demonstration
====================================
Administrateur   admin@salon.tn          mot de passe : votre SEED_PASSWORD
Agent d'accueil  agent@salon.tn          mot de passe : votre SEED_PASSWORD
Visiteur         sami.benali@mail.tn     mot de passe : $DemoPassword   (billet VIP Salon de l'Innovation, 3 sessions)
Exposant         amine@techcorp.tn       mot de passe : $DemoPassword   (stand A1 reserve, A3 en attente)

Visiteurs : ines.trabelsi, yassine.jaziri, mariem.kchaou, omar.dridi, nour.ayari, aziz.hammami,
            salma.rekik, khalil.zouari, meriem.benamor, fares.jlassi, lina.mabrouk (@mail.tn)
Exposants : lina@biofood.tn, leila@artisana.tn, karim@robotix.tn, sarra@designlab.tn,
            hedi@pixelforge.tn, rim@chefmaison.tn, yasmine@atelierbleu.tn, walid@edunext.tn
Tous les comptes de demonstration : $DemoPassword

6 evenements : Salon de l'Innovation, Tunisia Game Fest, Festival des Saveurs, Expo Artisanat,
               Salon Mode et Design, Salon de l'Etudiant 2026 (termine)
Demandes a traiter en direct : Artisana -> C2 (valider), Robotix -> A2 (refuser avec un motif)
Atelier poterie de Nabeul (Expo Artisanat) : 11/12 inscrits, une place a prendre en direct
Billet Early bird (Tunisia Game Fest) : epuise
Categorie "Divers" : aucun stand, suppression autorisee
Billet non scanne (secours pour le scan) : $($tickets[4].codeQr)  (Omar Dridi, Standard)
Billet deja scanne (pour montrer le refus) : $($tickets[1].codeQr)  (Ines Trabelsi)
"@
Write-Host ""
Write-Host $summary -ForegroundColor Green
$summary | Out-File -FilePath (Join-Path $PSScriptRoot "demo-comptes.txt") -Encoding utf8
Write-Host "Resume enregistre dans demo-comptes.txt" -ForegroundColor Cyan
