package fr.goat.argentic.service;

import fr.goat.argentic.dto.ClientRequest;
import fr.goat.argentic.dto.ClientResponse;
import fr.goat.argentic.dto.SeanceRequest;
import fr.goat.argentic.dto.SeanceResponse;
import fr.goat.argentic.exception.DonneeInvalideException;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.*;
import fr.goat.argentic.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeanceService {

    private final SeanceRepository seanceRepository;
    private final FactureRepository factureRepository;
    private final PhotographeRepository photographeRepository;
    private final ClientRepository clientRepository;
    private final SpecialiteRepository specialiteRepository;

    // lister les séances avec filtres (période du/au, photographe, client, statut)
    public List<SeanceResponse> getSeance(LocalDate du, LocalDate au, Long idPhotographe, Long idClient, StatutSeance statutSeance) {
        if (du != null && au != null && du.isAfter(au)) {
            throw new RegleMetierException("La date de début (du) ne peut pas être postérieure à la date de fin (au)");
        }

        return seanceRepository.getSeances(du, au, idPhotographe, idClient, statutSeance).stream()
                .map(SeanceResponse::fromEntity)
                .toList();
    }

    public List<SeanceResponse> getSeancesPourPhotographe(String emailPhotographe, LocalDate du, LocalDate au, Long idClient, StatutSeance statutSeance) {
        Photographe photographe = photographeRepository.findByEmailIgnoreCase(emailPhotographe)
                .orElseThrow(() -> new ResourceNotFoundException("Photographe introuvable pour le compte : " + emailPhotographe));
        return getSeance(du, au, photographe.getId(), idClient, statutSeance);
    }

    public SeanceResponse getSeanceById(Long idSeance) {
        Seance seance = trouverSeance(idSeance);
        Facture facture = factureRepository.findBySeanceId(idSeance).orElse(null);
        return SeanceResponse.fromEntity(seance, facture);
    }

    public SeanceResponse getSeanceByIdPourPhotographe(Long idSeance, String emailPhotographe) {
        Seance seance = trouverSeance(idSeance);
        if (!seance.getPhotographe().getEmail().equalsIgnoreCase(emailPhotographe)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres séances");
        }
        Facture facture = factureRepository.findBySeanceId(idSeance).orElse(null);
        return SeanceResponse.fromEntity(seance, facture);
    }

    @Transactional
    public SeanceResponse changerStatut(Long idSeance, StatutSeance nouveauStatut) {
        if (nouveauStatut == null) {
            throw new DonneeInvalideException("statut", "Le statut est obligatoire");
        }

        Seance seance = trouverSeance(idSeance);
        StatutSeance statutActuel = seance.getStatut();

        if (!statutActuel.peutPasserA(nouveauStatut)) {
            if (statutActuel.estFinal()) {
                throw new RegleMetierException("Impossible de modifier le statut d'une séance " + statutActuel
                        + " : ce statut est final.");
            }
            throw new RegleMetierException("Transition de statut invalide : impossible de passer de "
                    + statutActuel + " à " + nouveauStatut + ".");
        }

        if (nouveauStatut == StatutSeance.TERMINEE && seance.getFin().isAfter(LocalDateTime.now())) {
            throw new RegleMetierException("Impossible de passer la séance à TERMINEE : la fin de la séance n'est pas encore passée.");
        }

        seance.setStatut(nouveauStatut);
        seanceRepository.save(seance);
        Facture facture = factureRepository.findBySeanceId(idSeance).orElse(null);
        return SeanceResponse.fromEntity(seance, facture);
    }

    @Transactional
    public void deleteSeance(Long idSeance) {
        Seance seance = trouverSeance(idSeance);

        if (factureRepository.existsBySeanceId(idSeance)) {
            throw new RegleMetierException("Impossible de supprimer cette séance : une facture y est associée. Vous pouvez l'annuler à la place.");
        }

        seanceRepository.delete(seance);
    }

    private Seance trouverSeance(Long idSeance) {
        return seanceRepository.getSeanceById(idSeance)
                .orElseThrow(() -> new ResourceNotFoundException("Séance non trouvée avec l'id : " + idSeance));
    }

    @Transactional
    public SeanceResponse addSeance(@Valid SeanceRequest request) {
        if (request == null) {
            throw new DonneeInvalideException("request", "La requête est obligatoire");
        }

        LocalDateTime debut = request.date().atTime(request.heureDebut());
        if (debut.isBefore(LocalDateTime.now())) {
            throw new RegleMetierException("La date et l'heure de début doivent être postérieures ou égales à maintenant.");
        }

        LocalDateTime fin = debut.plusMinutes(request.dureeMinutes());
        if (fin.toLocalDate().isAfter(request.date()) && !fin.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            throw new RegleMetierException("Une séance ne peut pas dépasser minuit (fin au plus tard à 24:00 le même jour).");
        }

        Photographe p = photographeRepository.findById(request.photographeId())
                .orElseThrow(() -> new ResourceNotFoundException("Photographe introuvable : " + request.photographeId()));

        Client c = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.clientId()));

        Specialite s = specialiteRepository.findById(request.specialiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Spécialité introuvable : " + request.specialiteId()));

        if (!p.isActif()) {
            throw new RegleMetierException("Impossible d'assigner un photographe inactif à une séance.");
        }

        if (!p.possedeSpecialite(s)) {
            throw new RegleMetierException(String.format(
                    "Le photographe %s ne possède pas la spécialité '%s'.",
                    p.getNom(), s.getLibelle()
            ));
        }

        verifierChevauchement(null, p.getId(), request.date(), debut, fin);

        Seance seance = new Seance(p, c, s, request.date(), request.heureDebut(), request.dureeMinutes(), request.lieu());
        return SeanceResponse.fromEntity(seanceRepository.save(seance));
    }

    private void verifierChevauchement(Long seanceIdAExclure, Long photographeId, LocalDate date, LocalDateTime debut, LocalDateTime fin) {
        List<Seance> seancesDuJour = seanceRepository
                .findByPhotographeIdAndDateAndStatutNot(photographeId, date, StatutSeance.ANNULEE);

        for (Seance autre : seancesDuJour) {
            if (seanceIdAExclure != null && autre.getId().equals(seanceIdAExclure)) {
                continue;
            }
            if (debut.isBefore(autre.getFin()) && autre.getDebut().isBefore(fin)) {
                throw new RegleMetierException(String.format(
                        "Le créneau demandé (%s - %s) chevauche une autre séance du photographe (séance #%d de %s à %s).",
                        debut.toLocalTime(), fin.toLocalTime(),
                        autre.getId(), autre.getHeureDebut(), autre.getFin().toLocalTime()
                ));
            }
        }
    }

    private void validerHorairesEtConflits(Long seanceIdAExclure, SeanceRequest request) {
        LocalDateTime debut = request.date().atTime(request.heureDebut());
        LocalDateTime fin = debut.plusMinutes(request.dureeMinutes());

        if (debut.isBefore(LocalDateTime.now())) {
            throw new RegleMetierException("La date et l'heure de début doivent être postérieures ou égales à maintenant.");
        }
        if (fin.toLocalDate().isAfter(request.date()) && !fin.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            throw new RegleMetierException("Une séance ne peut pas dépasser minuit (fin au plus tard à 24:00 le même jour).");
        }
        verifierChevauchement(seanceIdAExclure, request.photographeId(), request.date(), debut, fin);
    }

    @Transactional
    public SeanceResponse updateSeance(Long id, @Valid SeanceRequest request) {
        if (request == null) {
            throw new DonneeInvalideException("request", "La requête est obligatoire");
        }

        Seance seance = trouverSeance(id);

        if (!seance.estModifiable()) {
            throw new RegleMetierException("Impossible de modifier une séance " + seance.getStatut()
                    + " (seules les séances PLANIFIEE ou CONFIRMEE sont modifiables).");
        }

        validerHorairesEtConflits(id, request);

        Photographe p = photographeRepository.findById(request.photographeId())
                .orElseThrow(() -> new ResourceNotFoundException("Photographe introuvable : " + request.photographeId()));

        Client c = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.clientId()));

        Specialite s = specialiteRepository.findById(request.specialiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Spécialité introuvable : " + request.specialiteId()));

        if (!p.isActif()) {
            throw new RegleMetierException("Impossible d'assigner un photographe inactif à une séance.");
        }

        if (!p.possedeSpecialite(s)) {
            throw new RegleMetierException(String.format(
                    "Le photographe %s ne possède pas la spécialité '%s'.",
                    p.getNom(), s.getLibelle()
            ));
        }

        seance.setDate(request.date());
        seance.setHeureDebut(request.heureDebut());
        seance.setDureeMinutes(request.dureeMinutes());
        seance.setLieu(request.lieu());
        seance.setPhotographe(p);
        seance.setClient(c);
        seance.setSpecialite(s);

        Seance updated = seanceRepository.save(seance);
        Facture facture = factureRepository.findBySeanceId(id).orElse(null);
        return SeanceResponse.fromEntity(updated, facture);
    }
}
