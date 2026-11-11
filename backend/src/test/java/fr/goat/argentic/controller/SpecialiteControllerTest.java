package fr.goat.argentic.controller;

import fr.goat.argentic.dto.SpecialiteResponse;
import fr.goat.argentic.exception.GlobalExceptionHandler;
import fr.goat.argentic.service.SpecialiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpecialiteControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SpecialiteService stubService = new SpecialiteService(null) {
            @Override
            public List<SpecialiteResponse> findAll() {
                return List.of(
                        new SpecialiteResponse(1L, "Reportage"),
                        new SpecialiteResponse(2L, "Mariage et fêtes"),
                        new SpecialiteResponse(3L, "Famille")
                );
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(new SpecialiteController(stubService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /specialites renvoie 200 et la liste des spécialités")
    void getSpecialites_doitRetourner200EtListe() throws Exception {
        mockMvc.perform(get("/specialites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].libelle").value("Reportage"))
                .andExpect(jsonPath("$[1].libelle").value("Mariage et fêtes"))
                .andExpect(jsonPath("$[2].libelle").value("Famille"));
    }
}
