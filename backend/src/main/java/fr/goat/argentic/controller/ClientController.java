package fr.goat.argentic.controller;

import fr.goat.argentic.dto.*;
import fr.goat.argentic.model.Client;
import fr.goat.argentic.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Route des clients")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @Operation(summary = "Récupérer tout les clients")
    public ResponseEntity<List<ClientResponse>> getClients() {
        return ResponseEntity.ok(clientService.getClients());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un client avec son id ")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Supprimer un client",
            description = "Supprime définitivement un client. Refusé (409) s'il a des séances."
    )
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(
            summary = "Ajouter un nouveau client",
            description = "Ajout d'un nouveau client avec son nom prénom, etc"
    )

    public ResponseEntity<ClientResponse> addClient(@Valid @RequestBody ClientRequest request) {
        ClientResponse cree = clientService.addClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cree);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Modifier un client",
            description = "Modifier un client depuis son id"
    )

    public ResponseEntity<ClientResponse> updateClient(@Valid @RequestBody ClientRequest request, @PathVariable Long id) {
        ClientResponse cree = clientService.updateClient(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cree);
    }


}
