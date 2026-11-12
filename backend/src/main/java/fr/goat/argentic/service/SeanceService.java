package fr.goat.argentic.service;

import fr.goat.argentic.dto.SeanceResponse;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.repository.SeanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeanceService {

    private final SeanceRepository seanceRepository;

    // lister les séances avec filtres (période du/au, photographe, client, statut)
    public List<SeanceResponse> getSeance(LocalDate du, LocalDate au, Long idPhotographe, Long idClient, StatutSeance statutSeance) {
        if (du != null && au != null && du.isAfter(au)) {
            throw new RegleMetierException("La date de début (du) ne peut pas être postérieure à la date de fin (au)");
        }

        return seanceRepository.getSeances(du, au, idPhotographe, idClient, statutSeance).stream()
                .map(SeanceResponse::fromEntity)
                .toList();
    }

    // TODO: récupérer une séance par son id (ou lever ResourceNotFoundException)

}
