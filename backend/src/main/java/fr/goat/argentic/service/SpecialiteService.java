package fr.goat.argentic.service;

import fr.goat.argentic.dto.SpecialiteResponse;
import fr.goat.argentic.repository.SpecialiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialiteService {

    private final SpecialiteRepository specialiteRepository;

    @Transactional(readOnly = true)
    public List<SpecialiteResponse> findAll() {
        return specialiteRepository.findAll().stream()
                .map(SpecialiteResponse::fromEntity)
                .toList();
    }
}
