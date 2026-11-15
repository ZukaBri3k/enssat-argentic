package fr.goat.argentic.service;

import fr.goat.argentic.dto.*;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.Client;
import fr.goat.argentic.model.ClientEntreprise;
import fr.goat.argentic.model.ClientParticulier;
import fr.goat.argentic.repository.ClientRepository;
import fr.goat.argentic.repository.FactureRepository;
import fr.goat.argentic.repository.SeanceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final FactureRepository factureRepository;
    private final SeanceRepository seanceRepository;

    public List<ClientResponse> getClients() {
        List<Client> clients = clientRepository.findAll();
        return clients.stream()
                .map(ClientResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + clientId));

        if (seanceRepository.existsByClientId(clientId)) {
            throw new RegleMetierException("Impossible de supprimer " + client.getIdentite()
                    + " : il/elle a des séances enregistrées.");
        }

        clientRepository.delete(client);
    }

    public ClientResponse getClientById(Long clientId) {
        return clientRepository.findById(clientId)
                .map(ClientResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + clientId));
    }

    @Transactional
    public ClientResponse addClient(ClientRequest request) {
        // Vérification du type et validation conditionnelle
        if ("PARTICULIER".equalsIgnoreCase(request.type())) {
            if (request.nom() == null || request.nom().isBlank()) {
                throw new IllegalArgumentException("Le nom est obligatoire pour un particulier");
            }
            if (request.prenom() == null || request.prenom().isBlank()) {
                throw new IllegalArgumentException("Le prénom est obligatoire pour un particulier");
            }

            ClientParticulier particulier = new ClientParticulier(
                    request.email().trim(),
                    request.adressePostale().trim(),
                    request.nom().trim(),
                    request.prenom().trim()
            );
            return ClientResponse.fromEntity(clientRepository.save(particulier));

        } else if ("ENTREPRISE".equalsIgnoreCase(request.type())) {
            if (request.raisonSociale() == null || request.raisonSociale().isBlank()) {
                throw new IllegalArgumentException("La raison sociale est obligatoire pour une entreprise");
            }
            if (request.numeroSirenTva() == null || request.numeroSirenTva().isBlank()) {
                throw new IllegalArgumentException("Le numéro SIREN/TVA est obligatoire pour une entreprise");
            }

            ClientEntreprise entreprise = new ClientEntreprise(
                    request.email().trim(),
                    request.adressePostale().trim(),
                    request.raisonSociale().trim(),
                    request.numeroSirenTva().trim()
            );
            return ClientResponse.fromEntity(clientRepository.save(entreprise));

        } else {
            throw new IllegalArgumentException("Type de client inconnu : " + request.type()
                    + ". Les valeurs autorisées sont PARTICULIER ou ENTREPRISE.");
        }
    }

    @Transactional
    public ClientResponse updateClient(Long id, ClientRequest request) {
        // 1. On charge le client existant en base (ou 404 s'il n'existe pas)
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));

        // 2. On vérifie la règle métier avec instanceof :
        if (client instanceof ClientParticulier p) {
            // En base c'est un PARTICULIER : est-ce que la requête essaie de le changer ?
            if (!"PARTICULIER".equalsIgnoreCase(request.type())) {
                throw new RegleMetierException("Le type d'un client ne peut pas être modifié après création");
            }

            // Tout va bien : on met à jour les champs propres au particulier
            p.setNom(request.nom().trim());
            p.setPrenom(request.prenom().trim());

        } else if (client instanceof ClientEntreprise e) {
            // En base c'est une ENTREPRISE : est-ce que la requête essaie de la changer ?
            if (!"ENTREPRISE".equalsIgnoreCase(request.type())) {
                throw new RegleMetierException("Le type d'un client ne peut pas être modifié après création");
            }

            // Tout va bien : on met à jour les champs propres à l'entreprise
            e.setRaisonSociale(request.raisonSociale().trim());
            e.setNumeroSirenTva(request.numeroSirenTva().trim());
        }

        // 3. On met à jour les champs communs aux deux types
        client.setEmail(request.email().trim());
        client.setAdressePostale(request.adressePostale().trim());

        // 4. On sauvegarde et on renvoie le DTO à jour
        Client updated = clientRepository.save(client);
        return ClientResponse.fromEntity(updated);
    }
}
