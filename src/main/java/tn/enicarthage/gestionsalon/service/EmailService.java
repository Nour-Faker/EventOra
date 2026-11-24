package tn.enicarthage.gestionsalon.service;

import com.google.gson.Gson;
import tn.enicarthage.gestionsalon.exception.ServiceIndisponibleException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Envoi d'emails avec l'API Resend (https://resend.com).
 * - RESEND_API_KEY : cle de l'API (obligatoire pour un vrai envoi, jamais ecrite dans le code).
 * - RESEND_FROM    : expediteur, par defaut "EventOra <onboarding@resend.dev>".
 *   Avec l'adresse de test onboarding@resend.dev, Resend n'envoie qu'a l'adresse du compte Resend ;
 *   pour envoyer a tout le monde, il faut verifier un domaine dans Resend.
 * Sans cle (developpement, demo hors ligne), rien n'est envoye : le code est ecrit dans la console.
 */
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static final String API_URL = "https://api.resend.com/emails";
    private static final Gson GSON = new Gson();

    private static final String API_KEY = System.getenv("RESEND_API_KEY");
    private final String apiKey = API_KEY;
    private final String expediteur = System.getenv().getOrDefault("RESEND_FROM", "EventOra <onboarding@resend.dev>");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /**
     * Les fonctions par email (verification a l'inscription, mot de passe oublie) ne sont actives
     * que si RESEND_API_KEY est defini. Sans cle : l'inscription connecte directement.
     */
    public static boolean actif() {
        return API_KEY != null && !API_KEY.isBlank();
    }

    public void envoyerCodeVerification(String email, String prenom, String code, int minutes) {
        envoyer(email, "Votre code de verification EventOra : " + code,
                gabarit(prenom, "Confirmez votre adresse email",
                        "Bienvenue sur EventOra ! Saisissez ce code pour activer votre compte :",
                        code, minutes,
                        "Si vous n'avez pas cree de compte, ignorez simplement cet email."),
                "verification", code);
    }

    public void envoyerCodeReinitialisation(String email, String prenom, String code, int minutes) {
        envoyer(email, "Reinitialisation de votre mot de passe EventOra",
                gabarit(prenom, "Reinitialisez votre mot de passe",
                        "Vous avez demande a changer votre mot de passe. Saisissez ce code :",
                        code, minutes,
                        "Si vous n'etes pas a l'origine de cette demande, ignorez cet email : "
                                + "votre mot de passe actuel reste valable."),
                "reinitialisation", code);
    }

    private void envoyer(String destinataire, String sujet, String html, String usage, String code) {
        if (apiKey == null || apiKey.isBlank()) {
            LOGGER.warning("RESEND_API_KEY non defini : email non envoye. Code de " + usage
                    + " pour " + destinataire + " : " + code);
            return;
        }
        String corps = GSON.toJson(Map.of(
                "from", expediteur,
                "to", List.of(destinataire),
                "subject", sujet,
                "html", html));
        HttpRequest requete = HttpRequest.newBuilder(URI.create(API_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corps, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> reponse = http.send(requete, HttpResponse.BodyHandlers.ofString());
            if (reponse.statusCode() / 100 != 2) {
                LOGGER.warning("Resend a refuse l'email (" + reponse.statusCode() + ") : " + reponse.body());
                throw new ServiceIndisponibleException("L'email n'a pas pu etre envoye. Reessayez dans quelques instants.");
            }
        } catch (IOException e) {
            LOGGER.warning("Resend injoignable : " + e.getMessage());
            throw new ServiceIndisponibleException("Le service d'email est injoignable. Reessayez dans quelques instants.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceIndisponibleException("Envoi de l'email interrompu.");
        }
    }

    // Email HTML compatible avec les messageries (tableaux et styles en ligne)
    private static String gabarit(String prenom, String titre, String intro, String code, int minutes, String note) {
        return """
            <!doctype html><html lang="fr"><body style="margin:0;padding:0;background:#0b0a1f;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#0b0a1f;padding:32px 12px;">
            <tr><td align="center">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#13112e;border-radius:20px;overflow:hidden;border:1px solid #2a2650;">
                <tr><td style="background:linear-gradient(120deg,#ff7a59,#ff4d8d 50%%,#7c5cff);background-color:#ff4d8d;padding:26px 32px;">
                  <span style="font-family:Arial,sans-serif;font-size:22px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">EventOra</span>
                </td></tr>
                <tr><td style="padding:32px;font-family:Arial,sans-serif;color:#f4f2ff;">
                  <h1 style="margin:0 0 12px;font-size:22px;">%s</h1>
                  <p style="margin:0 0 6px;color:#c9c4ee;font-size:15px;">Bonjour %s,</p>
                  <p style="margin:0 0 24px;color:#c9c4ee;font-size:15px;line-height:1.5;">%s</p>
                  <div style="text-align:center;margin:0 0 24px;">
                    <span style="display:inline-block;padding:16px 26px;border-radius:14px;background:#0b0a1f;border:1px solid #3a3570;font-family:'Courier New',monospace;font-size:34px;font-weight:700;letter-spacing:10px;color:#ffffff;">%s</span>
                  </div>
                  <p style="margin:0 0 8px;color:#a7a2c9;font-size:13px;">Ce code expire dans %d minutes.</p>
                  <p style="margin:0;color:#a7a2c9;font-size:13px;">%s</p>
                </td></tr>
              </table>
              <p style="font-family:Arial,sans-serif;color:#6f6a96;font-size:12px;margin:18px 0 0;">EventOra · Salons &amp; festivals</p>
            </td></tr></table></body></html>
            """.formatted(echapper(titre), echapper(prenom), echapper(intro), code, minutes, echapper(note));
    }

    private static String echapper(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
