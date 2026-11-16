package fr.goat.argentic.service;

import fr.goat.argentic.dto.SeanceResponse;
import fr.goat.argentic.exception.DonneeInvalideException;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.Facture;
import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.repository.FactureRepository;
import fr.goat.argentic.repository.SeanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeanceService {

    private final SeanceRepository seanceRepository;
    private final FactureRepository factureRepository;

    // lister les séances avec filtres (période du/au, photographe, client, statut)
    public List<SeanceResponse> getSeance(LocalDate du, LocalDate au, Long idPhotographe, Long idClient, StatutSeance statutSeance) {
        if (du != null && au != null && du.isAfter(au)) {
            throw new RegleMetierException("La date de début (du) ne peut pas être postérieure à la date de fin (au)");
        }

        return seanceRepository.getSeances(du, au, idPhotographe, idClient, statutSeance).stream()
                .map(SeanceResponse::fromEntity)
                .toList();
    }

    public SeanceResponse getSeanceById(Long idSeance) {
        Seance seance = trouverSeance(idSeance);
        Facture facture = factureRepository.findBySeanceId(idSeance).orElse(null);
        return SeanceResponse.fromEntity(seance, facture);
    }

    @Transactional
    public SeanceResponse changerStatut(Long idSeance, StatutSeance nouveauStatut) {
        if (nouveauStatut == null) {
            throw new DonneeInvalideException("statut", "Le statut est obligatoire");
        }

        Seance seance = trouverSeance(idSeance);
        StatutSeance statutActuel = seance.getStatut();

        if (!statutActuel.peutPasserA(nouveauStatut)) {
            if (statutActuel.estFinal()) {
                throw new RegleMetierException("Impossible de modifier le statut d'une séance " + statutActuel
                        + " : ce statut est final.");
            }
            throw new RegleMetierException("Transition de statut invalide : impossible de passer de "
                    + statutActuel + " à " + nouveauStatut + ".");
        }

        if (nouveauStatut == StatutSeance.TERMINEE && seance.getFin().isAfter(LocalDateTime.now())) {
            throw new RegleMetierException("Impossible de passer la séance à TERMINEE : la fin de la séance n'est pas encore passée.");
        }

        seance.setStatut(nouveauStatut);
        seanceRepository.save(seance);
        Facture facture = factureRepository.findBySeanceId(idSeance).orElse(null);
        return SeanceResponse.fromEntity(seance, facture);
    }

    @Transactional
    public void deleteSeance(Long idSeance) {
        Seance seance = trouverSeance(idSeance);

        if (factureRepository.existsBySeanceId(idSeance)) {
            throw new RegleMetierException("Impossible de supprimer cette séance : une facture y est associée. Vous pouvez l'annuler à la place.");
        }

        seanceRepository.delete(seance);
    }

    private Seance trouverSeance(Long idSeance) {
        return seanceRepository.getSeanceById(idSeance)
                .orElseThrow(() -> new ResourceNotFoundException("Séance non trouvée avec l'id : " + idSeance));
    }

}
