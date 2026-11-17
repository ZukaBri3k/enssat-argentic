package fr.goat.argentic.controller;

import fr.goat.argentic.dto.FactureRequest;
import fr.goat.argentic.dto.FactureResponse;
import fr.goat.argentic.dto.PaiementFactureRequest;
import fr.goat.argentic.exception.GlobalExceptionHandler;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.CoordonneesFacturation;
import fr.goat.argentic.model.PrestationFacturee;
import fr.goat.argentic.model.StatutPaiement;
import fr.goat.argentic.service.FactureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FactureControllerTest {

    @Mock
    private FactureService factureService;

    private MockMvc mockMvc;

    private final FactureResponse factureResponse = new FactureResponse(
            42L,
            "FAC-2026-0001",
            1L,
            LocalDate.of(2026, 11, 17),
            new BigDecimal("250.00"),
            new BigDecimal("20.00"),
            new BigDecimal("300.00"),
            StatutPaiement.EN_ATTENTE,
            null,
            new CoordonneesFacturation(
                    "Dupont Marie",
                    "12 rue de Kerampont, 22300 Lannion",
                    "marie.dupont@mail.fr",
                    null
            ),
            new PrestationFacturee(
                    LocalDate.of(2026, 11, 10),
                    LocalTime.of(14, 0),
                    120,
                    "Mariage et fêtes",
                    "Lucas Bernard"
            )
    );

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FactureController(factureService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /seances/{id}/facture renvoie 201 Created et la facture générée")
    void genererFacture_valide_doitRetourner201() throws Exception {
        when(factureService.genererFacture(eq(1L), any(FactureRequest.class))).thenReturn(factureResponse);

        mockMvc.perform(post("/seances/1/facture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montantHt": 250.00, "tauxTva": 20.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.numero").value("FAC-2026-0001"))
                .andExpect(jsonPath("$.seanceId").value(1))
                .andExpect(jsonPath("$.montantHt").value(250.00))
                .andExpect(jsonPath("$.tauxTva").value(20.00))
                .andExpect(jsonPath("$.montantTtc").value(300.00))
                .andExpect(jsonPath("$.statutPaiement").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.coordonneesClient.identite").value("Dupont Marie"))
                .andExpect(jsonPath("$.prestation.typeSeance").value("Mariage et fêtes"));
    }

    @Test
    @DisplayName("POST /seances/{id}/facture renvoie 400 en ProblemDetail si montantHt est manquant ou négatif")
    void genererFacture_montantHtManquant_doitRetourner400() throws Exception {
        mockMvc.perform(post("/seances/1/facture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.montantHt").exists());

        verifyNoInteractions(factureService);
    }

    @Test
    @DisplayName("POST /seances/{id}/facture renvoie 409 en ProblemDetail si la séance n'est pas TERMINEE ou a déjà une facture")
    void genererFacture_regleMetierViolee_doitRetourner409() throws Exception {
        when(factureService.genererFacture(eq(1L), any(FactureRequest.class)))
                .thenThrow(new RegleMetierException("Une facture a déjà été émise pour la séance #1."));

        mockMvc.perform(post("/seances/1/facture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montantHt": 250.00}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Une facture a déjà été émise pour la séance #1."));
    }

    @Test
    @DisplayName("POST /seances/{id}/facture renvoie 404 en ProblemDetail si la séance est introuvable")
    void genererFacture_seanceInconnue_doitRetourner404() throws Exception {
        when(factureService.genererFacture(eq(999L), any(FactureRequest.class)))
                .thenThrow(new ResourceNotFoundException("Séance non trouvée avec l'id : 999"));

        mockMvc.perform(post("/seances/999/facture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montantHt": 250.00}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Séance non trouvée avec l'id : 999"));
    }

    @Test
    @DisplayName("GET /factures renvoie 200 et la liste des factures filtrée")
    void getFactures_avecFiltres_doitRetourner200() throws Exception {
        when(factureService.getFactures(
                StatutPaiement.EN_ATTENTE,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        )).thenReturn(List.of(factureResponse));

        mockMvc.perform(get("/factures")
                        .param("statut", "EN_ATTENTE")
                        .param("du", "2026-11-01")
                        .param("au", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].numero").value("FAC-2026-0001"));
    }

    @Test
    @DisplayName("GET /factures/{id} renvoie 200 et le détail de la facture")
    void getFactureById_existante_doitRetourner200() throws Exception {
        when(factureService.getFactureById(42L)).thenReturn(factureResponse);

        mockMvc.perform(get("/factures/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.numero").value("FAC-2026-0001"))
                .andExpect(jsonPath("$.montantTtc").value(300.00));
    }

    @Test
    @DisplayName("GET /factures/{id} renvoie 404 en ProblemDetail si la facture est introuvable")
    void getFactureById_inexistante_doitRetourner404() throws Exception {
        when(factureService.getFactureById(999L))
                .thenThrow(new ResourceNotFoundException("Facture non trouvée avec l'id : 999"));

        mockMvc.perform(get("/factures/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Facture non trouvée avec l'id : 999"));
    }

    @Test
    @DisplayName("POST /factures/{id}/paiement renvoie 200 et la facture PAYEE avec sa date de paiement")
    void enregistrerPaiement_valide_doitRetourner200() throws Exception {
        PaiementFactureRequest attendu = new PaiementFactureRequest(LocalDate.of(2026, 11, 17));
        when(factureService.enregistrerPaiement(42L, attendu))
                .thenReturn(factureAvecStatut(StatutPaiement.PAYEE, LocalDate.of(2026, 11, 17)));

        mockMvc.perform(post("/factures/42/paiement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"datePaiement": "2026-11-17"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.statutPaiement").value("PAYEE"))
                .andExpect(jsonPath("$.datePaiement").value("2026-11-17"));
    }

    @Test
    @DisplayName("POST /factures/{id}/paiement renvoie 400 en ProblemDetail si la date de paiement est manquante")
    void enregistrerPaiement_sansDate_doitRetourner400() throws Exception {
        mockMvc.perform(post("/factures/42/paiement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.datePaiement").exists());

        verifyNoInteractions(factureService);
    }

    @Test
    @DisplayName("POST /factures/{id}/paiement renvoie 409 en ProblemDetail si la facture n'est pas EN_ATTENTE")
    void enregistrerPaiement_factureNonEnAttente_doitRetourner409() throws Exception {
        when(factureService.enregistrerPaiement(eq(42L), any(PaiementFactureRequest.class)))
                .thenThrow(new RegleMetierException("La facture FAC-2026-0001 est au statut ANNULEE : seule une facture EN_ATTENTE peut être payée ou annulée."));

        mockMvc.perform(post("/factures/42/paiement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"datePaiement": "2026-11-17"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("La facture FAC-2026-0001 est au statut ANNULEE : seule une facture EN_ATTENTE peut être payée ou annulée."));
    }

    @Test
    @DisplayName("POST /factures/{id}/annulation renvoie 200 et la facture ANNULEE")
    void annulerFacture_valide_doitRetourner200() throws Exception {
        when(factureService.annulerFacture(42L)).thenReturn(factureAvecStatut(StatutPaiement.ANNULEE, null));

        mockMvc.perform(post("/factures/42/annulation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.statutPaiement").value("ANNULEE"));
    }

    @Test
    @DisplayName("POST /factures/{id}/annulation renvoie 409 en ProblemDetail si la facture n'est pas EN_ATTENTE")
    void annulerFacture_factureNonEnAttente_doitRetourner409() throws Exception {
        when(factureService.annulerFacture(42L))
                .thenThrow(new RegleMetierException("La facture FAC-2026-0001 est au statut PAYEE : seule une facture EN_ATTENTE peut être payée ou annulée."));

        mockMvc.perform(post("/factures/42/annulation"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("La facture FAC-2026-0001 est au statut PAYEE : seule une facture EN_ATTENTE peut être payée ou annulée."));
    }

    @Test
    @DisplayName("POST /factures/{id}/annulation renvoie 404 en ProblemDetail si la facture est introuvable")
    void annulerFacture_inexistante_doitRetourner404() throws Exception {
        when(factureService.annulerFacture(999L))
                .thenThrow(new ResourceNotFoundException("Facture non trouvée avec l'id : 999"));

        mockMvc.perform(post("/factures/999/annulation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private FactureResponse factureAvecStatut(StatutPaiement statut, LocalDate datePaiement) {
        return new FactureResponse(
                factureResponse.id(),
                factureResponse.numero(),
                factureResponse.seanceId(),
                factureResponse.dateEmission(),
                factureResponse.montantHt(),
                factureResponse.tauxTva(),
                factureResponse.montantTtc(),
                statut,
                datePaiement,
                factureResponse.coordonneesClient(),
                factureResponse.prestation()
        );
    }
}
