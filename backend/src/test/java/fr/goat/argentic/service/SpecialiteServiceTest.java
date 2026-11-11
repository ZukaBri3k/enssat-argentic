package fr.goat.argentic.service;

import fr.goat.argentic.dto.SpecialiteResponse;
import fr.goat.argentic.model.Specialite;
import fr.goat.argentic.repository.SpecialiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class SpecialiteServiceTest {

    private TestSpecialiteRepository specialiteRepository;
    private SpecialiteService specialiteService;

    @BeforeEach
    void setUp() {
        specialiteRepository = new TestSpecialiteRepository();
        specialiteService = new SpecialiteService(specialiteRepository);
    }

    @Test
    @DisplayName("findAll retourne toutes les spécialités mappées en DTO")
    void findAll_doitRetournerListeSpecialites() {
        Specialite s1 = new Specialite("Reportage");
        s1.setId(1L);
        Specialite s2 = new Specialite("Mariage et fêtes");
        s2.setId(2L);
        Specialite s3 = new Specialite("Famille");
        s3.setId(3L);
        specialiteRepository.setData(List.of(s1, s2, s3));

        List<SpecialiteResponse> result = specialiteService.findAll();

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(new SpecialiteResponse(1L, "Reportage"));
        assertThat(result.get(1)).isEqualTo(new SpecialiteResponse(2L, "Mariage et fêtes"));
        assertThat(result.get(2)).isEqualTo(new SpecialiteResponse(3L, "Famille"));
    }

    static class TestSpecialiteRepository implements SpecialiteRepository {
        private List<Specialite> data = new ArrayList<>();

        public void setData(List<Specialite> data) {
            this.data = data;
        }

        @Override
        public List<Specialite> findAll() {
            return data;
        }

        @Override public void flush() {}
        @Override public <S extends Specialite> S saveAndFlush(S entity) { return entity; }
        @Override public <S extends Specialite> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<Specialite> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Specialite getOne(Long aLong) { return null; }
        @Override public Specialite getById(Long aLong) { return null; }
        @Override public Specialite getReferenceById(Long aLong) { return null; }
        @Override public <S extends Specialite> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Specialite> List<S> findAll(Example<S> example) { return List.of(); }
        @Override public <S extends Specialite> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends Specialite> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends Specialite> long count(Example<S> example) { return 0; }
        @Override public <S extends Specialite> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Specialite, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Specialite> S save(S entity) { return entity; }
        @Override public <S extends Specialite> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public Optional<Specialite> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Specialite> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public long count() { return data.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Specialite entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Specialite> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Specialite> findAll(Sort sort) { return data; }
        @Override public Page<Specialite> findAll(Pageable pageable) { return Page.empty(); }
    }
}
