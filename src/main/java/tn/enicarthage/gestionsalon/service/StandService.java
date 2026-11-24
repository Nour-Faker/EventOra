package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.dao.CategorieDAO;
import tn.enicarthage.gestionsalon.dao.DemandeStandDAO;
import tn.enicarthage.gestionsalon.dao.EvenementDAO;
import tn.enicarthage.gestionsalon.dao.StandDAO;
import tn.enicarthage.gestionsalon.dao.StandVueDAO;
import tn.enicarthage.gestionsalon.dao.StandVueDAO.DemandeVue;
import tn.enicarthage.gestionsalon.dao.StandVueDAO.StandVue;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.DemandeStand;
import tn.enicarthage.gestionsalon.model.Stand;
import tn.enicarthage.gestionsalon.model.StatutDemande;
import tn.enicarthage.gestionsalon.model.StatutStand;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class StandService {

    private final StandDAO standDAO = new StandDAO();
    private final DemandeStandDAO demandeDAO = new DemandeStandDAO();
    private final StandVueDAO vueDAO = new StandVueDAO();
    private final EvenementDAO evenementDAO = new EvenementDAO();
    private final CategorieDAO categorieDAO = new CategorieDAO();

    public List<StandVue> stands(int evenementId) { return vueDAO.listerStands(evenementId); }

    public StandVue stand(int standId) {
        StandVue vue = vueDAO.trouverStand(standId);
        if (vue == null) {
            throw new ResourceNotFoundException("Stand introuvable.");
        }
        return vue;
    }
    public List<DemandeVue> mesDemandes(int exposantId) { return vueDAO.demandesDeLExposant(exposantId); }
    public List<DemandeVue> toutesLesDemandes() { return vueDAO.toutesLesDemandes(); }

    public DemandeVue demander(int exposantId, int standId) {
        int demandeId;
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Stand stand = standDAO.findByIdForUpdate(c, standId); // verrou
                if (stand == null) {
                    throw new ResourceNotFoundException("Stand introuvable.");
                }
                if (stand.getStatut() == StatutStand.RESERVE) {
                    throw new ConflictException("Ce stand est deja reserve.");
                }
                if (demandeDAO.existeEnAttente(c, exposantId, standId)) {
                    throw new ConflictException("Vous avez deja une demande en attente pour ce stand.");
                }
                DemandeStand d = new DemandeStand();
                d.setExposantId(exposantId);
                d.setStandId(standId);
                d.setStatut(StatutDemande.EN_ATTENTE);
                demandeDAO.create(c, d);
                if (stand.getStatut() == StatutStand.LIBRE) {
                    standDAO.updateStatut(c, standId, StatutStand.EN_ATTENTE);
                }
                c.commit();
                demandeId = d.getId();
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return vueDAO.trouverDemande(demandeId);
    }

    public DemandeVue traiter(int adminId, int demandeId, String decision, String motif) {
        if (!"VALIDEE".equals(decision) && !"REFUSEE".equals(decision)) {
            throw new IllegalArgumentException("Decision invalide : VALIDEE ou REFUSEE.");
        }
        boolean valider = "VALIDEE".equals(decision);
        if (!valider && (motif == null || motif.isBlank())) {
            throw new IllegalArgumentException("Le motif du refus est obligatoire.");
        }

        DemandeStand demande = demandeDAO.findById(demandeId);
        if (demande == null) {
            throw new ResourceNotFoundException("Demande introuvable.");
        }

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Meme ordre de verrouillage partout : le stand d'abord
                Stand stand = standDAO.findByIdForUpdate(c, demande.getStandId());
                if (stand == null) {
                    throw new ResourceNotFoundException("Stand introuvable.");
                }
                demande.setAdminId(adminId);
                demande.setStatut(valider ? StatutDemande.VALIDEE : StatutDemande.REFUSEE);
                demande.setMotifRefus(valider ? null : motif.trim());

                if (valider && stand.getStatut() == StatutStand.RESERVE) {
                    throw new ConflictException("Ce stand est deja reserve.");
                }
                if (!demandeDAO.updateSiEnAttente(c, demande)) {
                    throw new ConflictException("Cette demande a deja ete traitee.");
                }
                if (valider) {
                    vueDAO.reserver(c, demande.getStandId(), demande.getExposantId());
                    demandeDAO.refuserAutresEnAttente(c, demande.getStandId(), demandeId, adminId);
                } else if (demandeDAO.countEnAttenteByStand(c, demande.getStandId()) == 0) {
                    standDAO.updateStatutSiEnAttente(c, demande.getStandId(), StatutStand.LIBRE);
                }
                c.commit();
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return vueDAO.trouverDemande(demandeId);
    }

    // ---------- Gestion des stands (administrateur) : BF-11 a BF-13 ----------

    public StandVue creerStand(int evenementId, int categorieId, String numero,
                               String emplacement, BigDecimal superficie, BigDecimal prix) {
        if (evenementDAO.findById(evenementId) == null) {
            throw new ResourceNotFoundException("Evenement introuvable.");
        }
        Stand stand = new Stand();
        stand.setEvenementId(evenementId);
        stand.setStatut(StatutStand.LIBRE);
        remplir(stand, categorieId, numero, emplacement, superficie, prix);
        standDAO.create(stand);
        return vueDAO.trouverStand(stand.getId());
    }

    // L'evenement et le statut d'un stand ne changent pas ici (le statut suit les demandes)
    public StandVue modifierStand(int standId, int categorieId, String numero,
                                  String emplacement, BigDecimal superficie, BigDecimal prix) {
        Stand stand = standDAO.findById(standId);
        if (stand == null) {
            throw new ResourceNotFoundException("Stand introuvable.");
        }
        remplir(stand, categorieId, numero, emplacement, superficie, prix);
        standDAO.update(stand);
        return vueDAO.trouverStand(standId);
    }

    public void supprimerStand(int standId) {
        if (standDAO.findById(standId) == null) {
            throw new ResourceNotFoundException("Stand introuvable.");
        }
        // Condition dans le WHERE : pas de fenetre entre la verification et la suppression
        if (!standDAO.supprimerSiLibre(standId)) {
            throw new ConflictException("Seul un stand libre peut etre supprime.");
        }
    }

    private void remplir(Stand stand, int categorieId, String numero, String emplacement,
                         BigDecimal superficie, BigDecimal prix) {
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("Le numero du stand est obligatoire.");
        }
        if (superficie == null || superficie.signum() <= 0) {
            throw new IllegalArgumentException("La superficie doit etre strictement positive.");
        }
        if (prix == null || prix.signum() < 0) {
            throw new IllegalArgumentException("Le prix doit etre positif ou nul.");
        }
        if (categorieDAO.findById(categorieId) == null) {
            throw new ResourceNotFoundException("Categorie introuvable.");
        }
        String numeroNormalise = numero.trim();
        if (standDAO.existeNumero(stand.getEvenementId(), numeroNormalise, stand.getId())) {
            throw new ConflictException("Ce numero de stand existe deja pour cet evenement.");
        }
        stand.setCategorieId(categorieId);
        stand.setNumero(numeroNormalise);
        stand.setEmplacement(emplacement);
        stand.setSuperficie(superficie.doubleValue());
        stand.setPrix(prix);
    }
}
