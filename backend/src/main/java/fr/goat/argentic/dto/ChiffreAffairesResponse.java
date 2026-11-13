package fr.goat.argentic.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ChiffreAffairesResponse(
        LocalDate du,
        LocalDate au,
        BigDecimal montantHt,
        BigDecimal montantTtc
) {}
