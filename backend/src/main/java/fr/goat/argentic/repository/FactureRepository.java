package fr.goat.argentic.repository;

import fr.goat.argentic.model.Facture;
import fr.goat.argentic.model.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
