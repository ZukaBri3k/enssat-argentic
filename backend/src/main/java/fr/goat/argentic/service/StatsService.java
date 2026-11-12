package fr.goat.argentic.service;

import fr.goat.argentic.dto.TopPhotographeDureeResponse;
import fr.goat.argentic.dto.TopPhotographeSeancesResponse;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.repository.SeanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private static final int TOP_LIMIT = 5;

    private final SeanceRepository seanceRepository;

    public List<TopPhotographeSeancesResponse> getTopPhotographesParSeances() {
        return seanceRepository.findTopPhotographesBySeances(
                StatutSeance.TERMINEE,
                PageRequest.of(0, TOP_LIMIT)
        );
    }

    public List<TopPhotographeDureeResponse> getTopPhotographesParDuree() {
        return seanceRepository.findTopPhotographesByDuree(
                StatutSeance.TERMINEE,
                PageRequest.of(0, TOP_LIMIT)
        );
    }
}
