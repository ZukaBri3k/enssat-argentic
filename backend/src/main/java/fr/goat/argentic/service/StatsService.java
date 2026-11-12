package fr.goat.argentic.service;

import fr.goat.argentic.dto.ChiffreAffairesResponse;
import fr.goat.argentic.dto.TopPhotographeDureeResponse;
import fr.goat.argentic.dto.TopPhotographeSeancesResponse;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.model.StatutPaiement;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.repository.FactureRepository;
import fr.goat.argentic.repository.SeanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private static final int TOP_LIMIT = 5;

    private final SeanceRepository seanceRepository;
    private final FactureRepository factureRepository;

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

    public ChiffreAffairesResponse getChiffreAffaires(LocalDate du, LocalDate au) {
        if (du != null && au != null && du.isAfter(au)) {
            throw new RegleMetierException("La date de début (du) ne peut pas être postérieure à la date de fin (au)");
        }

        BigDecimal ht = factureRepository.sumMontantHtByStatutAndPeriode(StatutPaiement.PAYEE, du, au);
        BigDecimal ttc = factureRepository.sumMontantTtcByStatutAndPeriode(StatutPaiement.PAYEE, du, au);

        return new ChiffreAffairesResponse(
                du,
                au,
                ht != null ? ht.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                ttc != null ? ttc.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        );
    }
}
