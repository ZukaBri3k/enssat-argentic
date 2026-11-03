package fr.goat.argentic.model;

public enum StatutSeance {
    PLANIFIEE,
    CONFIRMEE,
    TERMINEE,
    ANNULEE;

    public boolean peutPasserA(StatutSeance nouveau) {
        return switch (this) {
            case PLANIFIEE -> nouveau == CONFIRMEE || nouveau == ANNULEE;
            case CONFIRMEE -> nouveau == TERMINEE || nouveau == ANNULEE;
            case TERMINEE, ANNULEE -> false;
        };
    }

    public boolean estFinal() {
        return this == TERMINEE || this == ANNULEE;
    }
}
