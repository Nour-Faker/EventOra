package tn.enicarthage.gestionSalon.service;

import tn.enicarthage.gestionSalon.dao.CategorieDAO;
import tn.enicarthage.gestionSalon.dao.EvenementDAO;
import tn.enicarthage.gestionSalon.dao.StandDAO;
import tn.enicarthage.gestionSalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionSalon.model.Stand;
import tn.enicarthage.gestionSalon.model.StatutStand;

import java.math.BigDecimal;
import java.util.List;

public class StandService {

    private final StandDAO standDAO;
    private final CategorieDAO categorieDAO;
    private final EvenementDAO evenementDAO;

    public StandService() {
        this.standDAO = new StandDAO();
        this.categorieDAO = new CategorieDAO();
        this.evenementDAO = new EvenementDAO();
    }

    public void createStand(Stand stand) {
        validate(stand);
        if (stand.getStatut() == null) {
            stand.setStatut(StatutStand.LIBRE); // un nouveau stand est libre
        }
        standDAO.create(stand);
    }

    public List<Stand> getAllStands() {
        return standDAO.findAll();
    }

    public List<Stand> getStandsByEvenement(int evenementId) {
        return standDAO.findByEvenementId(evenementId);
    }

    public Stand getStandById(int id) {
        Stand stand = standDAO.findById(id);
        if (stand == null) {
            throw new ResourceNotFoundException("Stand introuvable : " + id);
        }
        return stand;
    }

    public void updateStand(Stand stand) {
        getStandById(stand.getId()); // verifie que le stand existe
        validate(stand);
        standDAO.update(stand);
    }

    public void deleteStand(int id) {
        getStandById(id);
        standDAO.delete(id);
    }

    private void validate(Stand stand) {
        if (stand.getNumero() == null || stand.getNumero().isBlank()) {
            throw new IllegalArgumentException("Le numero du stand est obligatoire.");
        }
        if (stand.getSuperficie() <= 0) {
            throw new IllegalArgumentException("La superficie doit etre positive.");
        }
        if (stand.getPrix() == null || stand.getPrix().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix doit etre positif.");
        }
        if (evenementDAO.findById(stand.getEvenementId()) == null) {
            throw new ResourceNotFoundException(
                    "Evenement introuvable : " + stand.getEvenementId());
        }
        if (categorieDAO.findById(stand.getCategorieId()) == null) {
            throw new ResourceNotFoundException(
                    "Categorie introuvable : " + stand.getCategorieId());
        }
        // Un numero de stand est unique dans un evenement
        for (Stand existant : standDAO.findByEvenementId(stand.getEvenementId())) {
            if (existant.getNumero().equalsIgnoreCase(stand.getNumero())
                    && existant.getId() != stand.getId()) {
                throw new IllegalArgumentException(
                        "Ce numero de stand existe deja pour cet evenement.");
            }
        }
    }
}