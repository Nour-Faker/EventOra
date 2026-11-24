package tn.enicarthage.gestionsalon.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * PDF de confirmation d'un billet (BF-32) : informations du billet et code QR.
 * Le code QR est dessine en carres vectoriels : net a l'impression, sans image intermediaire.
 */
public final class BilletPdf {

    private static final float TAILLE_QR = 180f; // en points (1 pt = 1/72 pouce)

    private BilletPdf() {}

    public static byte[] generer(String codeQr, String titreEvenement, String lieu, LocalDate date,
                                 String typeBillet, BigDecimal prixPaye, String visiteur, String statut) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.addTitle("Billet " + codeQr);
            doc.open();

            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
            Font libelle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font valeur = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font code = FontFactory.getFont(FontFactory.COURIER_BOLD, 12);

            Paragraph p = new Paragraph("EventOra - Billet d'entree", titre);
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);
            p = new Paragraph(titreEvenement, sousTitre);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingAfter(14);
            doc.add(p);

            PdfPTable infos = new PdfPTable(new float[]{1f, 2f});
            infos.setWidthPercentage(100);
            ligne(infos, "Visiteur", visiteur, libelle, valeur);
            ligne(infos, "Lieu", lieu, libelle, valeur);
            ligne(infos, "Date", date != null ? date.toString() : "-", libelle, valeur);
            ligne(infos, "Type de billet", typeBillet, libelle, valeur);
            ligne(infos, "Prix paye", prixPaye != null ? prixPaye.toPlainString() + " TND" : "-", libelle, valeur);
            ligne(infos, "Statut", statut, libelle, valeur);
            infos.setSpacingAfter(TAILLE_QR + 24);
            doc.add(infos);

            // Code QR centre sous le tableau
            float x = (doc.getPageSize().getWidth() - TAILLE_QR) / 2;
            float y = writer.getVerticalPosition(true) + 12;
            dessinerQr(writer.getDirectContent(), codeQr, x, y, TAILLE_QR);

            p = new Paragraph(codeQr, code);
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);
            p = new Paragraph("Presentez ce code a l'entree. Un billet ne peut etre valide qu'une seule fois.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY));
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);
        } catch (DocumentException e) {
            throw new IllegalStateException("Generation du PDF impossible", e);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
        return out.toByteArray();
    }

    private static void ligne(PdfPTable table, String libelle, String valeur, Font fLibelle, Font fValeur) {
        PdfPCell c1 = new PdfPCell(new Paragraph(libelle, fLibelle));
        PdfPCell c2 = new PdfPCell(new Paragraph(valeur != null ? valeur : "-", fValeur));
        c1.setPadding(5);
        c2.setPadding(5);
        c1.setBackgroundColor(new Color(240, 240, 240));
        table.addCell(c1);
        table.addCell(c2);
    }

    // (x, y) = coin bas-gauche du carre du code QR, en points
    private static void dessinerQr(PdfContentByte cb, String texte, float x, float y, float taille) {
        BitMatrix m;
        try {
            m = new QRCodeWriter().encode(texte, BarcodeFormat.QR_CODE, 0, 0,
                    // Version 3 + correction Q : lisible par les lecteurs bases sur ZXing (la version 1
                    // en correction M est mal detectee) et tolerant a ~25 % de degradation (impression)
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q,
                            EncodeHintType.QR_VERSION, 3,
                            EncodeHintType.MARGIN, 4)); // zone blanche de 4 modules (norme QR)
        } catch (WriterException e) {
            throw new IllegalStateException("Generation du code QR impossible", e);
        }
        int n = m.getWidth();
        // Module en nombre entier de points : bords nets, lecture fiable meme en basse resolution
        float module = Math.max(1f, (float) Math.floor(taille / n));
        x += (taille - module * n) / 2; // recentre le code dans la zone prevue
        y += (taille - module * n) / 2;
        cb.saveState();
        cb.setColorFill(Color.BLACK);
        for (int ligne = 0; ligne < n; ligne++) {
            for (int col = 0; col < n; col++) {
                if (m.get(col, ligne)) {
                    // l'axe Y du PDF monte : la ligne 0 du QR est en haut
                    cb.rectangle(x + col * module, y + (n - 1 - ligne) * module, module, module);
                }
            }
        }
        cb.fill();
        cb.restoreState();
    }
}
