package fr.goat.argentic.service;

import fr.goat.argentic.dto.ChiffreAffairesResponse;
import fr.goat.argentic.dto.TopPhotographeDureeResponse;
import fr.goat.argentic.dto.TopPhotographeSeancesResponse;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.model.Facture;
import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.Specialite;
import fr.goat.argentic.model.StatutPaiement;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.repository.FactureRepository;
import fr.goat.argentic.repository.SeanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatsServiceTest {

    private TestSeanceRepository seanceRepository;
    private TestFactureRepository factureRepository;
    private StatsService statsService;

    @BeforeEach
    void setUp() {
        seanceRepository = new TestSeanceRepository();
        factureRepository = new TestFactureRepository();
        statsService = new StatsService(seanceRepository, factureRepository);
    }

    @Test
    @DisplayName("getTopPhotographesParSeances filtre sur TERMINEE et limite au Top 5")
    void getTopPhotographesParSeances_doitFiltrerTermineeEtLimiterTop5() {
        List<TopPhotographeSeancesResponse> attendu = List.of(
                new TopPhotographeSeancesResponse(5L, "Léa Moreau", "lea.moreau@argentic.fr", 5L),
                new TopPhotographeSeancesResponse(1L, "Camille Martin", "camille.martin@argentic.fr", 4L)
        );
        seanceRepository.setTopSeancesResult(attendu);

        List<TopPhotographeSeancesResponse> result = statsService.getTopPhotographesParSeances();

        assertThat(result).isEqualTo(attendu);
        assertThat(seanceRepository.getCapturedStatut()).isEqualTo(StatutSeance.TERMINEE);
        assertThat(seanceRepository.getCapturedPageable()).isEqualTo(PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("getTopPhotographesParDuree filtre sur TERMINEE et limite au Top 5")
    void getTopPhotographesParDuree_doitFiltrerTermineeEtLimiterTop5() {
        List<TopPhotographeDureeResponse> attendu = List.of(
                new TopPhotographeDureeResponse(2L, "Lucas Bernard", "lucas.bernard@argentic.fr", 1440L),
                new TopPhotographeDureeResponse(5L, "Léa Moreau", "lea.moreau@argentic.fr", 810L)
        );
        seanceRepository.setTopDureeResult(attendu);

        List<TopPhotographeDureeResponse> result = statsService.getTopPhotographesParDuree();

        assertThat(result).isEqualTo(attendu);
        assertThat(seanceRepository.getCapturedStatut()).isEqualTo(StatutSeance.TERMINEE);
        assertThat(seanceRepository.getCapturedPageable()).isEqualTo(PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("getChiffreAffaires calcule les montants HT et TTC pour les factures PAYEE sur la période")
    void getChiffreAffaires_periodeValide_doitCalculerHtEtTtc() {
        LocalDate du = LocalDate.of(2026, 1, 1);
        LocalDate au = LocalDate.of(2026, 12, 31);

        factureRepository.setMontantHt(new BigDecimal("10530.00"));
        factureRepository.setMontantTtc(new BigDecimal("12636.00"));

        ChiffreAffairesResponse res = statsService.getChiffreAffaires(du, au);

        assertThat(res.du()).isEqualTo(du);
        assertThat(res.au()).isEqualTo(au);
        assertThat(res.montantHt()).isEqualByComparingTo("10530.00");
        assertThat(res.montantTtc()).isEqualByComparingTo("12636.00");

        assertThat(factureRepository.getCapturedStatut()).isEqualTo(StatutPaiement.PAYEE);
        assertThat(factureRepository.getCapturedDu()).isEqualTo(du);
        assertThat(factureRepository.getCapturedAu()).isEqualTo(au);
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

    static class TestSeanceRepository implements SeanceRepository {
        private List<TopPhotographeSeancesResponse> topSeancesResult = List.of();
        private List<TopPhotographeDureeResponse> topDureeResult = List.of();
        private StatutSeance capturedStatut;
        private Pageable capturedPageable;

        public void setTopSeancesResult(List<TopPhotographeSeancesResponse> topSeancesResult) {
            this.topSeancesResult = topSeancesResult;
        }

        public void setTopDureeResult(List<TopPhotographeDureeResponse> topDureeResult) {
            this.topDureeResult = topDureeResult;
        }

        public StatutSeance getCapturedStatut() {
            return capturedStatut;
        }

        public Pageable getCapturedPageable() {
            return capturedPageable;
        }

        @Override
        public List<TopPhotographeSeancesResponse> findTopPhotographesBySeances(StatutSeance statut, Pageable pageable) {
            this.capturedStatut = statut;
            this.capturedPageable = pageable;
            return topSeancesResult;
        }

        @Override
        public List<TopPhotographeDureeResponse> findTopPhotographesByDuree(StatutSeance statut, Pageable pageable) {
            this.capturedStatut = statut;
            this.capturedPageable = pageable;
            return topDureeResult;
        }

        @Override public boolean existsByPhotographeId(Long photographeId) { return false; }
        @Override public List<Specialite> findSpecialitesAvecSeancesAVenir(Long photographeId, Collection<Long> specialiteIds, LocalDate date, LocalTime heure) { return List.of(); }
        @Override public void flush() {}
        @Override public <S extends Seance> S saveAndFlush(S entity) { return entity; }
        @Override public <S extends Seance> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<Seance> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Seance getOne(Long aLong) { return null; }
        @Override public Seance getById(Long aLong) { return null; }
        @Override public Seance getReferenceById(Long aLong) { return null; }
        @Override public <S extends Seance> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Seance> List<S> findAll(Example<S> example) { return List.of(); }
        @Override public <S extends Seance> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends Seance> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends Seance> long count(Example<S> example) { return 0; }
        @Override public <S extends Seance> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Seance, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Seance> S save(S entity) { return entity; }
        @Override public <S extends Seance> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public Optional<Seance> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Seance> findAll() { return List.of(); }
        @Override public List<Seance> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Seance entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Seance> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Seance> findAll(Sort sort) { return List.of(); }
        @Override public Page<Seance> findAll(Pageable pageable) { return Page.empty(); }
    }

    static class TestFactureRepository implements FactureRepository {
        private BigDecimal montantHt = BigDecimal.ZERO;
        private BigDecimal montantTtc = BigDecimal.ZERO;
        private StatutPaiement capturedStatut;
        private LocalDate capturedDu;
        private LocalDate capturedAu;

        public void setMontantHt(BigDecimal montantHt) {
            this.montantHt = montantHt;
        }

        public void setMontantTtc(BigDecimal montantTtc) {
            this.montantTtc = montantTtc;
        }

        public StatutPaiement getCapturedStatut() {
            return capturedStatut;
        }

        public LocalDate getCapturedDu() {
            return capturedDu;
        }

        public LocalDate getCapturedAu() {
            return capturedAu;
        }

        @Override
        public BigDecimal sumMontantHtByStatutAndPeriode(StatutPaiement statut, LocalDate du, LocalDate au) {
            this.capturedStatut = statut;
            this.capturedDu = du;
            this.capturedAu = au;
            return montantHt;
        }

        @Override
        public BigDecimal sumMontantTtcByStatutAndPeriode(StatutPaiement statut, LocalDate du, LocalDate au) {
            this.capturedStatut = statut;
            this.capturedDu = du;
            this.capturedAu = au;
            return montantTtc;
        }

        @Override public void flush() {}
        @Override public <S extends Facture> S saveAndFlush(S entity) { return entity; }
        @Override public <S extends Facture> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<Facture> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Facture getOne(Long aLong) { return null; }
        @Override public Facture getById(Long aLong) { return null; }
        @Override public Facture getReferenceById(Long aLong) { return null; }
        @Override public <S extends Facture> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Facture> List<S> findAll(Example<S> example) { return List.of(); }
        @Override public <S extends Facture> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends Facture> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends Facture> long count(Example<S> example) { return 0; }
        @Override public <S extends Facture> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Facture, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Facture> S save(S entity) { return entity; }
        @Override public <S extends Facture> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public Optional<Facture> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Facture> findAll() { return List.of(); }
        @Override public List<Facture> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Facture entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Facture> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Facture> findAll(Sort sort) { return List.of(); }
        @Override public Page<Facture> findAll(Pageable pageable) { return Page.empty(); }
    }
}
