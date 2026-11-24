# EventOra - Frontend React

Interface web d'EventOra (gestion de salons et festivals), compatible avec le backend Java
du projet (`gestion-salon`). React 19 + Vite + React Router.

## Lancer

1. Demarrer le backend (MySQL + `Main`). Par defaut il ecoute sur `http://localhost:8080`.
2. Installer et lancer le front :
   ```bash
   npm install
   npm run dev
   ```
3. Ouvrir **http://localhost:5173** (et pas 127.0.0.1 : le backend n'autorise que cette adresse en CORS).

### Backend sur un autre port

Si le backend tourne sur un autre port (par exemple 8081 quand Oracle occupe le 8080),
creer un fichier `.env.local` a la racine du front :
```
VITE_API_URL=http://localhost:8081
```
puis relancer `npm run dev`.

## Fonctions de connexion

- Le front demande au backend (`GET /auth/config`) si les emails sont actifs (cle Resend configuree).
  - Emails desactives (par defaut) : l'inscription connecte directement, le lien "Mot de passe oublie" est cache.
  - Emails actifs : un code a 6 chiffres est envoye a l'inscription ; l'ecran de verification le demande avant
    de connecter. Mot de passe oublie : email -> code + nouveau mot de passe, en deux etapes qui glissent.
- Mode jour / nuit : bouton soleil / lune dans la barre de navigation et sur les pages de connexion.
  Le choix est memorise ; le changement se fait par une revelation circulaire depuis le bouton (Chrome, Edge).
- Icones : bibliotheque [lucide-react](https://lucide.dev) (traits fins, suivent la couleur du theme).

## Pages par role

| Role | Pages |
|---|---|
| Tous | Accueil (evenements), page d'un evenement (billetterie + programme), connexion / inscription, verification de l'email, mot de passe oublie, profil |
| Visiteur | Mon espace : billets (PDF avec QR, annulation), sessions ; reservation et inscription aux sessions |
| Exposant | Plan des stands (demande de stand), mes demandes |
| Agent d'accueil | Controle des entrees (scan ou saisie du code, compatible lecteur QR USB) |
| Administrateur | Tableau de bord, evenements (stands, types de billets, sessions), demandes de stand, categories, utilisateurs |

## Design

- Theme "festival de nuit" : fond indigo anime, degrades corail -> rose -> violet (couleurs du logo).
- Logo EventOra : un pavillon d'exposition dont les allees dessinent la lettre E (`src/components/Logo.jsx`, `public/favicon.svg`).
- Connexion / inscription : panneau colore qui glisse d'un formulaire a l'autre, champs qui apparaissent un par un
  (sur telephone, les formulaires defilent horizontalement).
- Curseur anime : point + anneau qui le suit, grossit sur les elements cliquables, onde au clic.
  Desactive sur ecran tactile et si Windows est regle sur "reduire les animations".

## Structure

```
src/
  api.js              appels au backend (une fonction par route)
  auth.jsx            connexion, jeton, routes protegees par role
  App.jsx             routes
  styles.css          theme et animations
  components/         Logo, Cursor, Layout (navigation, fond), Toast, ui (modale, badges...)
  pages/              pages publiques, visiteur, exposant, agent
  pages/admin/        pages administrateur
```
