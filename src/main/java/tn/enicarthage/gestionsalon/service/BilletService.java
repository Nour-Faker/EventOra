package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.dao.BilletDAO;
import tn.enicarthage.gestionsalon.dao.BilletDAO.BilletVue;
import tn.enicarthage.gestionsalon.dao.BilletDAO.ScanVue;
import tn.enicarthage.gestionsalon.dao.BilletDAO.TypeBilletVue;
import tn.enicarthage.gestionsalon.dao.BilletDAO.TypeVerrouille;
import tn.enicarthage.gestionsalon.dao.EvenementDAO;
import tn.enicarthage.gestionsalon.dao.UtilisateurDAO;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.Utilisateur;
import tn.enicarthage.gestionsalon.util.BilletPdf;
import tn.enicarthage.gestionsalon.util.CodeBillet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class BilletService {

    private final BilletDAO dao = new BilletDAO();
    private final EvenementDAO evenementDAO = new EvenementDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    public List<TypeBilletVue> types(int evenementId) {
        return dao.listerTypes(evenementId);
    }

    public List<BilletVue> mesBillets(int visiteurId) {
        return dao.trouverParVisiteur(visiteurId);
    }

    public BilletVue acheter(int visiteurId, int typeBilletId) {
        int billetId;
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TypeVerrouille type = dao.verrouillerType(c, typeBilletId);
                if (type == null) {
                    throw new ResourceNotFoundException("Type de billet introuvable.");
                }
                if (type.dateFin().isBefore(LocalDate.now())) {
                    throw new ConflictException("Cet evenement est termine.");
                }
                if (dao.existeActifPourEvenement(c, visiteurId, type.evenementId())) {
                    throw new ConflictException("Vous avez deja un billet pour cet evenement.");
                }
                if (dao.compterActifs(c, typeBilletId) >= type.quota()) {
                    throw new ConflictException("Plus de places pour ce type de billet.");
                }
                billetId = dao.inserer(c, visiteurId, typeBilletId, CodeBillet.generer(), type.prix());
                c.commit();
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return dao.trouverVue(billetId);
    }

    public void annuler(int visiteurId, int billetId) {
        // 404 aussi pour le billet d'un autre : on ne revele pas son existence
        if (!dao.annuler(visiteurId, billetId)) {
            throw new ResourceNotFoundException("Billet introuvable ou non annulable.");
        }
    }

    public ScanVue scanner(int agentId, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Le code est obligatoire.");
        }
        String codeNormalise = code.trim().toUpperCase();
        boolean ok = dao.marquerUtilise(agentId, codeNormalise);
        ScanVue info = dao.infoScan(codeNormalise);
        if (info == null) {
            throw new ResourceNotFoundException("Billet inconnu.");
        }
        if (!ok) {
            throw new ConflictException("UTILISE".equals(info.statut())
                    ? "Billet deja utilise" + (info.dateScan() != null
                            ? " (premier scan : " + info.dateScan() + ")." : ".")
                    : "Billet annule.");
        }
        return info;
    }

    // ---------- PDF de confirmation (BF-32) ----------

    // Le billet doit appartenir au visiteur connecte : sinon 404 (on ne revele pas son existence)
    public byte[] pdf(int visiteurId, int billetId) {
        BilletVue b = dao.trouverVueDuVisiteur(visiteurId, billetId);
        if (b == null) {
            throw new ResourceNotFoundException("Billet introuvable.");
        }
        Utilisateur u = utilisateurDAO.findById(visiteurId);
        String visiteur = u != null ? u.getPrenom() + " " + u.getNom() : "-";
        return BilletPdf.generer(b.codeQr(), b.titre(), b.lieu(), b.dateDebut(),
                b.typeBillet(), b.prixPaye(), visiteur, b.statut());
    }

    // ---------- Types de billets (administrateur) : BF-28 a BF-30 ----------

    public TypeBilletVue creerType(int evenementId, String nom, BigDecimal prix, int quota) {
        if (evenementDAO.findById(evenementId) == null) {
            throw new ResourceNotFoundException("Evenement introuvable.");
        }
        String nomNormalise = validerType(nom, prix, quota);
        if (dao.existeNomType(evenementId, nomNormalise, 0)) {
            throw new ConflictException("Ce type de billet existe deja pour cet evenement.");
        }
        return dao.trouverTypeVue(dao.creerType(evenementId, nomNormalise, prix, quota));
    }

    public TypeBilletVue modifierType(int typeId, String nom, BigDecimal prix, int quota) {
        Integer evenementId = dao.evenementDuType(typeId);
        if (evenementId == null) {
            throw new ResourceNotFoundException("Type de billet introuvable.");
        }
        String nomNormalise = validerType(nom, prix, quota);
        if (dao.existeNomType(evenementId, nomNormalise, typeId)) {
            throw new ConflictException("Ce type de billet existe deja pour cet evenement.");
        }
        if (quota < dao.compterVendus(typeId)) {
            throw new ConflictException("Le quota ne peut pas etre inferieur aux billets deja vendus.");
        }
        dao.modifierType(typeId, nomNormalise, prix, quota);
        return dao.trouverTypeVue(typeId);
    }

    public void supprimerType(int typeId) {
        if (dao.evenementDuType(typeId) == null) {
            throw new ResourceNotFoundException("Type de billet introuvable.");
        }
        // FK billet -> type_billet en RESTRICT : 409 explicite plutot qu'une erreur SQL (500)
        if (dao.compterTous(typeId) > 0) {
            throw new ConflictException("Des billets existent pour ce type : suppression impossible.");
        }
        dao.supprimerType(typeId);
    }

    private String validerType(String nom, BigDecimal prix, int quota) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du type de billet est obligatoire.");
        }
        if (prix == null || prix.signum() < 0) {
            throw new IllegalArgumentException("Le prix doit etre positif ou nul.");
        }
        if (quota <= 0) {
            throw new IllegalArgumentException("Le quota doit etre strictement positif.");
        }
        return nom.trim();
    }
}
