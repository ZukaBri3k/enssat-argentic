package fr.goat.argentic.repository;

import fr.goat.argentic.model.Photographe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotographeRepository extends JpaRepository<Photographe, Long> {

    // Les spécialités sont chargées dans la même requête pour éviter le N+1
    @EntityGraph(attributePaths = "specialites")
    List<Photographe> findAllByOrderByNomAsc();

    @EntityGraph(attributePaths = "specialites")
    List<Photographe> findByActifOrderByNomAsc(boolean actif);
}
