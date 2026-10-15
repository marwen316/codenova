export interface MediaAsset {
  id: string;
  filename: string;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
  url: string;
}
export interface MediaSlot {
  key: string;
  label: string;
  description: string;
  defaultUrl: string;
  preview: string;
}
export const mediaSlots: MediaSlot[] = [
  {
    key: "logoIcon",
    label: "Symbole du site",
    description:
      "En-tête, pied de page et icône du navigateur. Privilégiez une image carrée.",
    defaultUrl: "/logo.svg",
    preview: "/logo.svg",
  },
  {
    key: "logoFull",
    label: "Logo de présentation",
    description: "Logo complet affiché dans la section À propos.",
    defaultUrl: "/logo-full.svg",
    preview: "/logo-full.svg",
  },
  {
    key: "heroImage",
    label: "Visuel d’accueil",
    description:
      "Une image peut remplacer l’illustration animée. La restauration réactive l’illustration d’origine.",
    defaultUrl: "",
    preview: "/logo.svg",
  },
  {
    key: "socialImage",
    label: "Image de partage",
    description:
      "Image utilisée par les métadonnées de partage. Un format horizontal est conseillé.",
    defaultUrl: "/social-card.png",
    preview: "/social-card.png",
  },
];
