package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.dao.CategorieDAO;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.Categorie;

import java.util.List;

public class CategorieService {

    private final CategorieDAO categorieDAO;

    public CategorieService() {
        this.categorieDAO = new CategorieDAO();
    }

    public void createCategorie(Categorie categorie) {
        validate(categorie);
        verifierNomUnique(categorie.getNom(), 0);
        categorieDAO.create(categorie);
    }

    public List<Categorie> getAllCategories() {
        return categorieDAO.findAll();
    }

    public Categorie getCategorieById(int id) {
        Categorie categorie = categorieDAO.findById(id);
        if (categorie == null) {
            throw new ResourceNotFoundException("Categorie introuvable : " + id);
        }
        return categorie;
    }

    public void updateCategorie(Categorie categorie) {
        getCategorieById(categorie.getId()); // verifie que la categorie existe
        validate(categorie);
        verifierNomUnique(categorie.getNom(), categorie.getId());
        categorieDAO.update(categorie);
    }

    public void deleteCategorie(int id) {
        getCategorieById(id);
        // BF-10 : refusee si des stands utilisent la categorie
        if (categorieDAO.estUtilisee(id)) {
            throw new ConflictException("Cette categorie est utilisee par des stands : suppression impossible.");
        }
        categorieDAO.delete(id);
    }

    private void verifierNomUnique(String nom, int exceptId) {
        if (categorieDAO.existeNom(nom, exceptId)) {
            throw new ConflictException("Une categorie porte deja ce nom.");
        }
    }

    private void validate(Categorie categorie) {
        if (categorie.getNom() == null || categorie.getNom().isBlank()) {
            throw new IllegalArgumentException("Le nom de la categorie est obligatoire.");
        }
        categorie.setNom(categorie.getNom().trim());
    }
}