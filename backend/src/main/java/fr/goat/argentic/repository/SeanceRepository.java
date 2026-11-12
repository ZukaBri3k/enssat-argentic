package fr.goat.argentic.repository;

import fr.goat.argentic.dto.TopPhotographeDureeResponse;
import fr.goat.argentic.dto.TopPhotographeSeancesResponse;
import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.StatutSeance;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeanceRepository extends JpaRepository<Seance, Long> {

    @Query("""
            SELECT new fr.goat.argentic.dto.TopPhotographeSeancesResponse(
                s.photographe.id,
                s.photographe.nom,
                s.photographe.email,
                COUNT(s)
            )
            FROM Seance s
            WHERE s.statut = :statut
            GROUP BY s.photographe.id, s.photographe.nom, s.photographe.email
            ORDER BY COUNT(s) DESC, s.photographe.nom ASC
            """)
    List<TopPhotographeSeancesResponse> findTopPhotographesBySeances(
            @Param("statut") StatutSeance statut,
            Pageable pageable
    );

    @Query("""
            SELECT new fr.goat.argentic.dto.TopPhotographeDureeResponse(
                s.photographe.id,
                s.photographe.nom,
                s.photographe.email,
                SUM(s.dureeMinutes)
            )
            FROM Seance s
            WHERE s.statut = :statut
            GROUP BY s.photographe.id, s.photographe.nom, s.photographe.email
            ORDER BY SUM(s.dureeMinutes) DESC, s.photographe.nom ASC
            """)
    List<TopPhotographeDureeResponse> findTopPhotographesByDuree(
            @Param("statut") StatutSeance statut,
            Pageable pageable
    );
}
