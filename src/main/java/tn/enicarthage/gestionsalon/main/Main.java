package tn.enicarthage.gestionsalon.main;

import tn.enicarthage.gestionsalon.dao.EvenementDAO;
import tn.enicarthage.gestionsalon.model.Evenement;

import java.time.LocalDate;

public class Main {

    public static void main(String[] args) {
        EvenementDAO dao = new EvenementDAO();

        // CREATE
        Evenement e = new Evenement();
        e.setTitre("Evenement test");
        e.setDescription("Evenement de test");
        e.setDateDebut(LocalDate.of(2026, 12, 20));
        e.setDateFin(LocalDate.of(2026, 12, 21));
        e.setLieu("Sfax");
        dao.create(e);
        System.out.println("Cree, id = " + e.getId());

        // READ
        System.out.println("--- Evenements ---");
        for (Evenement ev : dao.findAll()) {
            System.out.println(ev.getId() + " - " + ev.getTitre()
                    + " (" + ev.getDateDebut() + " -> " + ev.getDateFin() + ")");
        }

        // UPDATE
        e.setLieu("Sousse");
        dao.update(e);
        System.out.println("Apres modification : lieu = "
                + dao.findById(e.getId()).getLieu());

        // DELETE
        dao.delete(e.getId());
        System.out.println("Apres suppression : " + dao.findById(e.getId()));
    }
}