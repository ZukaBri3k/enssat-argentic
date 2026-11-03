package fr.goat.argentic.repository;

import fr.goat.argentic.model.Photographe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotographeRepository extends JpaRepository<Photographe, Long> {
}
