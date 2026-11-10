# Gestion de Salon / Festival

Application web de gestion d'un salon ou d'un festival : l'organisateur gère les événements, les stands et le programme ; les exposants demandent des stands ; les visiteurs réservent un billet et s'inscrivent aux sessions ; un agent d'accueil valide les billets à l'entrée.

Projet de cours réalisé à l'ENICarthage (Projet Full Stack Web).

## Sommaire

1. [Technologies](#1-technologies)
2. [État d'avancement](#2-état-davancement)
3. [Installation et lancement](#3-installation-et-lancement)
4. [Structure du projet](#4-structure-du-projet)
5. [Architecture du backend](#5-architecture-du-backend)
6. [Base de données](#6-base-de-données)
7. [Package `model`](#7-package-model)
8. [Package `dao`](#8-package-dao)
9. [Package `service` et règles métier](#9-package-service-et-règles-métier)
10. [Fonctionnalités](#10-fonctionnalités)
11. [Tests réalisés](#11-tests-réalisés)
12. [Points d'attention et limites connues](#12-points-dattention-et-limites-connues)
13. [Travail restant](#13-travail-restant)
14. [Travailler à deux avec Git](#14-travailler-à-deux-avec-git)

---

## 1. Technologies

| Partie | Technologie |
|---|---|
| Frontend | React (pas encore commencé) |
| Backend | Java 17, **sans Spring Boot** (consigne du cours) |
| Base de données | MySQL (via XAMPP, qui fournit en réalité MariaDB) |
| Communication Java ↔ MySQL | JDBC (driver `mysql-connector-j` 9.4.0) |
| Communication React ↔ Java | HTTP / JSON (à mettre en place) |
| Gestion du projet | Maven |
| IDE | IntelliJ IDEA |
| Test des routes HTTP (plus tard) | Postman |

## 2. État d'avancement

| Étape | État |
|---|---|
| Liste des fonctionnalités, diagramme de cas d'utilisation, diagramme de classes | Fait, **validé par le professeur** |
| Projet Maven et arborescence des packages | Fait |
| Base de données MySQL : tables, relations, données de test, requêtes | Fait |
| Connexion JDBC | Fait |
| Package `model` (classes et énumérations) | Fait |
| Package `dao` (accès aux données, CRUD) | Fait pour les entités principales |
| Package `service` (règles métier) | Fait pour les entités principales |
| Hachage des mots de passe, inscription, connexion | Fait |
| Packages `controller`, `router`, `dto` | **Vides, à faire** |
| Serveur HTTP et routes (test avec Postman) | **À faire** |
| Frontend React | **À faire** |

Les tests réalisés, ceux qui restent à rejouer et la façon de tester sont détaillés dans la [section 11](#11-tests-réalisés).

## 3. Installation et lancement

### Prérequis

- **JDK 17** (le `pom.xml` est configuré pour Java 17)
- **IntelliJ IDEA**
- **XAMPP** (MySQL et phpMyAdmin)

### Étapes

1. **Cloner le dépôt** et l'ouvrir dans IntelliJ (**File → Open**, choisir le dossier du projet). IntelliJ reconnaît le `pom.xml` et télécharge le driver MySQL. Si ce n'est pas automatique, clic droit sur `pom.xml` → **Maven → Reload project**.
2. **Démarrer MySQL** : ouvrir le *XAMPP Control Panel* et cliquer sur **Start** à côté de **MySQL** (et d'**Apache** pour phpMyAdmin).
3. **Créer la base de données** : ouvrir `http://localhost/phpmyadmin`, onglet **Importer** (au niveau du serveur, sans sélectionner de base), choisir le fichier `database.sql`, puis **Importer**. La base `gestion_salon` est créée avec ses 10 tables et des données de test.
4. **Vérifier la configuration** dans `config/DatabaseConnection.java` : hôte `localhost`, port `3306`, base `gestion_salon`, utilisateur `root`, **mot de passe vide** (valeurs par défaut de XAMPP). Adapter si ta configuration est différente.
5. **Lancer un test** : exécuter `main/Main.java` (flèche verte). Si MySQL n'est pas démarré, la console affiche `Connection refused`.

> **Attention** : le script `database.sql` commence par `DROP DATABASE IF EXISTS gestion_salon`. Chaque import (ou chaque exécution de `DatabaseConnection.main`) **supprime la base et la recrée** avec les données de départ.

## 4. Structure du projet

Arborescence actuelle :

```
gestion-salon/
├── pom.xml
├── src/
│   └── main/
│       ├── java/tn/enicarthage/gestionsalon/
│       │   ├── config/        DatabaseConnection
│       │   ├── model/         entités et énumérations
│       │   ├── dao/           accès à MySQL (JDBC)
│       │   ├── service/       règles métier
│       │   ├── controller/    (vide, à faire)
│       │   ├── dto/           (vide)
│       │   ├── exception/     exceptions personnalisées
│       │   ├── util/          PasswordUtil
│       │   ├── router/        (vide, à faire)
│       │   └── main/          Main (programme de test)
│       └── resources/
│           └── database/
│               └── database.sql
└── README.md
```

> **À confirmer avec le professeur** : le guide du cours place `pom.xml` et `src` dans un dossier `backend/`, et `database.sql` dans un dossier `database/` à la racine, à côté d'un dossier `frontend/`. Ce n'est pas encore le cas. Quand la structure sera modifiée, mettre à jour ce schéma et le chemin lu par `DatabaseConnection.main` (`src/main/resources/database.sql`).

Package de base : `gestionSalon`.

## 5. Architecture du backend

Le backend suit une architecture en couches. Une donnée traverse les couches dans cet ordre :

```
React  →  Controller  →  Service  →  DAO  →  JDBC  →  MySQL
```

| Package | Rôle |
|---|---|
| `model` | Classes qui représentent les entités (`Stand`, `Billet`...). Seulement des attributs, constructeurs, getters et setters. |
| `dao` | *Data Access Object* : seule couche qui parle à MySQL. Exécute les requêtes SQL avec `PreparedStatement`. |
| `service` | Logique métier : applique les règles (« un prix ne peut pas être négatif ») avant d'appeler le DAO. |
| `controller` | Recevra les requêtes HTTP de React et appellera les services. *À faire.* |
| `router` | Associera une URL et une méthode HTTP au bon contrôleur. *À faire.* |
| `dto` | Objets qui transportent les données entre les couches (requête reçue, réponse envoyée). *À faire.* |
| `config` | Configuration technique (`DatabaseConnection`). |
| `exception` | Exceptions personnalisées. |
| `util` | Classes utilitaires réutilisables (`PasswordUtil`). |
| `main` | Point de démarrage (`Main`). Pour l'instant, sert de programme de test. |

**Règle à respecter** : un service ne contient jamais de SQL, et un DAO ne contient jamais de règle métier.

### Exceptions

| Exception | Signification | Code HTTP prévu |
|---|---|---|
| `IllegalArgumentException` (Java) | Donnée invalide ou règle métier non respectée | 400 |
| `AuthenticationException` | Identifiants incorrects, compte bloqué | 401 |
| `ResourceNotFoundException` | Élément introuvable | 404 |

Ces correspondances seront appliquées dans les contrôleurs.

## 6. Base de données

Base : `gestion_salon`. Script : `database.sql` (création des tables, données de test, requêtes de vérification en commentaires à la fin du fichier).

### Modèle relationnel

Notation : `#` = clé primaire, `→` = clé étrangère.

```
utilisateur(#id, nom, prenom, email, mot_de_passe_hash, telephone, role, est_bloque)
exposant(#id → utilisateur.id, nom_entreprise, logo_url, description)
agent_accueil(#id → utilisateur.id, matricule)

categorie(#id, nom, description)
evenement(#id, titre, description, date_debut, date_fin, lieu)

session_evenement(#id, evenement_id → evenement.id, titre, description,
                  heure_debut, heure_fin, capacite_max)

stand(#id, evenement_id → evenement.id, categorie_id → categorie.id,
      numero, emplacement, superficie, prix, statut)
      contrainte : (evenement_id, numero) unique

inscription_session(#visiteur_id → utilisateur.id, #session_id → session_evenement.id)

demande_stand(#id, exposant_id → exposant.id, stand_id → stand.id,
              admin_id → utilisateur.id (peut être NULL),
              date_demande, statut, motif_refus)

billet(#id, visiteur_id → utilisateur.id, evenement_id → evenement.id,
       agent_id → agent_accueil.id (peut être NULL),
       code_qr, date_achat, statut)
```

### Choix de conception

- **Héritage d'`Utilisateur`** : la table `utilisateur` contient les champs communs et la colonne `role` (`ADMINISTRATEUR`, `EXPOSANT`, `VISITEUR`, `AGENT_ACCUEIL`). Seules les sous-classes qui ont des attributs en plus ont leur table : `exposant` et `agent_accueil`. Leur clé primaire est aussi une clé étrangère vers `utilisateur.id` (relation 1:1). `Administrateur` et `Visiteur` n'ont pas de table : la colonne `role` suffit.
- **Table `session_evenement`** (et non `session`) : pour éviter le mot réservé SQL `SESSION`.
- **Relation N:N** Visiteur ↔ Session : table intermédiaire `inscription_session`, sans classe Java associée.
- **`ON DELETE CASCADE`** : supprimer un événement supprime ses stands, sessions et billets ; supprimer un stand supprime ses demandes ; supprimer un utilisateur supprime sa ligne `exposant` ou `agent_accueil`.
- **`ON DELETE SET NULL`** : supprimer un administrateur ou un agent efface seulement le lien dans `demande_stand` et `billet`.
- Les colonnes `statut` et `role` sont des `ENUM` MySQL, dont les valeurs sont **identiques** aux énumérations Java.

### Valeurs des énumérations

| Énumération | Valeurs |
|---|---|
| `Role` | `ADMINISTRATEUR`, `EXPOSANT`, `VISITEUR`, `AGENT_ACCUEIL` |
| `StatutStand` | `LIBRE`, `EN_ATTENTE`, `RESERVE` |
| `StatutDemande` | `EN_ATTENTE`, `VALIDEE`, `REFUSEE` |
| `StatutBillet` | `VALIDE`, `UTILISE`, `ANNULE` |

### Données de test (après un import propre)

| id | Utilisateur | Rôle |
|---|---|---|
| 1 | admin@salon.tn | Administrateur |
| 2, 3, 4 | TechPlus, Artisanat Tunisien, Bio Delices | Exposants |
| 5, 6 | Amine Jlassi, Sarra Ben Ali | Visiteurs |
| 7 | Youssef Hammami (matricule `AG-001`) | Agent d'accueil |

Ces comptes ont un mot de passe **provisoire** (`hash_provisoire_N`) : **ils ne peuvent pas se connecter**. Pour obtenir un compte utilisable, il faut passer par `AuthService` (inscription) ou remplacer ces valeurs par de vrais hachages.

Il y a aussi 2 événements, 3 catégories, 5 stands, 3 sessions, 3 demandes de stand et 3 billets.

## 7. Package `model`

Chaque classe correspond à une table, et chaque attribut à une colonne (`categorie_id` devient `categorieId`).

| Classe | Table | Remarque |
|---|---|---|
| `Utilisateur` (abstraite) | `utilisateur` | Parent des 4 classes ci-dessous |
| `Administrateur` | `utilisateur` | Rôle `ADMINISTRATEUR` fixé par le constructeur |
| `Visiteur` | `utilisateur` | Rôle `VISITEUR` |
| `Exposant` | `utilisateur` + `exposant` | Ajoute `nomEntreprise`, `logoUrl`, `description` |
| `AgentAccueil` | `utilisateur` + `agent_accueil` | Ajoute `matricule` |
| `Categorie`, `Evenement` | `categorie`, `evenement` | |
| `Session` | `session_evenement` | |
| `Stand`, `DemandeStand`, `Billet` | `stand`, `demande_stand`, `billet` | |

### Conventions

- **Les relations sont des identifiants, pas des objets** : `Stand` contient `evenementId`, pas un objet `Evenement` (choix du guide du cours, proche de la clé étrangère). Pour afficher le nom de l'événement avec un stand, on fera un `JOIN` et un DTO.
- **`Integer` au lieu de `int`** pour `adminId` (dans `DemandeStand`) et `agentId` (dans `Billet`) : ces colonnes peuvent valoir `NULL`.
- **Types** : `BigDecimal` pour les prix (type SQL `DECIMAL`), `LocalDate` pour une date, `LocalDateTime` pour une date avec heure.
- **Les méthodes du diagramme de classes** (`sAuthentifier()`, `reserverBillet()`...) ne sont **pas** dans les classes du `model` : elles sont dans les services.

## 8. Package `dao`

Tous les DAO utilisent `PreparedStatement` (protection contre l'injection SQL) et `try-with-resources` (fermeture automatique de la connexion). Une méthode privée `mapRow` convertit une ligne MySQL en objet Java.

| DAO | Méthodes principales |
|---|---|
| `CategorieDAO` | `create`, `findAll`, `findById`, `update`, `delete` |
| `EvenementDAO` | `create`, `findAll`, `findById`, `update`, `delete` |
| `StandDAO` | CRUD + `findByEvenementId` |
| `DemandeStandDAO` | `create`, `findAll`, `findById`, `findByExposantId`, `findByStandId`, `update`, `delete` |
| `BilletDAO` | `create`, `findById`, `findByCodeQr`, `findByVisiteurId`, `update`, `delete` |
| `SessionDAO` | CRUD + `findByEvenementId`, `findByVisiteurId` (avec un `JOIN`) |
| `InscriptionSessionDAO` | `add`, `remove`, `exists`, `countBySession` |
| `UtilisateurDAO` | `findRole`, `existsByEmail`, `findByEmail`, `create`, `createExposant`, `setBloque`, `delete` |
| `ExposantDAO` | `exists` (version minimale) |

Points techniques à connaître :

- **Énumérations** : on écrit avec `statut.name()` et on lit avec `StatutStand.valueOf(result.getString("statut"))`.
- **Dates** : `java.sql.Date.valueOf(localDate)` pour écrire un `LocalDate` ; `getObject(colonne, LocalDateTime.class)` pour lire une date avec heure.
- **`create` remplit l'`id`** de l'objet grâce à `RETURN_GENERATED_KEYS`.
- **`UtilisateurDAO.createExposant`** utilise une **transaction** : les deux `INSERT` (`utilisateur` et `exposant`) sont enregistrés ensemble ou pas du tout.

## 9. Package `service` et règles métier

| Service | Rôle |
|---|---|
| `CategorieService`, `EvenementService`, `StandService` | CRUD avec validation |
| `SessionService` | Sessions et inscriptions des visiteurs |
| `DemandeStandService` | Demandes de stand, validation, refus |
| `BilletService` | Réservation, scan et annulation des billets |
| `AuthService` | Inscription (visiteur, exposant) et connexion |

### Règles métier implémentées

**Événements**
- Titre, lieu et dates obligatoires.
- La date de fin ne peut pas précéder la date de début.

**Stands**
- Numéro obligatoire, superficie positive, prix positif ou nul.
- L'événement et la catégorie du stand doivent exister.
- Le numéro est unique dans un événement.
- Un nouveau stand est `LIBRE` par défaut.

**Demandes de stand** (règle centrale du projet)
- *Création* : l'exposant et le stand doivent exister, le stand ne doit pas être `RESERVE`, un exposant ne peut pas avoir deux demandes en attente sur le même stand. Le stand passe de `LIBRE` à `EN_ATTENTE`.
- *Validation* (par un administrateur) : la demande doit être `EN_ATTENTE`. Elle devient `VALIDEE`, le stand devient `RESERVE`, et les autres demandes en attente sur ce stand sont **refusées automatiquement**.
- *Refus* : motif obligatoire. Si plus aucune demande n'est en attente, le stand redevient `LIBRE`.
- Plusieurs exposants peuvent demander le même stand tant qu'il n'est pas réservé.

**Billets**
- *Réservation* : l'utilisateur doit être un `VISITEUR`, l'événement doit exister et ne pas être terminé, un visiteur ne peut pas avoir deux billets non annulés pour le même événement. Le code QR est généré automatiquement (`QR-` suivi d'un UUID).
- *Scan* : réservé à un `AGENT_ACCUEIL`. Le billet doit être `VALIDE` ; il devient `UTILISE` et l'agent est enregistré.
- *Annulation* : impossible si le billet est déjà utilisé ou annulé.

**Sessions**
- Titre obligatoire, capacité positive, heure de fin après l'heure de début.
- La session doit avoir lieu **pendant les dates de l'événement**.
- *Inscription* : seul un `VISITEUR` peut s'inscrire, pas deux fois à la même session, et pas si la session est **complète**.

**Authentification**
- Inscription : nom et prénom obligatoires, email valide et unique (enregistré en minuscules), mot de passe d'au moins 8 caractères.
- Les mots de passe sont **hachés** avec PBKDF2 (`util/PasswordUtil`), avec un sel aléatoire. Format stocké : `iterations:sel:empreinte`.
- Connexion : **même message** pour un email inconnu et un mauvais mot de passe (`Email ou mot de passe incorrect.`). Un compte bloqué est refusé.

## 10. Fonctionnalités

Les fonctionnalités viennent du diagramme de cas d'utilisation validé par le professeur. Les acteurs sont l'**Administrateur**, l'**Exposant**, le **Visiteur** et l'**Agent d'accueil**.

Légende : **Service** = la logique est écrite et testée ; **À faire** = rien d'écrit ; **Partiel** = une partie seulement. Aucune fonctionnalité n'est encore accessible en HTTP.

| Fonctionnalité | Acteur | État | Où |
|---|---|---|---|
| S'inscrire | Visiteur, Exposant | Service | `AuthService` |
| S'authentifier | Tous | Service (pas encore de session ni de jeton) | `AuthService.seConnecter` |
| Gérer son profil | Tous | À faire | — |
| Gérer les utilisateurs (consulter, bloquer, supprimer) | Administrateur | Partiel : blocage et suppression au niveau DAO, pas de liste ni de service | `UtilisateurDAO` |
| Gérer les catégories | Administrateur | Service | `CategorieService` |
| Gérer les stands | Administrateur | Service | `StandService` |
| Consulter le plan des stands | Tous | Partiel : liste des stands d'un événement, pas de plan visuel | `StandService.getStandsByEvenement` |
| Demander la réservation d'un stand | Exposant | Service | `DemandeStandService.createDemande` |
| Consulter l'état de ses demandes | Exposant | Service | `DemandeStandService.getDemandesByExposant` |
| Traiter la demande de stand (valider, refuser) | Administrateur | Service | `DemandeStandService` |
| Gérer les événements | Administrateur | Service | `EvenementService` |
| Gérer les sessions | Administrateur | Service | `SessionService` |
| Consulter le programme | Tous | Service | `SessionService.getSessionsByEvenement` |
| S'inscrire à une session | Visiteur | Service | `SessionService.inscrireVisiteur` |
| Réserver / obtenir un billet | Visiteur | Service (test à rejouer) | `BilletService.reserverBillet` |
| Générer le PDF de confirmation du billet | Visiteur | À faire | — |
| Scanner et valider un QR code | Agent d'accueil | Service (test à rejouer) | `BilletService.scannerBillet` |
| Consulter le tableau de bord | Administrateur | À faire | — |
| Gérer les types de billets | Administrateur | À faire, **à clarifier** | — |

> **À clarifier avec le professeur** : le diagramme de cas d'utilisation contient « Gérer les types de billets », mais le diagramme de classes n'a pas de classe `TypeBillet`, et la base n'a pas de table correspondante.

## 11. Tests réalisés

Il n'y a pas encore de serveur HTTP. Les tests sont de deux types :

- **tests SQL**, exécutés à la main dans phpMyAdmin ;
- **tests Java**, écrits dans `main/Main.java` et exécutés avec la flèche verte d'IntelliJ.

### 11.1 Comment tester

`Main.java` contient le **dernier scénario de test** (inscription et connexion). On remplace son contenu par un autre scénario pour tester une autre fonctionnalité. Le modèle utilisé :

```java
// Exécute une action et affiche le refus si une règle métier l'interdit
private static void essayer(Runnable action) {
    try {
        action.run();
    } catch (RuntimeException e) {
        System.out.println("Refuse : " + e.getMessage());
    }
}
```

Principes suivis :

- Chaque test **crée ses propres données** (un événement de test, des emails uniques) et les **supprime à la fin** : on peut relancer sans nettoyer la base.
- On teste les **cas qui doivent échouer** (`Refuse : ...`) autant que le cas qui doit réussir.
- MySQL doit être **démarré** avant chaque exécution.
- Si un test s'arrête avant son nettoyage, supprimer les lignes de test dans phpMyAdmin ou réimporter `database.sql`.

### 11.2 Tests SQL (phpMyAdmin)

| Test | Ce qu'il vérifie | Résultat |
|---|---|---|
| Import de `database.sql` | Les 10 tables sont créées avec leurs clés et contraintes, et les données de départ sont insérées | 10 tables, 34 lignes au total |
| `SELECT` et `WHERE` | Lecture simple ; stands libres d'un événement | Résultats attendus (`A02` et `B02` pour l'événement 1) |
| `JOIN` | Stands avec événement et catégorie ; demandes avec nom de l'exposant ; sessions d'un visiteur (table N:N) ; places restantes par session (`LEFT JOIN` et `GROUP BY`) | Résultats attendus |
| `INSERT` | Nouveau stand `C02`, puis demande sur ce stand | Une relance du même `INSERT` a produit l'erreur `#1062 Duplicate entry '2-C02'` : **la contrainte d'unicité (événement, numéro) fonctionne** |
| `UPDATE` | Validation d'une demande (statut de la demande et du stand) ; scan d'un billet (statut et agent) | Statuts mis à jour |
| `DELETE` | Annulation d'une inscription ; suppression d'un stand | La demande liée au stand supprimé a disparu : **`ON DELETE CASCADE` fonctionne** |

Les requêtes sont rassemblées en commentaires à la fin de `database.sql`.

### 11.3 Tests Java réussis

| # | Scénario | Ce qu'il vérifie | Résultat obtenu |
|---|---|---|---|
| 1 | `DatabaseConnection.getConnection()` | Java communique avec MySQL | `Connexion reussie a MySQL.` |
| 2 | `CategorieDAO` : create, findAll, update, findById, delete | Le CRUD complet en JDBC ; l'`id` généré est bien rempli dans l'objet | Toutes les étapes conformes ; après suppression, `findById` renvoie `null` |
| 3 | `StandDAO` : create, findByEvenementId, update, delete | Conversion des énumérations (`StatutStand`) et des prix (`BigDecimal`) dans les deux sens | Liste des stands avec statuts et prix corrects ; statut modifié en `RESERVE` ; `null` après suppression |
| 4 | `StandService` : règles de validation | Prix négatif refusé ; catégorie inexistante refusée ; stand créé avec le statut `LIBRE` par défaut ; stand introuvable signalé | 5 lignes conformes |
| 5 | `EvenementService` et `StandService` | Date de fin avant la date de début refusée ; stand sur un événement inexistant refusé ; numéro de stand déjà pris refusé ; suppression de l'événement avec ses stands (`CASCADE`) | 5 lignes conformes |
| 6 | `DemandeStandService` : scénario complet | Deux exposants demandent le même stand (statuts `EN_ATTENTE`) ; doublon refusé ; exposant inconnu refusé ; **validation de la demande A : A = `VALIDEE`, B = `REFUSEE` automatiquement, stand = `RESERVE`** ; revalidation refusée ; demande sur un stand réservé refusée | 9 lignes conformes |
| 7 | Lecture d'une date avec heure | La demande 1 des données de départ est lue en `2026-10-01T09:30`, **comme dans phpMyAdmin** | Aucun décalage de fuseau horaire |
| 8 | `SessionService` : sessions et inscriptions | Session refusée si fin avant début, hors des dates de l'événement ou capacité nulle ; inscription ; doublon refusé ; **session complète refusée** ; exposant refusé ; désinscription qui libère la place ; liste des sessions d'un visiteur (`JOIN`) | 17 lignes conformes |
| 9 | `AuthService` : inscription et connexion | Mot de passe **haché** ; email déjà utilisé, invalide, mot de passe trop court, nom vide : refusés ; exposant enregistré dans **deux tables** (transaction) ; connexion acceptée même avec l'email en majuscules ; **même message** pour un mauvais mot de passe, un email inconnu et un compte de départ ; compte bloqué refusé, puis débloqué accepté | 14 lignes conformes |

### 11.4 Un test d'erreur utile : MySQL arrêté

Un test a été lancé alors que MySQL n'était pas démarré. Résultat : des erreurs `Communications link failure` et `Connection refused`, suivies de lignes trompeuses (`Cree, id = 0`, `Evenement introuvable : 1`).

Ce n'est pas un bug de la règle métier : les DAO attrapent l'erreur SQL et la laissent passer, donc le service croit que tout s'est bien passé. C'est la limite décrite dans la section 12. **Avant de lancer un test, toujours vérifier que MySQL est démarré dans XAMPP.**

### 11.5 Ce qui n'est pas encore vérifié

À rejouer avant de considérer ces parties comme terminées :

| Élément | Remarque |
|---|---|
| `BilletService` (réservation, scan, annulation) | Le scénario de test est écrit, mais son résultat n'a pas été contrôlé |
| `EvenementDAO` : `findAll` et `update` | Seuls `create`, `findById` et `delete` ont servi dans les tests 5 et suivants |
| `DatabaseConnection.main` (création de la base depuis le script) | Jamais exécuté |
| `DemandeStandService.refuserDemande` | Le refus automatique (test 6) est vérifié, mais pas le refus manuel avec motif, ni le retour du stand à `LIBRE` |
| `CategorieService`, `annulerBillet` (cas réussi), `getDemandesByExposant` | Non testés |
| Contrôleurs, routes HTTP, frontend | Pas encore écrits |

## 12. Points d'attention et limites connues

- **Erreurs SQL silencieuses** : les DAO attrapent les `SQLException` avec `printStackTrace()` (comme dans le guide du cours). Le service ne sait donc pas qu'une requête a échoué. Exemple : si MySQL est arrêté, `create` affiche une erreur mais le service croit avoir réussi (`id = 0`). À améliorer avant les contrôleurs, par exemple en relançant une exception personnalisée.
- **Transactions** : seule `createExposant` utilise une transaction. `DemandeStandService.validerDemande` enchaîne plusieurs `UPDATE` sur des connexions séparées : si l'un échoue au milieu, les statuts peuvent devenir incohérents.
- **`DatabaseConnection.main` réinitialise toute la base** (voir la section 3).
- **Comptes de test** : mots de passe provisoires, connexion impossible (voir la section 6).
- **Règles volontairement absentes**, à poser au professeur :
  - exiger un billet pour l'événement avant de s'inscrire à une session ;
  - vérifier la date de l'événement au moment du scan d'un billet.
- **Fuseau horaire** : l'URL de connexion contient `serverTimezone=UTC` (modèle du professeur). Les dates avec heure ont été vérifiées : l'heure écrite par Java est celle lue dans phpMyAdmin.
- **Mot de passe de la base** : il est vide dans le code. Ne jamais publier un vrai mot de passe sur GitHub.

## 13. Travail restant

Dans l'ordre conseillé :

1. **Réorganiser l'arborescence** avec le professeur (`backend/`, `database/`, `frontend/`).
2. **Contrôleurs** : un par entité (`StandController`, `DemandeStandController`, `BilletController`, `SessionController`, `AuthController`...), branchés sur les services.
3. **Serveur HTTP, routeur et JSON** : démarrer le serveur depuis `Main`, associer les URL aux contrôleurs, convertir les objets en JSON, traduire les exceptions en codes HTTP (400, 401, 404). Tester avec Postman.
4. **DTO** : séparer les données reçues de React, les modèles et les données renvoyées (par exemple une demande de stand avec le nom de l'exposant et le numéro du stand).
5. **Services manquants** : gestion des utilisateurs (liste, blocage), modification du profil, tableau de bord (statistiques), consultation du plan.
6. **Authentification dans l'application** : session ou jeton (JWT) pour savoir qui est connecté, et contrôle des rôles (par exemple seul un administrateur valide une demande).
7. **Comptes de test utilisables** : remplacer les mots de passe provisoires par de vrais hachages.
8. **PDF de confirmation** du billet.
9. **Frontend React**.
10. Rapport : joindre le modèle relationnel (section 6).

### Routes prévues (proposition, à adapter)

| Méthode | URL | Opération |
|---|---|---|
| POST | `/auth/register` | Inscription |
| POST | `/auth/login` | Connexion |
| GET | `/events` | Liste des événements |
| GET | `/events/{id}/stands` | Stands d'un événement |
| GET | `/events/{id}/sessions` | Programme d'un événement |
| POST | `/stands` | Créer un stand (administrateur) |
| POST | `/stand-requests` | Demander un stand (exposant) |
| PUT | `/stand-requests/{id}/validate` | Valider une demande (administrateur) |
| PUT | `/stand-requests/{id}/reject` | Refuser une demande (administrateur) |
| POST | `/tickets` | Réserver un billet (visiteur) |
| PUT | `/tickets/scan` | Scanner un billet (agent d'accueil) |
| POST | `/sessions/{id}/registrations` | S'inscrire à une session |

## 14. Travailler à deux avec Git

Le dépôt est sur GitHub. Le deuxième membre doit être ajouté dans **Settings → Collaborators** du dépôt.

Habitudes à prendre :

- **Récupérer avant de travailler** : **Git → Pull** (Ctrl + T) pour avoir la dernière version.
- **Un commit par fonctionnalité**, avec un message qui dit ce qui a changé, par exemple `Ajout des sessions et des inscriptions des visiteurs`.
- **Envoyer souvent** : Ctrl + K (commit), puis Ctrl + Shift + K (push).
- **Ne pas modifier le même fichier en même temps** : se répartir les packages ou les fonctionnalités (par exemple l'un les contrôleurs, l'autre les services manquants).
- **Ne jamais envoyer** le dossier `target/` : il est dans le `.gitignore`.
- En cas de **conflit** au moment d'un pull, IntelliJ ouvre un outil de fusion : lire les deux versions et garder les deux changements utiles.
- Si on modifie `database.sql`, **prévenir l'autre** : il doit réimporter le fichier pour que sa base corresponde.