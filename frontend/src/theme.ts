export const themeFields = [
  { key: "themePrimary", label: "Couleur principale", defaultValue: "#a879ff" },
  {
    key: "themeSecondary",
    label: "Couleur secondaire",
    defaultValue: "#50daef",
  },
  { key: "themeBackground", label: "Fond du site", defaultValue: "#080b16" },
  {
    key: "themeSurface",
    label: "Fond des cartes et formulaires",
    defaultValue: "#11182a",
  },
  { key: "themeText", label: "Texte principal", defaultValue: "#eef0ff" },
  { key: "themeMuted", label: "Texte secondaire", defaultValue: "#a0a8c3" },
];
export function validColor(value: string): boolean {
  return /^#[0-9a-fA-F]{6}$/.test(value);
}
export function contrast(first: string, second: string): number {
  const luminance = (hex: string) => {
    const rgb = [1, 3, 5]
      .map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
      .map((c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
    return rgb[0] * 0.2126 + rgb[1] * 0.7152 + rgb[2] * 0.0722;
  };
  const a = luminance(first),
    b = luminance(second);
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
}
export function accentText(color: string): string {
  return contrast(color, "#ffffff") >= contrast(color, "#000000")
    ? "#ffffff"
    : "#000000";
}
