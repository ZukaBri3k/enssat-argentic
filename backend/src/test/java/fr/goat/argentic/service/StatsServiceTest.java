package fr.goat.argentic.service;

import fr.goat.argentic.dto.TopPhotographeDureeResponse;
import fr.goat.argentic.dto.TopPhotographeSeancesResponse;
import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.StatutSeance;
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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class StatsServiceTest {

    private TestSeanceRepository seanceRepository;
    private StatsService statsService;

    @BeforeEach
    void setUp() {
        seanceRepository = new TestSeanceRepository();
        statsService = new StatsService(seanceRepository);
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

    static class TestSeanceRepository implements SeanceRepository {
        private List<TopPhotographeSeancesResponse> topSeancesResult = List.of();
        private StatutSeance capturedStatut;
        private Pageable capturedPageable;

        public void setTopSeancesResult(List<TopPhotographeSeancesResponse> topSeancesResult) {
            this.topSeancesResult = topSeancesResult;
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
            return List.of();
        }

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
}
