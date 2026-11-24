# Gestion de Salon / Festival

Application web de gestion d'un salon : l'administrateur gère événements, stands et programme ; les exposants demandent des stands ; les visiteurs réservent un billet et s'inscrivent aux sessions ; un agent d'accueil valide les billets.

Projet de cours ENICarthage (Projet Full Stack Web).

## Technologies

| Partie | Technologie |
|---|---|
| Frontend | React (Vite) |
| Backend | Java 17, sans Spring Boot, serveur `com.sun.net.httpserver` |
| Base de données | MySQL (XAMPP), JDBC (mysql-connector-j) |
| JSON | Gson |
| Authentification | JWT (HMAC-SHA256), mots de passe hachés PBKDF2 |
| Build | Maven |

## Avancement

| Étape | État |
|---|---|
| Conception (cas d'utilisation, classes) | Fait |
| Base de données (11 tables, contraintes CHECK, clés étrangères) | Fait |
| Packages model, dao, service, controller, dto | Fait |
| Transactions et protection contre la concurrence (verrous `FOR UPDATE`) | Fait |
| Serveur HTTP, Router, CORS, codes 400/401/403/404/409/500 | Fait |
| Routes du backend (BF-01 à BF-34, voir ci-dessous) | Fait |
| PDF du billet avec code QR (BF-32) | Fait |
| Tableau de bord (BF-34) | Fait |
| Tests automatisés (JUnit) | À faire |
| Front : pages exposant, visiteur, agent, admin | À faire |

## Installation

1. JDK 17, IntelliJ, XAMPP (démarrer MySQL).
2. Créer la base : importer `src/main/resources/database/database.sql` dans phpMyAdmin,
   ou lancer `DatabaseConnection.main`. **Attention : le script supprime et recrée la base `gestion_salon`.**
3. Créer l'administrateur et l'agent d'accueil : définir `SEED_PASSWORD` (8 caractères min.)
   puis lancer `main/SeedDev` (`admin@salon.tn`, `agent@salon.tn`).
4. Lancer `Main` (ou `mvn exec:java`) : serveur sur `http://localhost:8080`.
5. Frontend :
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Ouvrir `http://localhost:5173`.

Variables d'environnement :
- `JWT_SECRET` : secret de signature des jetons, 32 caractères minimum conseillés. Aucun secret n'est écrit
  dans le code : sans cette variable, un secret aléatoire est généré au démarrage (avertissement dans la console)
  et les jetons deviennent invalides à chaque redémarrage du serveur.
- `DB_NAME` : nom de la base (défaut `gestion_salon`). Utile pour lancer les tests sur une base séparée,
  par exemple `DB_NAME=gestion_salon_test`.
- `PORT` : port du serveur (défaut `8080`). Exemple : `PORT=8081` sur un PC où Oracle occupe déjà le 8080.
- `RESEND_API_KEY` : clé de l'API [Resend](https://resend.com). **C'est l'interrupteur des fonctions par email** :
  - **sans clé** (par défaut) : pas de vérification d'email, l'inscription connecte directement et
    « mot de passe oublié » est désactivé (`GET /auth/config` renvoie `{"emailActif": false}`) ;
  - **avec une clé** : code à 6 chiffres envoyé par email à l'inscription, et mot de passe oublié disponible.
  Attention : sans domaine vérifié dans Resend, les emails ne partent que vers l'adresse du compte Resend.
- `RESEND_FROM` : expéditeur des emails (défaut `EventOra <onboarding@resend.dev>`). Avec l'adresse de test
  `onboarding@resend.dev`, Resend n'envoie qu'à l'adresse email du compte Resend ; pour écrire à n'importe qui,
  il faut vérifier un domaine dans Resend puis mettre par exemple `EventOra <noreply@votre-domaine.tn>`.

Dans IntelliJ, ces variables se règlent dans Run → Edit Configurations → Main → Environment variables,
par exemple `RESEND_API_KEY=re_xxx;PORT=8080`.

**Base existante** : pour ajouter la vérification par email sans perdre les données, exécuter
`src/main/resources/database/migration_email.sql` une seule fois (les comptes existants sont considérés comme vérifiés).

## Architecture

```
React -> Router -> Controller -> Service -> DAO -> JDBC -> MySQL
```

Un service ne contient pas de SQL, un DAO ne contient pas de règle métier. Les opérations multi-écritures passent par `Transaction.run(...)` avec une seule connexion.

## Authentification

- `POST /auth/register` (VISITEUR ou EXPOSANT), `POST /auth/login` : renvoient `{ token, user }`.
- Les routes protégées demandent `Authorization: Bearer <token>`.
- Les identifiants (admin, exposant, visiteur) viennent du jeton, jamais du corps JSON.
- Créer un admin : s'inscrire comme visiteur, puis
  `UPDATE utilisateur SET role = 'ADMINISTRATEUR' WHERE email = '...';`

## Routes

Les identifiants de l'utilisateur (visiteur, exposant, admin, agent) viennent toujours du jeton, jamais du corps JSON.

Erreurs : toujours `{"erreur": "..."}`. Codes : 400 (données invalides), 401 (non authentifié), 403 (rôle interdit),
404 (route ou ressource inconnue), 405 (route connue, méthode non supportée : en-tête `Allow`), 409 (conflit), 500.

**Comptes et profil**

Inscription et mot de passe oublié par code à 6 chiffres envoyé par email (valable 15 minutes, 5 essais,
60 secondes entre deux envois, stocké haché). « Renvoyer le code » et « mot de passe oublié » répondent
toujours 204, que l'email existe ou non (on ne révèle pas quels comptes existent).

| Méthode | URL | Accès | Rôle |
|---|---|---|---|
| POST | /auth/register → `{verificationRequise, email, emailEnvoye}` (pas de jeton) | Public | Inscription, envoie le code |
| POST | /auth/verify-email `{"email", "code"}` → `{token, user}` | Public | Active le compte et connecte |
| POST | /auth/resend-code `{"email"}` | Public | Nouveau code de vérification |
| POST | /auth/forgot-password `{"email"}` | Public | Code de réinitialisation |
| POST | /auth/reset-password `{"email", "code", "nouveauMotDePasse"}` | Public | Nouveau mot de passe |

Connexion d'un compte non vérifié (seulement quand les emails sont actifs) : `403 {"erreur": "...", "code": "EMAIL_NON_VERIFIE"}`.
Quand les emails sont désactivés, les routes de vérification et de mot de passe oublié répondent `503`.
`GET /auth/config` (public) indique au front si les fonctions par email sont actives.
Les comptes créés par l'administrateur (agents) et par `SeedDev` sont vérifiés d'office.

| Méthode | URL | Accès | Besoin |
|---|---|---|---|
| POST | /auth/register, /auth/login | Public | BF-01, BF-02 |
| GET, PUT | /me | Connecté | BF-03, BF-04 |
| PUT | /me/password | Connecté | BF-04 |
| GET | /users | Administrateur | BF-05 |
| PUT | /users/{id}/block `{"bloque": true}` | Administrateur | BF-06 |
| DELETE | /users/{id} (409 si billets ou stand réservé : bloquer à la place) | Administrateur | BF-07 |
| POST | /users/agents | Administrateur | |

**Catégories et stands**

| Méthode | URL | Accès | Besoin |
|---|---|---|---|
| GET | /categories, /categories/{id} | Connecté | |
| POST, PUT, DELETE | /categories, /categories/{id} (suppression refusée si utilisée) | Administrateur | BF-08 à BF-10 |
| POST | /events/{id}/stands | Administrateur | BF-11 |
| PUT, DELETE | /stands/{id} (seul un stand libre est supprimable) | Administrateur | BF-12, BF-13 |
| GET | /events/{id}/stands, /stands/{id} (avec catégorie, événement et exposant) | Exposant, administrateur | BF-14 |

**Demandes de stand**

| Méthode | URL | Accès | Besoin |
|---|---|---|---|
| POST | /stand-requests `{"standId": 3}` | Exposant | BF-15 |
| GET | /me/stand-requests | Exposant | BF-16 |
| GET | /stand-requests | Administrateur | BF-17 |
| PUT | /stand-requests/{id} `{"decision": "VALIDEE"}` ou `{"decision": "REFUSEE", "motif": "..."}` | Administrateur | BF-18, BF-19 |

**Événements et programme**

| Méthode | URL | Accès | Besoin |
|---|---|---|---|
| GET | /events, /events/{id} | Public | BF-26 |
| POST, PUT, DELETE | /events, /events/{id} (suppression refusée si des billets sont vendus) | Administrateur | BF-20 à BF-22 |
| GET | /events/{id}/sessions, /sessions/{id} | Public | BF-26 |
| POST | /events/{id}/sessions | Administrateur | BF-23 |
| PUT, DELETE | /sessions/{id} | Administrateur | BF-24, BF-25 |
| POST, DELETE | /sessions/{id}/registration (billet valide exigé) | Visiteur | BF-27 |
| GET | /me/sessions | Visiteur | |

**Billetterie et accès**

| Méthode | URL | Accès | Besoin |
|---|---|---|---|
| GET | /events/{id}/ticket-types | Public | |
| POST | /events/{id}/ticket-types | Administrateur | BF-28 |
| PUT, DELETE | /ticket-types/{id} | Administrateur | BF-29, BF-30 |
| POST | /tickets `{"typeBilletId": 1}` | Visiteur | BF-31 |
| DELETE | /tickets/{id} (annulation) | Visiteur | |
| GET | /me/tickets | Visiteur | |
| GET | /tickets/{id}/pdf (PDF avec code QR) | Visiteur propriétaire | BF-32 |
| POST | /checkin `{"code": "..."}` | Agent d'accueil | BF-33 |

**Tableau de bord**

| Méthode | URL | Accès | Besoin |
|---|---|---|---|
| GET | /stats (billets, chiffre d'affaires, stands réservés, demandes en attente, remplissage des sessions, utilisateurs par rôle) | Administrateur | BF-34 |

## Tests manuels (Postman)

| Cas | Attendu |
|---|---|
| Mauvais mot de passe | 401 |
| `POST /events` sans jeton | 401 |
| `POST /events` avec jeton visiteur | 403 |
| JSON invalide | 400 |
| Élément inexistant | 404 |

## Limites connues

- Pas encore de tests JUnit (le scénario API couvre les routes, mais demande MySQL).
- `database.sql` : emplacement à confirmer avec le professeur (`resources/` selon le guide).
- Java 17 (le guide indique 21/22) : à confirmer.
- Inscription à une session : la capacité est vérifiée avant l'insertion, sans verrou ;
  deux inscriptions simultanées sur la dernière place pourraient dépasser la capacité.

## Travailler à deux avec Git

```bash
git pull                      # avant de travailler
git add .
git commit -m "Message clair"
git push
```

Un commit par fonctionnalité. Ne jamais publier `target/`, `node_modules/` ni de secret.