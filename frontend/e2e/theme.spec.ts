import { test, expect } from "@playwright/test";
test.use({ actionTimeout: 5000 });

test("palette preview stays private, saves and survives reload", async ({
  page,
  request,
}) => {
  const original = await (await request.get("/api/content")).json();
  await page.goto("/admin");
  await page.getByLabel("Identifiant").fill(process.env["ADMIN_USERNAME"]!);
  await page.getByLabel("Mot de passe").fill(process.env["ADMIN_PASSWORD"]!);
  await page.getByRole("button", { name: "Se connecter" }).click();
  await page.getByRole("tab", { name: "Contenu du site" }).click();
  const field = (label: string) =>
    page.getByRole("textbox", {
      name: label + " — code hexadécimal",
      exact: true,
    });
  const palette = [
    ["Couleur principale", "#ff8800"],
    ["Couleur secondaire", "#55ccaa"],
    ["Fond du site", "#f1f5f9"],
    ["Fond des cartes et formulaires", "#ffffff"],
    ["Texte principal", "#112233"],
    ["Texte secondaire", "#445566"],
  ];
  try {
    await page.getByLabel("Rechercher un champ").fill("Apparence");
    await field("Couleur principale").fill("red");
    await expect(
      page.getByRole("button", { name: "Enregistrer les modifications" }),
    ).toBeDisabled();
    for (const [label, value] of palette) await field(label).fill(value);
    await expect(
      page.getByRole("region", { name: "Aperçu des couleurs" }),
    ).toHaveCSS("background-color", "rgb(241, 245, 249)");
    await expect(page.locator(".theme-preview-button")).toHaveCSS(
      "background-color",
      "rgb(255, 136, 0)",
    );
    const visitor = await page.context().newPage();
    await visitor.goto("/");
    await expect
      .poll(() =>
        visitor.evaluate(() =>
          document.documentElement.style.getPropertyValue("--theme-primary"),
        ),
      )
      .toBe(original.settings.themePrimary);
    await page
      .getByRole("button", { name: "Enregistrer les modifications" })
      .click();
    await expect(page.getByRole("status")).toContainText("a été enregistré");
    await visitor.reload();
    await expect(visitor.locator(".hero-actions .button")).toHaveCSS(
      "background-color",
      "rgb(255, 136, 0)",
    );
    await expect(visitor.locator(".hero-actions .button")).toHaveCSS(
      "color",
      "rgb(0, 0, 0)",
    );
    await expect(visitor.locator("body")).toHaveCSS(
      "background-color",
      "rgb(241, 245, 249)",
    );
    await expect(visitor.locator(".service").first()).toHaveCSS(
      "background-color",
      "rgb(255, 255, 255)",
    );
    await visitor.close();
    await page.reload();
    await page.getByRole("tab", { name: "Contenu du site" }).click();
    await page.getByLabel("Rechercher un champ").fill("Apparence");
    await expect(field("Couleur principale")).toHaveValue("#ff8800");
    await page
      .getByRole("button", { name: "Rétablir les couleurs par défaut" })
      .click();
    await expect(field("Couleur principale")).toHaveValue("#a879ff");
    // Resetting the draft must not publish without an explicit save.
    expect(
      (await (await page.request.get("/api/content")).json()).settings
        .themePrimary,
    ).toBe("#ff8800");
  } finally {
    const token = (await (await page.request.get("/api/csrf")).json()).token;
    const response = await page.request.put("/api/admin/content", {
      headers: { "X-XSRF-TOKEN": token },
      data: {
        headline: original.headline,
        intro: original.intro,
        available: original.available,
        settings: original.settings,
      },
    });
    expect(response.ok()).toBeTruthy();
  }
});
