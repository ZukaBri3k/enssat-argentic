package fr.goat.argentic.service;

import fr.goat.argentic.dto.FactureRequest;
import fr.goat.argentic.dto.FactureResponse;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FactureService {

    private static final BigDecimal TVA_DEFAUT = new BigDecimal("20.00");
    private static final BigDecimal TVA_MAX = new BigDecimal("100.00");

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
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée avec l'id : " + id));
        return FactureResponse.fromEntity(facture);
    }

    private String genererProchainNumero(int annee) {
        String prefixe = "FAC-" + annee + "-";
        int prochainIndex = factureRepository.findTopByNumeroStartingWithOrderByNumeroDesc(prefixe)
                .map(Facture::getNumero)
                .map(dernierNumero -> Integer.parseInt(dernierNumero.substring(prefixe.length())) + 1)
                .orElse(1);
        return "%s%04d".formatted(prefixe, prochainIndex);
    }
}
