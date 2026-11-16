DROP DATABASE IF EXISTS gestion_salon;
CREATE DATABASE gestion_salon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gestion_salon;

-- 1. Tables sans dependance
CREATE TABLE utilisateur (
                             id INT AUTO_INCREMENT PRIMARY KEY,
                             nom VARCHAR(100) NOT NULL,
                             prenom VARCHAR(100) NOT NULL,
                             email VARCHAR(255) NOT NULL UNIQUE,
                             mot_de_passe_hash VARCHAR(255) NOT NULL,
                             telephone VARCHAR(30),
                             role ENUM('ADMINISTRATEUR', 'EXPOSANT', 'VISITEUR', 'AGENT_ACCUEIL') NOT NULL,
                             est_bloque BOOLEAN NOT NULL DEFAULT FALSE,
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE categorie (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           nom VARCHAR(100) NOT NULL UNIQUE,
                           description TEXT,
                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE evenement (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           titre VARCHAR(150) NOT NULL,
                           description TEXT,
                           date_debut DATE NOT NULL,
                           date_fin DATE NOT NULL,
                           lieu VARCHAR(200) NOT NULL,
                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           CONSTRAINT chk_evenement_dates CHECK (date_fin >= date_debut)
);

-- 2. Sous-classes d'Utilisateur (cle primaire = cle etrangere, relation 1:1)
CREATE TABLE exposant (
                          id INT PRIMARY KEY,
                          nom_entreprise VARCHAR(150) NOT NULL,
                          logo_url VARCHAR(500),
                          description TEXT,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          CONSTRAINT fk_exposant_utilisateur
                              FOREIGN KEY (id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE agent_accueil (
                               id INT PRIMARY KEY,
                               matricule VARCHAR(50) NOT NULL UNIQUE,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               CONSTRAINT fk_agent_utilisateur
                                   FOREIGN KEY (id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

-- 3. Tables dependant de evenement / categorie
CREATE TABLE session_evenement (
                                   id INT AUTO_INCREMENT PRIMARY KEY,
                                   evenement_id INT NOT NULL,
                                   titre VARCHAR(150) NOT NULL,
                                   description TEXT,
                                   heure_debut DATETIME NOT NULL,
                                   heure_fin DATETIME NOT NULL,
                                   capacite_max INT NOT NULL,
                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   CONSTRAINT fk_session_evenement
                                       FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE
);

CREATE TABLE stand (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       evenement_id INT NOT NULL,
                       categorie_id INT NOT NULL,
                       numero VARCHAR(20) NOT NULL,
                       emplacement VARCHAR(100),
                       superficie DECIMAL(8,2) NOT NULL,
                       prix DECIMAL(10,2) NOT NULL,
                       statut ENUM('LIBRE', 'EN_ATTENTE', 'RESERVE') NOT NULL DEFAULT 'LIBRE',
                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       CONSTRAINT uq_stand_numero UNIQUE (evenement_id, numero),
                       CONSTRAINT fk_stand_evenement
                           FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE,
                       CONSTRAINT fk_stand_categorie
                           FOREIGN KEY (categorie_id) REFERENCES categorie(id)
);

-- 4. Tables dependant des precedentes
CREATE TABLE inscription_session (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     visiteur_id INT NOT NULL,
                                     session_id INT NOT NULL,
                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     CONSTRAINT uq_insc_visiteur_session UNIQUE (visiteur_id, session_id),
                                     CONSTRAINT fk_insc_visiteur
                                         FOREIGN KEY (visiteur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_insc_session
                                         FOREIGN KEY (session_id) REFERENCES session_evenement(id) ON DELETE CASCADE
);

CREATE TABLE demande_stand (
                               id INT AUTO_INCREMENT PRIMARY KEY,
                               exposant_id INT NOT NULL,
                               stand_id INT NOT NULL,
                               admin_id INT NULL,
                               date_demande DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               statut ENUM('EN_ATTENTE', 'VALIDEE', 'REFUSEE') NOT NULL DEFAULT 'EN_ATTENTE',
                               motif_refus TEXT,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               CONSTRAINT fk_demande_exposant
                                   FOREIGN KEY (exposant_id) REFERENCES exposant(id) ON DELETE CASCADE,
                               CONSTRAINT fk_demande_stand
                                   FOREIGN KEY (stand_id) REFERENCES stand(id) ON DELETE CASCADE,
                               CONSTRAINT fk_demande_admin
                                   FOREIGN KEY (admin_id) REFERENCES utilisateur(id) ON DELETE SET NULL
);

CREATE TABLE billet (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        visiteur_id INT NOT NULL,
                        evenement_id INT NOT NULL,
                        agent_id INT NULL,
                        code_qr VARCHAR(100) NOT NULL UNIQUE,
                        date_achat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        statut ENUM('VALIDE', 'UTILISE', 'ANNULE') NOT NULL DEFAULT 'VALIDE',
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_billet_visiteur
                            FOREIGN KEY (visiteur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
                        CONSTRAINT fk_billet_evenement
                            FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE,
                        CONSTRAINT fk_billet_agent
                            FOREIGN KEY (agent_id) REFERENCES agent_accueil(id) ON DELETE SET NULL
);