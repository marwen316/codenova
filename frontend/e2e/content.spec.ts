import { test, expect } from "@playwright/test";
test.use({ actionTimeout: 5000 });

test("separate content tab saves public content, persists and cancels drafts", async ({
  page,
  request,
}) => {
  const original = await (await request.get("/api/content")).json();
  const publicPayload = (data: any) => ({
    headline: data.headline,
    intro: data.intro,
    available: data.available,
    settings: data.settings,
  });
  await page.goto("/admin");
  await page.getByLabel("Identifiant").fill(process.env["ADMIN_USERNAME"]!);
  await page.getByLabel("Mot de passe").fill(process.env["ADMIN_PASSWORD"]!);
  await page.getByRole("button", { name: "Se connecter" }).click();
  await expect(
    page.getByRole("heading", { name: "Votre espace CodeNova." }),
  ).toBeVisible();
  const csrf = async () =>
    (await (await page.request.get("/api/csrf")).json()).token;
  try {
    await expect(page.getByRole("tab", { name: /Messages/ })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    await expect(
      page.getByRole("heading", { name: "Le contenu du site" }),
    ).toHaveCount(0);
    await page.getByRole("tab", { name: "Contenu du site" }).click();
    await expect(
      page.getByRole("heading", { name: "Vos messages" }),
    ).toHaveCount(0);
    await page.getByLabel("E-mail de contact").fill("nouveau@example.com");
    await page
      .getByRole("textbox", { name: "Ville", exact: true })
      .fill("Ville de recette");
    await page
      .getByLabel("Rechercher un champ")
      .fill("Titre principal — ligne 1");
    await page
      .getByLabel("Titre principal — ligne 1")
      .fill("Construisons demain.");
    await page
      .getByRole("button", { name: "Enregistrer les modifications" })
      .click();
    await expect(page.getByRole("status")).toContainText("a été enregistré");
    await page.reload();
    await page.getByRole("tab", { name: "Contenu du site" }).click();
    await expect(page.getByLabel("E-mail de contact")).toHaveValue(
      "nouveau@example.com",
    );
    const visitor = await page.context().newPage();
    await visitor.goto("/");
    await expect(visitor.getByRole("heading", { level: 1 })).toContainText(
      "Construisons demain.",
    );
    await expect(visitor.locator("address")).toContainText("Ville de recette");
    await expect(visitor.locator(".contact-links a").first()).toHaveAttribute(
      "href",
      "mailto:nouveau@example.com",
    );
    await visitor.close();
    await page.getByLabel("E-mail de contact").fill("brouillon@example.com");
    page.once("dialog", (dialog) => dialog.accept());
    await page
      .getByRole("button", { name: "Annuler les modifications" })
      .click();
    await expect(page.getByLabel("E-mail de contact")).toHaveValue(
      "nouveau@example.com",
    );
    await page.getByRole("tab", { name: /Messages/ }).click();
    await expect(
      page.getByRole("heading", { name: "Vos messages" }),
    ).toBeVisible();
  } finally {
    const response = await page.request.put("/api/admin/content", {
      headers: { "X-XSRF-TOKEN": await csrf() },
      data: publicPayload(original),
    });
    expect(response.ok()).toBeTruthy();
  }
});
