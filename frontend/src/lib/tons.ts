// Tons de couleur de l'interface, dérivés uniquement des variables du thème shadcn (App.css).
// Changer de thème change donc toutes les couleurs ; ne jamais utiliser de couleur Tailwind en dur
// (bg-blue-*, text-emerald-*…). Les classes sont écrites en entier pour que Tailwind les détecte.

export type Ton = "succes" | "attention" | "info" | "accent" | "danger" | "neutre";

interface TonClasses {
  // Pastille / badge discret : fond léger, bordure, texte lisible
  badge: string;
  // Point de couleur (légendes, indicateurs)
  dot: string;
  // Texte ou icône colorés
  text: string;
  // Fond plein (barres de progression, pastilles pleines)
  solid: string;
  // Carte de planning : liseré gauche + fond léger
  card: string;
  // Zone / panneau mis en avant (encart, carte KPI)
  surface: string;
  // Bouton d'action secondaire teinté
  button: string;
}

export const TONS: Record<Ton, TonClasses> = {
  succes: {
    badge: "bg-primary/10 text-primary border-primary/25",
    dot: "bg-primary",
    text: "text-primary",
    solid: "bg-primary text-primary-foreground",
    card: "border-l-4 border-l-primary bg-primary/5 hover:bg-primary/10 border-border",
    surface: "bg-primary/5 border-primary/20",
    button: "border-primary/30 bg-primary/10 text-primary hover:bg-primary/15",
  },
  attention: {
    badge: "bg-chart-2/15 text-foreground border-chart-2/40",
    dot: "bg-chart-2",
    text: "text-chart-3",
    solid: "bg-chart-2 text-foreground",
    card: "border-l-4 border-l-chart-2 bg-chart-2/5 hover:bg-chart-2/10 border-border",
    surface: "bg-chart-2/5 border-chart-2/30",
    button: "border-chart-2/40 bg-chart-2/10 text-foreground hover:bg-chart-2/20",
  },
  info: {
    badge: "bg-chart-1/15 text-foreground border-chart-1/40",
    dot: "bg-chart-1",
    text: "text-chart-2",
    solid: "bg-chart-1 text-foreground",
    card: "border-l-4 border-l-chart-1 bg-chart-1/5 hover:bg-chart-1/10 border-border",
    surface: "bg-chart-1/5 border-chart-1/30",
    button: "border-chart-1/40 bg-chart-1/10 text-foreground hover:bg-chart-1/20",
  },
  accent: {
    badge: "bg-chart-4/10 text-foreground border-chart-4/30",
    dot: "bg-chart-4",
    text: "text-chart-4",
    solid: "bg-chart-4 text-primary-foreground",
    card: "border-l-4 border-l-chart-4 bg-chart-4/5 hover:bg-chart-4/10 border-border",
    surface: "bg-chart-4/5 border-chart-4/25",
    button: "border-chart-4/30 bg-chart-4/10 text-foreground hover:bg-chart-4/20",
  },
  danger: {
    badge: "bg-destructive/10 text-destructive border-destructive/30",
    dot: "bg-destructive",
    text: "text-destructive",
    solid: "bg-destructive text-primary-foreground",
    card: "border-l-4 border-l-destructive bg-destructive/5 hover:bg-destructive/10 border-border",
    surface: "bg-destructive/5 border-destructive/25",
    button: "border-destructive/30 bg-destructive/10 text-destructive hover:bg-destructive/15",
  },
  neutre: {
    badge: "bg-muted text-muted-foreground border-border",
    dot: "bg-muted-foreground",
    text: "text-muted-foreground",
    solid: "bg-muted-foreground text-background",
    card: "border-l-4 border-l-muted-foreground bg-muted/40 hover:bg-muted/70 border-border",
    surface: "bg-muted/40 border-border",
    button: "border-border bg-muted text-foreground hover:bg-muted/70",
  },
};

// Filtre / onglet sélectionné : identique pour tous les tons (lisibilité garantie par le thème)
export const PILL_ACTIVE = "bg-primary text-primary-foreground border-primary shadow-xs hover:bg-primary/90";
