package fr.goat.argentic.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import fr.goat.argentic.dto.FacturePdf;
import fr.goat.argentic.dto.FactureRequest;
import fr.goat.argentic.dto.FactureResponse;
import fr.goat.argentic.dto.PaiementFactureRequest;
import fr.goat.argentic.exception.DonneeInvalideException;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.CoordonneesFacturation;
import fr.goat.argentic.model.Facture;
import fr.goat.argentic.model.PrestationFacturee;
import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.StatutPaiement;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.repository.FactureRepository;
import fr.goat.argentic.repository.SeanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FactureService {

    private static final BigDecimal TVA_DEFAUT = new BigDecimal("20.00");
    private static final BigDecimal TVA_MAX = new BigDecimal("100.00");
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final DateTimeFormatter HEURE_FR = DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE);

    private final FactureRepository factureRepository;
    private final SeanceRepository seanceRepository;

    @Transactional
    public FactureResponse genererFacture(Long idSeance, FactureRequest request) {
        if (request == null || request.montantHt() == null || request.montantHt().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DonneeInvalideException("montantHt", "Le montant HT doit être strictement supérieur à 0");
        }

        BigDecimal tauxTva = request.tauxTva() != null ? request.tauxTva() : TVA_DEFAUT;
        if (tauxTva.compareTo(BigDecimal.ZERO) < 0 || tauxTva.compareTo(TVA_MAX) > 0) {
            throw new DonneeInvalideException("tauxTva", "Le taux de TVA doit être compris entre 0 et 100 %");
        }

        Seance seance = seanceRepository.getSeanceById(idSeance)
                .orElseThrow(() -> new ResourceNotFoundException("Séance non trouvée avec l'id : " + idSeance));

        if (seance.getStatut() != StatutSeance.TERMINEE) {
            throw new RegleMetierException("Impossible de générer une facture pour une séance au statut "
                    + seance.getStatut() + " : la séance doit être TERMINEE.");
        }

        if (factureRepository.existsBySeanceId(idSeance)) {
            throw new RegleMetierException("Une facture a déjà été émise pour la séance #" + idSeance + ".");
        }

        LocalDate dateEmission = LocalDate.now();
        String numero = genererProchainNumero(dateEmission.getYear());

        CoordonneesFacturation coordonneesClient = CoordonneesFacturation.depuis(seance.getClient());
        PrestationFacturee prestation = PrestationFacturee.depuis(seance);

        Facture facture = new Facture(
                seance,
                numero,
                dateEmission,
                request.montantHt(),
                tauxTva,
                coordonneesClient,
                prestation
        );
        facture.setStatutPaiement(StatutPaiement.EN_ATTENTE);

        Facture sauvegardee = factureRepository.save(facture);
        return FactureResponse.fromEntity(sauvegardee);
    }

    public List<FactureResponse> getFactures(StatutPaiement statut, LocalDate du, LocalDate au) {
        if (du != null && au != null && du.isAfter(au)) {
            throw new RegleMetierException("La date de début (du) ne peut pas être postérieure à la date de fin (au)");
        }

        return factureRepository.getFactures(statut, du, au).stream()
                .map(FactureResponse::fromEntity)
                .toList();
    }

    public FactureResponse getFactureById(Long id) {
        return FactureResponse.fromEntity(trouverFacture(id));
    }

    /**
     * Génère le PDF de la facture uniquement à partir de ses données figées
     * (coordonnées client, prestation, montants HT/TVA/TTC, numéro, émission et statut de paiement).
     */
    public FacturePdf genererFacturePdf(Long id) {
        Facture facture = trouverFacture(id);
        byte[] pdfBytes = construireDocumentPdf(facture);
        String filename = facture.getNumero() + ".pdf";
        return new FacturePdf(facture.getNumero(), filename, pdfBytes);
    }

    @Transactional
    public FactureResponse enregistrerPaiement(Long id, PaiementFactureRequest request) {
        if (request == null || request.datePaiement() == null) {
            throw new DonneeInvalideException("datePaiement", "La date de paiement est obligatoire");
        }
        LocalDate datePaiement = request.datePaiement();

        Facture facture = trouverFacture(id);
        verifierEnAttente(facture);

        if (datePaiement.isBefore(facture.getDateEmission())) {
            throw new RegleMetierException("La date de paiement ne peut pas précéder la date d'émission de la facture ("
                    + facture.getDateEmission() + ").");
        }
        if (datePaiement.isAfter(LocalDate.now())) {
            throw new RegleMetierException("La date de paiement ne peut pas être dans le futur.");
        }

        facture.setStatutPaiement(StatutPaiement.PAYEE);
        facture.setDatePaiement(datePaiement);
        return FactureResponse.fromEntity(facture);
    }

    // Annulation définitive : la séance ne pourra pas être refacturée (1-1 strict)
    @Transactional
    public FactureResponse annulerFacture(Long id) {
        Facture facture = trouverFacture(id);
        verifierEnAttente(facture);
        facture.setStatutPaiement(StatutPaiement.ANNULEE);
        return FactureResponse.fromEntity(facture);
    }

    // Seule une facture EN_ATTENTE peut être payée ou annulée (PAYEE et ANNULEE sont finaux)
    private static void verifierEnAttente(Facture facture) {
        if (facture.getStatutPaiement() != StatutPaiement.EN_ATTENTE) {
            throw new RegleMetierException("La facture " + facture.getNumero() + " est au statut " + facture.getStatutPaiement()
                    + " : seule une facture EN_ATTENTE peut être payée ou annulée.");
        }
    }

    private Facture trouverFacture(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée avec l'id : " + id));
    }

    private String genererProchainNumero(int annee) {
        String prefixe = "FAC-" + annee + "-";
        int prochainIndex = factureRepository.findTopByNumeroStartingWithOrderByNumeroDesc(prefixe)
                .map(Facture::getNumero)
                .map(dernierNumero -> Integer.parseInt(dernierNumero.substring(prefixe.length())) + 1)
                .orElse(1);
        return "%s%04d".formatted(prefixe, prochainIndex);
    }

    private byte[] construireDocumentPdf(Facture facture) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 48, 48, 48, 48);
        PdfWriter.getInstance(document, out);
        document.open();

        Font fontMarque = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(110, 95, 85));
        Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(35, 32, 30));
        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(80, 70, 62));
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(60, 55, 50));
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(35, 32, 30));
        Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(35, 32, 30));

        // 1. En-tête Studio & Numéro de facture
        Paragraph marque = new Paragraph("STUDIO ARGENTIC", fontMarque);
        marque.setSpacingAfter(4f);
        document.add(marque);

        Paragraph titre = new Paragraph("FACTURE N° " + facture.getNumero(), fontTitre);
        titre.setSpacingAfter(14f);
        document.add(titre);

        // 2. Métadonnées : date d'émission & statut de paiement
        String dateEmissionFr = facture.getDateEmission().format(DATE_FR);
        Paragraph emissionPara = new Paragraph();
        emissionPara.add(new Phrase("Date d'émission : ", fontLabel));
        emissionPara.add(new Phrase(dateEmissionFr + " (" + facture.getDateEmission() + ")", fontValeur));
        emissionPara.setSpacingAfter(4f);
        document.add(emissionPara);

        Paragraph statutPara = new Paragraph();
        statutPara.add(new Phrase("Statut de paiement : ", fontLabel));
        statutPara.add(new Phrase(formaterStatutPaiement(facture), fontValeur));
        statutPara.setSpacingAfter(18f);
        document.add(statutPara);

        // 3. Coordonnées du client (copie figée)
        CoordonneesFacturation client = facture.getCoordonneesClient();
        Paragraph titreClient = new Paragraph("COORDONNÉES DU CLIENT", fontSection);
        titreClient.setSpacingAfter(6f);
        document.add(titreClient);

        if (client != null) {
            ajouterLigne(document, "Client : ", client.identite(), fontLabel, fontValeur);
            ajouterLigne(document, "Adresse postale : ", client.adressePostale(), fontLabel, fontValeur);
            ajouterLigne(document, "Email : ", client.email(), fontLabel, fontValeur);
            if (client.numeroSirenTva() != null && !client.numeroSirenTva().isBlank()) {
                ajouterLigne(document, "N° SIREN / TVA : ", client.numeroSirenTva(), fontLabel, fontValeur);
            }
        }
        document.add(new Paragraph(" "));

        // 4. Détail de la prestation (copie figée)
        PrestationFacturee prestation = facture.getPrestation();
        Paragraph titrePrestation = new Paragraph("DÉTAIL DE LA PRESTATION", fontSection);
        titrePrestation.setSpacingAfter(6f);
        document.add(titrePrestation);

        if (prestation != null) {
            ajouterLigne(document, "Prestation : ", prestation.typeSeance(), fontLabel, fontValeur);
            String datePrestationFr = prestation.date().format(DATE_FR);
            ajouterLigne(document, "Date de la séance : ",
                    datePrestationFr + " (" + prestation.date() + ")", fontLabel, fontValeur);
            ajouterLigne(document, "Heure de début : ", prestation.heureDebut().format(HEURE_FR), fontLabel, fontValeur);
            ajouterLigne(document, "Durée : ", prestation.dureeMinutes() + " min", fontLabel, fontValeur);
            ajouterLigne(document, "Photographe : ", prestation.nomPhotographe(), fontLabel, fontValeur);
        }
        document.add(new Paragraph(" "));

        // 5. Récapitulatif financier (HT, TVA, TTC)
        Paragraph titreMontants = new Paragraph("RÉCAPITULATIF FINANCIER", fontSection);
        titreMontants.setSpacingAfter(8f);
        document.add(titreMontants);

        BigDecimal ht = facture.getMontantHt().setScale(2, RoundingMode.HALF_UP);
        BigDecimal tauxTva = facture.getTauxTva().setScale(2, RoundingMode.HALF_UP);
        BigDecimal ttc = facture.getMontantTtc().setScale(2, RoundingMode.HALF_UP);
        BigDecimal montantTva = ttc.subtract(ht).setScale(2, RoundingMode.HALF_UP);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setSpacingAfter(16f);

        ajouterCelluleMontant(table, "Montant HT", formaterMontant(ht), fontLabel, fontValeur, false);
        ajouterCelluleMontant(table, "TVA (" + formaterPourcentage(tauxTva) + ")", formaterMontant(montantTva), fontLabel, fontValeur, false);
        ajouterCelluleMontant(table, "Montant TTC", formaterMontant(ttc), fontTotal, fontTotal, true);

        document.add(table);

        document.close();
        return out.toByteArray();
    }

    private static void ajouterLigne(Document document, String label, String valeur, Font fontLabel, Font fontValeur) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(label, fontLabel));
        p.add(new Phrase(valeur != null ? valeur : "—", fontValeur));
        p.setSpacingAfter(3f);
        document.add(p);
    }

    private static void ajouterCelluleMontant(PdfPTable table, String libelle, String montant,
                                              Font fontLibelle, Font fontMontant, boolean estTotal) {
        Color fond = estTotal ? new Color(242, 238, 233) : Color.WHITE;
        int bordure = estTotal ? (Rectangle.TOP | Rectangle.BOTTOM) : Rectangle.BOTTOM;

        PdfPCell cellLibelle = new PdfPCell(new Phrase(libelle, fontLibelle));
        cellLibelle.setBorder(bordure);
        cellLibelle.setBorderColor(new Color(210, 205, 198));
        cellLibelle.setBackgroundColor(fond);
        cellLibelle.setPadding(8f);
        table.addCell(cellLibelle);

        PdfPCell cellMontant = new PdfPCell(new Phrase(montant, fontMontant));
        cellMontant.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellMontant.setBorder(bordure);
        cellMontant.setBorderColor(new Color(210, 205, 198));
        cellMontant.setBackgroundColor(fond);
        cellMontant.setPadding(8f);
        table.addCell(cellMontant);
    }

    private static String formaterStatutPaiement(Facture facture) {
        StatutPaiement statut = facture.getStatutPaiement();
        return switch (statut) {
            case PAYEE -> facture.getDatePaiement() != null
                    ? "PAYEE (Payée le " + facture.getDatePaiement().format(DATE_FR) + " / " + facture.getDatePaiement() + ")"
                    : "PAYEE (Payée)";
            case ANNULEE -> "ANNULEE (Facture annulée)";
            case EN_ATTENTE -> "EN_ATTENTE (En attente de règlement)";
        };
    }

    private static String formaterMontant(BigDecimal montant) {
        String fr = montant.toPlainString().replace('.', ',') + " €";
        return fr + " (" + montant.toPlainString() + " EUR)";
    }

    private static String formaterPourcentage(BigDecimal taux) {
        return taux.toPlainString().replace('.', ',') + " % (" + taux.toPlainString() + "%)";
    }
}
