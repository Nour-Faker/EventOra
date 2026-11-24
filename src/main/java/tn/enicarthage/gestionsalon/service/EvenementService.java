package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.dao.BilletDAO;
import tn.enicarthage.gestionsalon.dao.EvenementDAO;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.Evenement;

import java.util.List;

public class EvenementService {

    private final EvenementDAO evenementDAO;
    private final BilletDAO billetDAO = new BilletDAO();

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
        // billet -> type_billet est en RESTRICT : on renvoie un 409 clair au lieu d'une erreur SQL
        if (billetDAO.compterParEvenement(id) > 0) {
            throw new ConflictException("Des billets ont ete vendus pour cet evenement : suppression impossible.");
        }
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