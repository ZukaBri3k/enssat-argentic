package fr.goat.argentic.dto;

public record FacturePdf(
        String numero,
        String filename,
        byte[] content
) {
}
