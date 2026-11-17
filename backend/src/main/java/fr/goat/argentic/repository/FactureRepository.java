package fr.goat.argentic.repository;

import fr.goat.argentic.model.Facture;
import fr.goat.argentic.model.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {

    @Query("""
            SELECT COALESCE(SUM(f.montantHt), 0)
            FROM Facture f
            WHERE f.statutPaiement = :statut
              AND f.datePaiement BETWEEN :du AND :au
            """)
    BigDecimal sumMontantHtByStatutAndPeriode(
            @Param("statut") StatutPaiement statut,
            @Param("du") LocalDate du,
            @Param("au") LocalDate au
    );

    @Query("""
            SELECT COALESCE(SUM(f.montantHt * (1 + f.tauxTva / 100)), 0)
            FROM Facture f
            WHERE f.statutPaiement = :statut
              AND f.datePaiement BETWEEN :du AND :au
            """)
    BigDecimal sumMontantTtcByStatutAndPeriode(
            @Param("statut") StatutPaiement statut,
            @Param("du") LocalDate du,
            @Param("au") LocalDate au
    );

    Optional<Facture> findBySeanceId(Long seanceId);

    boolean existsBySeanceId(Long seanceId);

    Optional<Facture> findTopByNumeroStartingWithOrderByNumeroDesc(String prefixe);

    @Query("""
            SELECT f
            FROM Facture f
            JOIN FETCH f.seance
            WHERE (:statut IS NULL OR f.statutPaiement = :statut)
              AND (:du IS NULL OR f.dateEmission >= :du)
              AND (:au IS NULL OR f.dateEmission <= :au)
            ORDER BY f.dateEmission DESC, f.numero DESC
            """)
    List<Facture> getFactures(
            @Param("statut") StatutPaiement statut,
            @Param("du") LocalDate du,
            @Param("au") LocalDate au
    );
}
