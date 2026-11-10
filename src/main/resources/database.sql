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
                             est_bloque BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE categorie (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           nom VARCHAR(100) NOT NULL UNIQUE,
                           description TEXT
);

CREATE TABLE evenement (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           titre VARCHAR(150) NOT NULL,
                           description TEXT,
                           date_debut DATE NOT NULL,
                           date_fin DATE NOT NULL,
                           lieu VARCHAR(200) NOT NULL,
                           CONSTRAINT chk_evenement_dates CHECK (date_fin >= date_debut)
);

-- 2. Sous-classes d'Utilisateur (cle primaire = cle etrangere, relation 1:1)
CREATE TABLE exposant (
                          id INT PRIMARY KEY,
                          nom_entreprise VARCHAR(150) NOT NULL,
                          logo_url VARCHAR(500),
                          description TEXT,
                          CONSTRAINT fk_exposant_utilisateur
                              FOREIGN KEY (id) REFERENCES utilisateur(id)
                                  ON DELETE CASCADE
);

CREATE TABLE agent_accueil (
                               id INT PRIMARY KEY,
                               matricule VARCHAR(50) NOT NULL UNIQUE,
                               CONSTRAINT fk_agent_utilisateur
                                   FOREIGN KEY (id) REFERENCES utilisateur(id)
                                       ON DELETE CASCADE
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
                                   CONSTRAINT fk_session_evenement
                                       FOREIGN KEY (evenement_id) REFERENCES evenement(id)
                                           ON DELETE CASCADE
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
                       CONSTRAINT uq_stand_numero UNIQUE (evenement_id, numero),
                       CONSTRAINT fk_stand_evenement
                           FOREIGN KEY (evenement_id) REFERENCES evenement(id)
                               ON DELETE CASCADE,
                       CONSTRAINT fk_stand_categorie
                           FOREIGN KEY (categorie_id) REFERENCES categorie(id)
);

-- 4. Tables dependant des precedentes
CREATE TABLE inscription_session (
                                     visiteur_id INT NOT NULL,
                                     session_id INT NOT NULL,
                                     PRIMARY KEY (visiteur_id, session_id),
                                     CONSTRAINT fk_insc_visiteur
                                         FOREIGN KEY (visiteur_id) REFERENCES utilisateur(id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT fk_insc_session
                                         FOREIGN KEY (session_id) REFERENCES session_evenement(id)
                                             ON DELETE CASCADE
);

CREATE TABLE demande_stand (
                               id INT AUTO_INCREMENT PRIMARY KEY,
                               exposant_id INT NOT NULL,
                               stand_id INT NOT NULL,
                               admin_id INT NULL,
                               date_demande DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               statut ENUM('EN_ATTENTE', 'VALIDEE', 'REFUSEE') NOT NULL DEFAULT 'EN_ATTENTE',
                               motif_refus TEXT,
                               CONSTRAINT fk_demande_exposant
                                   FOREIGN KEY (exposant_id) REFERENCES exposant(id)
                                       ON DELETE CASCADE,
                               CONSTRAINT fk_demande_stand
                                   FOREIGN KEY (stand_id) REFERENCES stand(id)
                                       ON DELETE CASCADE,
                               CONSTRAINT fk_demande_admin
                                   FOREIGN KEY (admin_id) REFERENCES utilisateur(id)
                                       ON DELETE SET NULL
);

CREATE TABLE billet (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        visiteur_id INT NOT NULL,
                        evenement_id INT NOT NULL,
                        agent_id INT NULL,
                        code_qr VARCHAR(100) NOT NULL UNIQUE,
                        date_achat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        statut ENUM('VALIDE', 'UTILISE', 'ANNULE') NOT NULL DEFAULT 'VALIDE',
                        CONSTRAINT fk_billet_visiteur
                            FOREIGN KEY (visiteur_id) REFERENCES utilisateur(id)
                                ON DELETE CASCADE,
                        CONSTRAINT fk_billet_evenement
                            FOREIGN KEY (evenement_id) REFERENCES evenement(id)
                                ON DELETE CASCADE,
                        CONSTRAINT fk_billet_agent
                            FOREIGN KEY (agent_id) REFERENCES agent_accueil(id)
                                ON DELETE SET NULL
);

-- 5. Donnees de test (les mots de passe sont des valeurs provisoires, pas de vrais hash)
INSERT INTO utilisateur (nom, prenom, email, mot_de_passe_hash, telephone, role) VALUES
                                                                                     ('Ben Salah', 'Karim', 'admin@salon.tn', 'hash_provisoire_1', '71000001', 'ADMINISTRATEUR'),
                                                                                     ('Trabelsi', 'Sami', 'contact@techplus.tn', 'hash_provisoire_2', '71000002', 'EXPOSANT'),
                                                                                     ('Mansour', 'Leila', 'info@artisanat-tn.tn', 'hash_provisoire_3', '71000003', 'EXPOSANT'),
                                                                                     ('Gharbi', 'Nour', 'hello@bio-delices.tn', 'hash_provisoire_4', '71000004', 'EXPOSANT'),
                                                                                     ('Jlassi', 'Amine', 'amine.jlassi@mail.tn', 'hash_provisoire_5', '71000005', 'VISITEUR'),
                                                                                     ('Ben Ali', 'Sarra', 'sarra.benali@mail.tn', 'hash_provisoire_6', '71000006', 'VISITEUR'),
                                                                                     ('Hammami', 'Youssef', 'youssef.hammami@salon.tn', 'hash_provisoire_7', '71000007', 'AGENT_ACCUEIL');

INSERT INTO categorie (nom, description) VALUES
                                             ('Technologie', 'Informatique, objets connectes et innovation'),
                                             ('Artisanat', 'Poterie, ceramique, tissage et creations manuelles'),
                                             ('Alimentation', 'Produits du terroir et epicerie fine');

INSERT INTO evenement (titre, description, date_debut, date_fin, lieu) VALUES
                                                                           ('Salon Innovation et Artisanat 2026', 'Salon professionnel dedie a la technologie et a l artisanat', '2026-11-20', '2026-11-22', 'Parc des Expositions, Tunis'),
                                                                           ('Festival Gourmand 2026', 'Festival de la gastronomie tunisienne', '2026-12-05', '2026-12-06', 'Palais des Congres, Tunis');

INSERT INTO exposant (id, nom_entreprise, logo_url, description) VALUES
                                                                     (2, 'TechPlus', 'https://example.com/logo-techplus.png', 'Solutions informatiques et objets connectes'),
                                                                     (3, 'Artisanat Tunisien', NULL, 'Poterie, ceramique et tissage'),
                                                                     (4, 'Bio Delices', NULL, 'Produits du terroir et epicerie fine');

INSERT INTO agent_accueil (id, matricule) VALUES
    (7, 'AG-001');

INSERT INTO session_evenement (evenement_id, titre, description, heure_debut, heure_fin, capacite_max) VALUES
                                                                                                           (1, 'Conference IA et entreprises', 'Comment l IA transforme les entreprises', '2026-11-20 10:00:00', '2026-11-20 11:30:00', 100),
                                                                                                           (1, 'Atelier poterie', 'Initiation a la poterie traditionnelle', '2026-11-21 14:00:00', '2026-11-21 16:00:00', 20),
                                                                                                           (2, 'Atelier cuisine tunisienne', 'Preparation de plats traditionnels', '2026-12-05 11:00:00', '2026-12-05 13:00:00', 30);

INSERT INTO stand (evenement_id, categorie_id, numero, emplacement, superficie, prix, statut) VALUES
                                                                                                  (1, 1, 'A01', 'Hall A', 12.00, 800.00, 'RESERVE'),
                                                                                                  (1, 1, 'A02', 'Hall A', 9.00, 600.00, 'LIBRE'),
                                                                                                  (1, 2, 'B01', 'Hall B', 15.00, 700.00, 'EN_ATTENTE'),
                                                                                                  (1, 2, 'B02', 'Hall B', 10.00, 500.00, 'LIBRE'),
                                                                                                  (2, 3, 'C01', 'Zone C', 20.00, 900.00, 'LIBRE');

INSERT INTO inscription_session (visiteur_id, session_id) VALUES
                                                              (5, 1), (5, 2), (6, 1), (6, 3);

INSERT INTO demande_stand (exposant_id, stand_id, admin_id, date_demande, statut, motif_refus) VALUES
                                                                                                   (2, 1, 1, '2026-10-01 09:30:00', 'VALIDEE', NULL),
                                                                                                   (3, 3, NULL, '2026-10-05 14:10:00', 'EN_ATTENTE', NULL),
                                                                                                   (4, 5, 1, '2026-10-06 16:45:00', 'REFUSEE', 'Stand deja attribue');

INSERT INTO billet (visiteur_id, evenement_id, agent_id, code_qr, date_achat, statut) VALUES
                                                                                          (5, 1, NULL, 'QR-0001-ABC', '2026-10-10 10:00:00', 'VALIDE'),
                                                                                          (6, 1, NULL, 'QR-0002-DEF', '2026-10-11 11:20:00', 'VALIDE'),
                                                                                          (5, 2, NULL, 'QR-0003-GHI', '2026-10-12 08:15:00', 'ANNULE');

-- 6. Requetes de verification (a executer separement, une par une)

-- SELECT simple
-- SELECT * FROM stand;

-- WHERE : stands libres de l'evenement 1
-- SELECT numero, emplacement, prix FROM stand WHERE statut = 'LIBRE' AND evenement_id = 1;

-- JOIN : chaque stand avec son evenement et sa categorie
-- SELECT s.numero, s.prix, s.statut, e.titre AS evenement, c.nom AS categorie
-- FROM stand s
-- JOIN evenement e ON s.evenement_id = e.id
-- JOIN categorie c ON s.categorie_id = c.id;

-- JOIN : demandes de stand avec le nom de l'exposant
-- SELECT ex.nom_entreprise, s.numero AS stand, d.statut, d.date_demande
-- FROM demande_stand d
-- JOIN exposant ex ON d.exposant_id = ex.id
-- JOIN stand s ON d.stand_id = s.id
-- ORDER BY d.date_demande;

-- JOIN N:N : sessions de chaque visiteur
-- SELECT u.prenom, u.nom, se.titre AS session, se.heure_debut
-- FROM inscription_session i
-- JOIN utilisateur u ON i.visiteur_id = u.id
-- JOIN session_evenement se ON i.session_id = se.id
-- ORDER BY u.nom;

-- Places restantes par session
-- SELECT se.titre, se.capacite_max, COUNT(i.visiteur_id) AS inscrits,
--        se.capacite_max - COUNT(i.visiteur_id) AS places_restantes
-- FROM session_evenement se
-- LEFT JOIN inscription_session i ON i.session_id = se.id
-- GROUP BY se.id, se.titre, se.capacite_max;

-- INSERT : nouveau stand puis demande sur ce stand
-- INSERT INTO stand (evenement_id, categorie_id, numero, emplacement, superficie, prix)
-- VALUES (2, 3, 'C02', 'Zone C', 18.00, 850.00);
-- INSERT INTO demande_stand (exposant_id, stand_id)
-- VALUES (3, (SELECT id FROM stand WHERE evenement_id = 2 AND numero = 'C02'));

-- UPDATE : l'administrateur valide la demande 2 et le stand devient reserve
-- UPDATE demande_stand SET statut = 'VALIDEE', admin_id = 1 WHERE id = 2;
-- UPDATE stand SET statut = 'RESERVE' WHERE id = 3;

-- UPDATE : l'agent d'accueil scanne un billet
-- UPDATE billet SET statut = 'UTILISE', agent_id = 7 WHERE code_qr = 'QR-0001-ABC';

-- DELETE : annuler une inscription a une session
-- DELETE FROM inscription_session WHERE visiteur_id = 6 AND session_id = 3;

-- DELETE : supprimer un stand (la demande liee disparait avec ON DELETE CASCADE)
-- DELETE FROM stand WHERE evenement_id = 2 AND numero = 'C02';