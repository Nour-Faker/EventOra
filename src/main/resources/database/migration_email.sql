-- Mise a jour d'une base gestion_salon EXISTANTE pour la verification par email,
-- sans perdre les donnees (a executer une seule fois dans phpMyAdmin).
-- Pour une nouvelle base, importer simplement database.sql.
USE gestion_salon;

ALTER TABLE utilisateur ADD COLUMN email_verifie BOOLEAN NOT NULL DEFAULT FALSE AFTER est_bloque;

-- Les comptes deja crees restent utilisables : on les considere comme verifies
UPDATE utilisateur SET email_verifie = TRUE;

CREATE TABLE code_email (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            utilisateur_id INT NOT NULL,
                            type ENUM('VERIFICATION', 'REINITIALISATION') NOT NULL,
                            code_hash CHAR(64) NOT NULL,
                            expire_le DATETIME NOT NULL,
                            tentatives INT NOT NULL DEFAULT 0,
                            utilise BOOLEAN NOT NULL DEFAULT FALSE,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            INDEX idx_code_utilisateur_type (utilisateur_id, type),
                            CONSTRAINT fk_code_utilisateur
                                FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
);
