package fr.goat.argentic.service;

import fr.goat.argentic.dto.PhotographeResponse;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.Photographe;
import fr.goat.argentic.repository.PhotographeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PhotographeService {

    private final PhotographeRepository photographeRepository;

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
        return photographeRepository.findById(id)
                .map(PhotographeResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Photographe introuvable : " + id));
    }
}
