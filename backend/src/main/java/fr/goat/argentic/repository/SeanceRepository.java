package fr.goat.argentic.repository;

import fr.goat.argentic.dto.TopPhotographeDureeResponse;
import fr.goat.argentic.dto.TopPhotographeSeancesResponse;
import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.Specialite;
import fr.goat.argentic.model.StatutSeance;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface SeanceRepository extends JpaRepository<Seance, Long> {

    boolean existsByPhotographeId(Long photographeId);

    // Parmi les spécialités données, celles pour lesquelles le photographe a une séance non annulée débutant après (date, heure)
    @Query("""
            SELECT DISTINCT s.specialite
            FROM Seance s
            WHERE s.photographe.id = :photographeId
              AND s.specialite.id IN :specialiteIds
              AND s.statut <> fr.goat.argentic.model.StatutSeance.ANNULEE
              AND (s.date > :date OR (s.date = :date AND s.heureDebut > :heure))
            """)
    List<Specialite> findSpecialitesAvecSeancesAVenir(
            @Param("photographeId") Long photographeId,
            @Param("specialiteIds") Collection<Long> specialiteIds,
            @Param("date") LocalDate date,
            @Param("heure") LocalTime heure
    );

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
