package fr.goat.argentic.exception;

import lombok.Getter;

// Donnée de la requête invalide détectée par le service (ex. identifiant référencé inexistant) → 400
@Getter
public class DonneeInvalideException extends RuntimeException {
    private final String champ;

    public DonneeInvalideException(String champ, String message) {
        super(message);
        this.champ = champ;
    }
}
