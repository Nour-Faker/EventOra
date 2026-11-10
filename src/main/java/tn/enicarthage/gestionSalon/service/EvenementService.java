package tn.enicarthage.gestionSalon.service;

import tn.enicarthage.gestionSalon.dao.EvenementDAO;
import tn.enicarthage.gestionSalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionSalon.model.Evenement;

import java.util.List;

public class EvenementService {

    private final EvenementDAO evenementDAO;

    public EvenementService() {
        this.evenementDAO = new EvenementDAO();
    }

    public void createEvenement(Evenement evenement) {
        validate(evenement);
        evenementDAO.create(evenement);
    }

    public List<Evenement> getAllEvenements() {
        return evenementDAO.findAll();
    }

    public Evenement getEvenementById(int id) {
        Evenement evenement = evenementDAO.findById(id);
        if (evenement == null) {
            throw new ResourceNotFoundException("Evenement introuvable : " + id);
        }
        return evenement;
    }

    public void updateEvenement(Evenement evenement) {
        getEvenementById(evenement.getId()); // verifie que l'evenement existe
        validate(evenement);
        evenementDAO.update(evenement);
    }

    public void deleteEvenement(int id) {
        getEvenementById(id);
        evenementDAO.delete(id);
    }

    private void validate(Evenement evenement) {
        if (evenement.getTitre() == null || evenement.getTitre().isBlank()) {
            throw new IllegalArgumentException("Le titre de l'evenement est obligatoire.");
        }
        if (evenement.getLieu() == null || evenement.getLieu().isBlank()) {
            throw new IllegalArgumentException("Le lieu de l'evenement est obligatoire.");
        }
        if (evenement.getDateDebut() == null || evenement.getDateFin() == null) {
            throw new IllegalArgumentException("Les dates de debut et de fin sont obligatoires.");
        }
        if (evenement.getDateFin().isBefore(evenement.getDateDebut())) {
            throw new IllegalArgumentException(
                    "La date de fin ne peut pas preceder la date de debut.");
        }
    }
}