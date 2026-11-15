package fr.goat.argentic.service;

import fr.goat.argentic.dto.ClientRequest;
import fr.goat.argentic.dto.ClientResponse;
import fr.goat.argentic.exception.DonneeInvalideException;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.Client;
import fr.goat.argentic.model.ClientEntreprise;
import fr.goat.argentic.model.ClientParticulier;
import fr.goat.argentic.repository.ClientRepository;
import fr.goat.argentic.repository.SeanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private SeanceRepository seanceRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    @DisplayName("getClients retourne la liste des clients mappés en DTO")
    void getClients_doitRetournerListeClients() {
        ClientParticulier p = new ClientParticulier("marie@mail.fr", "12 rue de Lannion", "Dupont", "Marie");
        p.setId(1L);
        ClientEntreprise e = new ClientEntreprise("contact@breizh.fr", "15 rue Poterie", "Atelier Breizh", "FR123");
        e.setId(2L);

        when(clientRepository.findAll()).thenReturn(List.of(p, e));

        List<ClientResponse> res = clientService.getClients();

        assertThat(res).hasSize(2);
        assertThat(res.get(0).type()).isEqualTo("PARTICULIER");
        assertThat(res.get(0).identite()).isEqualTo("Dupont Marie");
        assertThat(res.get(1).type()).isEqualTo("ENTREPRISE");
        assertThat(res.get(1).identite()).isEqualTo("Atelier Breizh");
    }

    @Test
    @DisplayName("getClientById retourne le client existant")
    void getClientById_existant_doitRetournerClient() {
        ClientParticulier p = new ClientParticulier("marie@mail.fr", "12 rue de Lannion", "Dupont", "Marie");
        p.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(p));

        ClientResponse res = clientService.getClientById(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.identite()).isEqualTo("Dupont Marie");
    }

    @Test
    @DisplayName("getClientById lève ResourceNotFoundException si client introuvable")
    void getClientById_inexistant_doitLeverResourceNotFoundException() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("addClient crée un particulier valide")
    void addClient_particulierValide_doitSauvegarder() {
        ClientRequest req = new ClientRequest("PARTICULIER", "test@mail.fr", "Adresse 1", "Martin", "Paul", null, null);

        when(clientRepository.save(any(ClientParticulier.class))).thenAnswer(invocation -> {
            ClientParticulier cp = invocation.getArgument(0);
            cp.setId(10L);
            return cp;
        });

        ClientResponse res = clientService.addClient(req);

        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.type()).isEqualTo("PARTICULIER");
        assertThat(res.identite()).isEqualTo("Martin Paul");
    }

    @Test
    @DisplayName("addClient lève DonneeInvalideException si nom manquant pour un particulier")
    void addClient_particulierSansNom_doitLeverDonneeInvalideException() {
        ClientRequest req = new ClientRequest("PARTICULIER", "test@mail.fr", "Adresse 1", "", "Paul", null, null);

        assertThatThrownBy(() -> clientService.addClient(req))
                .isInstanceOf(DonneeInvalideException.class)
                .hasMessageContaining("nom");
    }

    @Test
    @DisplayName("addClient crée une entreprise valide")
    void addClient_entrepriseValide_doitSauvegarder() {
        ClientRequest req = new ClientRequest("ENTREPRISE", "tech@mail.fr", "Adresse 2", null, null, "Tech SAS", "FR999");

        when(clientRepository.save(any(ClientEntreprise.class))).thenAnswer(invocation -> {
            ClientEntreprise ce = invocation.getArgument(0);
            ce.setId(20L);
            return ce;
        });

        ClientResponse res = clientService.addClient(req);

        assertThat(res.id()).isEqualTo(20L);
        assertThat(res.type()).isEqualTo("ENTREPRISE");
        assertThat(res.identite()).isEqualTo("Tech SAS");
    }

    @Test
    @DisplayName("addClient lève DonneeInvalideException si raison sociale manquante pour une entreprise")
    void addClient_entrepriseSansRaisonSociale_doitLeverDonneeInvalideException() {
        ClientRequest req = new ClientRequest("ENTREPRISE", "tech@mail.fr", "Adresse 2", null, null, "", "FR999");

        assertThatThrownBy(() -> clientService.addClient(req))
                .isInstanceOf(DonneeInvalideException.class)
                .hasMessageContaining("raisonSociale");
    }

    @Test
    @DisplayName("updateClient met à jour un particulier sans changer de type")
    void updateClient_particulierMemeType_doitMettreAJour() {
        ClientParticulier p = new ClientParticulier("ancien@mail.fr", "Ancienne adresse", "Dupont", "Marie");
        p.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(p));
        when(clientRepository.save(any(ClientParticulier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientRequest req = new ClientRequest("PARTICULIER", "nouveau@mail.fr", "Nouvelle adresse", "Durand", "Marie", null, null);

        ClientResponse res = clientService.updateClient(1L, req);

        assertThat(res.email()).isEqualTo("nouveau@mail.fr");
        assertThat(res.adressePostale()).isEqualTo("Nouvelle adresse");
        assertThat(res.nom()).isEqualTo("Durand");
    }

    @Test
    @DisplayName("updateClient lève RegleMetierException si tentative de changer le type du client")
    void updateClient_changementType_doitLeverRegleMetierException() {
        ClientParticulier p = new ClientParticulier("marie@mail.fr", "Adresse", "Dupont", "Marie");
        p.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(p));

        ClientRequest req = new ClientRequest("ENTREPRISE", "marie@mail.fr", "Adresse", null, null, "Société", "FR123");

        assertThatThrownBy(() -> clientService.updateClient(1L, req))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("type d'un client ne peut pas être modifié");
    }

    @Test
    @DisplayName("deleteClient supprime le client s'il n'a pas de séances")
    void deleteClient_sansSeances_doitSupprimer() {
        ClientParticulier p = new ClientParticulier("marie@mail.fr", "Adresse", "Dupont", "Marie");
        p.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(p));
        when(seanceRepository.existsByClientId(1L)).thenReturn(false);

        clientService.deleteClient(1L);

        verify(clientRepository).delete(p);
    }

    @Test
    @DisplayName("deleteClient lève RegleMetierException (409) si le client a des séances")
    void deleteClient_avecSeances_doitLeverRegleMetierException() {
        ClientParticulier p = new ClientParticulier("marie@mail.fr", "Adresse", "Dupont", "Marie");
        p.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(p));
        when(seanceRepository.existsByClientId(1L)).thenReturn(true);

        assertThatThrownBy(() -> clientService.deleteClient(1L))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("séances");
    }
}
