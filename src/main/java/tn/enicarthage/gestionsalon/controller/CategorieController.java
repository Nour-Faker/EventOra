package tn.enicarthage.gestionsalon.controller;

import tn.enicarthage.gestionsalon.dto.CategorieRequest;
import tn.enicarthage.gestionsalon.model.Categorie;
import tn.enicarthage.gestionsalon.service.CategorieService;

import java.util.List;

public class CategorieController {

    private final CategorieService service = new CategorieService();

    public List<Categorie> lister() { return service.getAllCategories(); }

    public Categorie obtenir(int id) { return service.getCategorieById(id); }

    public Categorie creer(CategorieRequest r) {
        Categorie c = new Categorie();
        c.setNom(r.nom());
        c.setDescription(r.description());
        service.createCategorie(c);
        return c;
    }

    public Categorie modifier(int id, CategorieRequest r) {
        Categorie c = service.getCategorieById(id);
        c.setNom(r.nom());
        c.setDescription(r.description());
        service.updateCategorie(c);
        return c;
    }

    public void supprimer(int id) { service.deleteCategorie(id); }
}
