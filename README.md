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
| Base de données (10 tables avec `id`, `created_at`, `updated_at`) | Fait |
| Packages model, dao, service | Fait |
| DAO : exceptions `DataAccessException` (plus d'erreurs avalées) | Fait |
| Transactions (`util/Transaction`) : création, validation, refus de demande de stand | Fait |
| Protection contre la concurrence (`UPDATE ... WHERE statut = 'EN_ATTENTE'`) | Fait |
| Serveur HTTP, Router, CORS, codes 400/401/403/404/500 | Fait |
| Routes `/events` (écriture réservée à l'administrateur) | Fait |
| Routes `/auth/register` et `/auth/login` (JWT) | Fait |
| Front React : login, liste des événements | Fait (base) |
| Routes stands, demandes, billets, sessions | À faire |
| Contrôleurs/DTO restants, tests automatisés (JUnit) | À faire |
| Front : pages exposant, visiteur, agent, admin | À faire |
| PDF du billet, tableau de bord | À faire |

## Installation

1. JDK 17, IntelliJ, XAMPP (démarrer MySQL).
2. Importer `src/main/resources/database.sql` dans phpMyAdmin (crée la base `gestion_salon`).
3. Vérifier `config/DatabaseConnection.java` (root, mot de passe vide par défaut).
4. Lancer `Main` : serveur sur `http://localhost:8080`.
5. Frontend :
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Ouvrir `http://localhost:5173`.

Variable d'environnement conseillée : `JWT_SECRET` (secret de signature des jetons).

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

| Méthode | URL | Accès |
|---|---|---|
| POST | /auth/register, /auth/login | Public |
| GET | /events, /events/{id} | Public |
| POST, PUT, DELETE | /events, /events/{id} | Administrateur |

## Tests manuels (Postman)

| Cas | Attendu |
|---|---|
| Mauvais mot de passe | 401 |
| `POST /events` sans jeton | 401 |
| `POST /events` avec jeton visiteur | 403 |
| JSON invalide | 400 |
| Élément inexistant | 404 |

## Limites connues

- Pas encore de tests JUnit.
- `database.sql` : emplacement à confirmer avec le professeur (`resources/` selon le guide).
- Java 17 (le guide indique 21/22) : à confirmer.
- Comptes de test de `database.sql` : faux mots de passe, connexion impossible.

## Travailler à deux avec Git

```bash
git pull                      # avant de travailler
git add .
git commit -m "Message clair"
git push
```

Un commit par fonctionnalité. Ne jamais publier `target/`, `node_modules/` ni de secret.