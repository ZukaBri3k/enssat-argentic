package fr.goat.argentic.service;

import fr.goat.argentic.dto.ChiffreAffairesResponse;
import fr.goat.argentic.dto.TopPhotographeDureeResponse;
import fr.goat.argentic.dto.TopPhotographeSeancesResponse;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.model.StatutPaiement;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.repository.FactureRepository;
import fr.goat.argentic.repository.SeanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// Les stubs portent les arguments attendus : en strict stubs, un appel avec d'autres arguments fait échouer le test
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private SeanceRepository seanceRepository;

    @Mock
    private FactureRepository factureRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    @DisplayName("getTopPhotographesParSeances filtre sur TERMINEE et limite au Top 5")
    void getTopPhotographesParSeances_doitFiltrerTermineeEtLimiterTop5() {
        List<TopPhotographeSeancesResponse> attendu = List.of(
                new TopPhotographeSeancesResponse(5L, "Léa Moreau", "lea.moreau@argentic.fr", 5L),
                new TopPhotographeSeancesResponse(1L, "Camille Martin", "camille.martin@argentic.fr", 4L)
        );
        when(seanceRepository.findTopPhotographesBySeances(StatutSeance.TERMINEE, PageRequest.of(0, 5)))
                .thenReturn(attendu);

        List<TopPhotographeSeancesResponse> result = statsService.getTopPhotographesParSeances();

        assertThat(result).isEqualTo(attendu);
    }

    @Test
    @DisplayName("getTopPhotographesParDuree filtre sur TERMINEE et limite au Top 5")
    void getTopPhotographesParDuree_doitFiltrerTermineeEtLimiterTop5() {
        List<TopPhotographeDureeResponse> attendu = List.of(
                new TopPhotographeDureeResponse(2L, "Lucas Bernard", "lucas.bernard@argentic.fr", 1440L),
                new TopPhotographeDureeResponse(5L, "Léa Moreau", "lea.moreau@argentic.fr", 810L)
        );
        when(seanceRepository.findTopPhotographesByDuree(StatutSeance.TERMINEE, PageRequest.of(0, 5)))
                .thenReturn(attendu);

        List<TopPhotographeDureeResponse> result = statsService.getTopPhotographesParDuree();

        assertThat(result).isEqualTo(attendu);
    }

    @Test
    @DisplayName("getChiffreAffaires calcule les montants HT et TTC pour les factures PAYEE sur la période")
    void getChiffreAffaires_periodeValide_doitCalculerHtEtTtc() {
        LocalDate du = LocalDate.of(2026, 1, 1);
        LocalDate au = LocalDate.of(2026, 12, 31);
        when(factureRepository.sumMontantHtByStatutAndPeriode(StatutPaiement.PAYEE, du, au))
                .thenReturn(new BigDecimal("10530.00"));
        when(factureRepository.sumMontantTtcByStatutAndPeriode(StatutPaiement.PAYEE, du, au))
                .thenReturn(new BigDecimal("12636.00"));

        ChiffreAffairesResponse res = statsService.getChiffreAffaires(du, au);

        assertThat(res.du()).isEqualTo(du);
        assertThat(res.au()).isEqualTo(au);
        assertThat(res.montantHt()).isEqualByComparingTo("10530.00");
        assertThat(res.montantTtc()).isEqualByComparingTo("12636.00");
    }

    @Test
    @DisplayName("getChiffreAffaires lève RegleMetierException si du est après au")
    void getChiffreAffaires_duApresAu_doitLeverException() {
        LocalDate du = LocalDate.of(2026, 12, 31);
        LocalDate au = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> statsService.getChiffreAffaires(du, au))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("postérieure");
    }
}
