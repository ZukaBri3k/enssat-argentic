package fr.goat.argentic.service;

import fr.goat.argentic.dto.PhotographeRequest;
import fr.goat.argentic.dto.PhotographeResponse;
import fr.goat.argentic.dto.SeanceAVenirResponse;
import fr.goat.argentic.exception.DonneeInvalideException;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.Photographe;
import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.Specialite;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.repository.PhotographeRepository;
import fr.goat.argentic.repository.SeanceRepository;
import fr.goat.argentic.repository.SpecialiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotographeService {

    private static final Set<StatutSeance> STATUTS_ANNULABLES = Set.of(StatutSeance.PLANIFIEE, StatutSeance.CONFIRMEE);

    private final PhotographeRepository photographeRepository;
    private final SpecialiteRepository specialiteRepository;
    private final SeanceRepository seanceRepository;

    @Transactional(readOnly = true)
    public List<PhotographeResponse> findAll(Boolean actif) {
        List<Photographe> photographes = actif == null
                ? photographeRepository.findAllByOrderByNomAsc()
                : photographeRepository.findByActifOrderByNomAsc(actif);
        return photographes.stream()
                .map(PhotographeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PhotographeResponse findById(Long id) {
        return PhotographeResponse.fromEntity(trouverPhotographe(id));
    }

    @Transactional
    public PhotographeResponse create(PhotographeRequest request) {
        if (photographeRepository.existsByEmailIgnoreCase(request.email())) {
            throw emailDejaUtilise(request.email());
        }
        Set<Specialite> specialites = chargerSpecialites(request.specialiteIds());
        Photographe photographe = new Photographe(request.nom(), request.email(), specialites);
        return PhotographeResponse.fromEntity(photographeRepository.save(photographe));
    }

    @Transactional
    public PhotographeResponse update(Long id, PhotographeRequest request) {
        Photographe photographe = trouverPhotographe(id);
        if (photographeRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw emailDejaUtilise(request.email());
        }
        Set<Specialite> specialites = chargerSpecialites(request.specialiteIds());
        verifierRetraitSpecialites(photographe, request.specialiteIds());
        photographe.setNom(request.nom());
        photographe.setEmail(request.email());
        photographe.setSpecialites(specialites);
        return PhotographeResponse.fromEntity(photographe);
    }

    @Transactional(readOnly = true)
    public List<SeanceAVenirResponse> findSeancesAVenir(Long id) {
        Photographe photographe = trouverPhotographe(id);
        return seancesAAnnulerSiDesactive(photographe).stream()
                .map(SeanceAVenirResponse::fromEntity)
                .toList();
    }

    // Désactivation et annulation des séances à venir dans la même transaction
    @Transactional
    public PhotographeResponse desactiver(Long id) {
        Photographe photographe = trouverPhotographe(id);
        photographe.desactiver();
        seancesAAnnulerSiDesactive(photographe)
                .forEach(seance -> seance.setStatut(StatutSeance.ANNULEE));
        return PhotographeResponse.fromEntity(photographe);
    }

    @Transactional
    public PhotographeResponse activer(Long id) {
        Photographe photographe = trouverPhotographe(id);
        photographe.activer();
        return PhotographeResponse.fromEntity(photographe);
    }

    // Suppression physique uniquement si le photographe n'a aucune séance, quel que soit son statut
    @Transactional
    public void delete(Long id) {
        Photographe photographe = trouverPhotographe(id);
        if (seanceRepository.existsByPhotographeId(id)) {
            throw new RegleMetierException("Impossible de supprimer " + photographe.getNom()
                    + " : il a des séances enregistrées. Vous pouvez le désactiver à la place.");
        }
        photographeRepository.delete(photographe);
    }

    // Retirer une spécialité est interdit si le photographe a des séances à venir non annulées de ce type
    private void verifierRetraitSpecialites(Photographe photographe, Set<Long> nouvellesSpecialiteIds) {
        Set<Long> retirees = photographe.getSpecialites().stream()
                .map(Specialite::getId)
                .filter(id -> !nouvellesSpecialiteIds.contains(id))
                .collect(Collectors.toSet());
        if (retirees.isEmpty()) {
            return;
        }
        LocalDateTime maintenant = LocalDateTime.now();
        List<String> bloquees = seanceRepository.findSpecialitesAvecSeancesAVenir(
                        photographe.getId(), retirees, maintenant.toLocalDate(), maintenant.toLocalTime())
                .stream()
                .map(Specialite::getLibelle)
                .sorted()
                .toList();
        if (!bloquees.isEmpty()) {
            throw new RegleMetierException("Impossible de retirer la/les spécialité(s) " + String.join(", ", bloquees)
                    + " : le photographe a des séances à venir non annulées de ce type");
        }
    }

    // Séances qu'une désactivation annulerait : PLANIFIEE ou CONFIRMEE, débutant après maintenant
    private List<Seance> seancesAAnnulerSiDesactive(Photographe photographe) {
        LocalDateTime maintenant = LocalDateTime.now();
        return seanceRepository.findSeancesAVenir(photographe.getId(), STATUTS_ANNULABLES,
                maintenant.toLocalDate(), maintenant.toLocalTime());
    }

    private Photographe trouverPhotographe(Long id) {
        return photographeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Photographe introuvable : " + id));
    }

    private Set<Specialite> chargerSpecialites(Set<Long> specialiteIds) {
        Set<Specialite> specialites = new HashSet<>(specialiteRepository.findAllById(specialiteIds));
        List<Long> inconnues = specialiteIds.stream()
                .filter(id -> specialites.stream().noneMatch(s -> s.getId().equals(id)))
                .sorted()
                .toList();
        if (!inconnues.isEmpty()) {
            throw new DonneeInvalideException("specialiteIds", "Spécialité(s) introuvable(s) : " + inconnues);
        }
        return specialites;
    }

    private static RegleMetierException emailDejaUtilise(String email) {
        return new RegleMetierException("Un photographe utilise déjà l'adresse email " + email);
    }
}
