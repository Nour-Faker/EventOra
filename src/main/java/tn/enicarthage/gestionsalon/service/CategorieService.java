package tn.enicarthage.gestionSalon.service;

import tn.enicarthage.gestionSalon.dao.CategorieDAO;
import tn.enicarthage.gestionSalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionSalon.model.Categorie;

import java.util.List;

public class CategorieService {

    private final CategorieDAO categorieDAO;

    public CategorieService() {
        this.categorieDAO = new CategorieDAO();
    }

    public void createCategorie(Categorie categorie) {
        validate(categorie);
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
        categorieDAO.update(categorie);
    }

    public void deleteCategorie(int id) {
        getCategorieById(id);
        categorieDAO.delete(id);
    }

    private void validate(Categorie categorie) {
        if (categorie.getNom() == null || categorie.getNom().isBlank()) {
            throw new IllegalArgumentException("Le nom de la categorie est obligatoire.");
        }
    }
}