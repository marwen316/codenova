import { test, expect, Page } from "@playwright/test";
test.use({ actionTimeout: 5000 });
async function editor(page: Page) {
  await page.goto("/admin");
  await page.getByLabel("Identifiant").fill(process.env["ADMIN_USERNAME"]!);
  await page.getByLabel("Mot de passe").fill(process.env["ADMIN_PASSWORD"]!);
  await page.getByRole("button", { name: "Se connecter" }).click();
  await page.getByRole("tab", { name: "Contenu du site" }).click();
}

test("full editor saves a color-picker change and text with visible verified feedback", async ({
  page,
  request,
}) => {
  const original = await (await request.get("/api/content")).json();
  await editor(page);
  try {
    await page
      .getByLabel("Titre de présentation", { exact: true })
      .fill("Présentation enregistrée avec les couleurs");
    await page
      .locator(".editor-group")
      .filter({ has: page.locator("summary", { hasText: "Apparence" }) })
      .locator("summary")
      .click();
    await page.getByLabel("Couleur principale — sélecteur", {exact:true}).fill("#3377aa");
    await page
      .getByRole("button", { name: "Enregistrer les modifications" })
      .click();
    await expect(page.getByRole("status")).toContainText(
      "enregistré et vérifié",
    );
    await expect(page.getByRole("status")).toBeInViewport();
    await expect(page.locator(".save-state")).toHaveText(
      "Aucune modification en attente",
    );
    await page.reload();
    await page.getByRole("tab", { name: "Contenu du site" }).click();
    await expect(
      page.getByLabel("Titre de présentation", { exact: true }),
    ).toHaveValue("Présentation enregistrée avec les couleurs");
    await page
      .locator(".editor-group")
      .filter({ has: page.locator("summary", { hasText: "Apparence" }) })
      .locator("summary")
      .click();
    await expect(
      page.getByLabel("Couleur principale — sélecteur", {exact:true}),
    ).toHaveValue("#3377aa");
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

test("a refused save reports the HTTP error and retains the draft", async ({
  page,
}) => {
  await editor(page);
  await page.route("**/api/admin/content", (route) =>
    route.fulfill({ status: 403, body: "{}", contentType: "application/json" }),
  );
  await page
    .getByLabel("Titre de présentation", { exact: true })
    .fill("Brouillon conservé après erreur");
  await page
    .getByRole("button", { name: "Enregistrer les modifications" })
    .click();
  await expect(page.getByRole("alert")).toContainText("HTTP 403");
  await expect(page.getByRole("alert")).toBeInViewport();
  await expect(
    page.getByLabel("Titre de présentation", { exact: true }),
  ).toHaveValue("Brouillon conservé après erreur");
  await expect(page.getByRole("status")).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Enregistrer les modifications" }),
  ).toBeEnabled();
});

test("a successful HTTP response without persisted values cannot claim a saved change", async ({
  page,
}) => {
  await editor(page);
  await page.route("**/api/admin/content", (route) =>
    route.fulfill({ status: 200, body: "{}", contentType: "application/json" }),
  );
  await page
    .getByLabel("Titre de présentation", { exact: true })
    .fill("Modification non persistée");
  await page
    .getByRole("button", { name: "Enregistrer les modifications" })
    .click();
  await expect(page.getByRole("alert")).toContainText(
    "valeurs relues diffèrent",
  );
  await expect(page.getByRole("status")).toHaveCount(0);
  await expect(
    page.getByLabel("Titre de présentation", { exact: true }),
  ).toHaveValue("Modification non persistée");
});
