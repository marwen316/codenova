import { test, expect } from "@playwright/test";
import { readFile } from "node:fs/promises";
import {join} from "node:path";
test.use({ actionTimeout: 5000 });

test("images upload, publish, restore defaults and delete only unused files", async ({
  page,
  request,
}) => {
  const original = await (await request.get("/api/content")).json();
  let asset: any;
  await page.goto("/admin");
  await page.getByLabel("Identifiant").fill(process.env["ADMIN_USERNAME"]!);
  await page.getByLabel("Mot de passe").fill(process.env["ADMIN_PASSWORD"]!);
  await page.getByRole("button", { name: "Se connecter" }).click();
  await page.getByRole("tab", { name: "Contenu du site" }).click();
  const token = async () =>
    (await (await page.request.get("/api/csrf")).json()).token;
  try {
    await page
      .locator("summary")
      .filter({ hasText: "Images et logos" })
      .click();
    await page
      .getByLabel("Téléverser pour Symbole du site", { exact: true })
      .setInputFiles({
        name: "unsafe.svg",
        mimeType: "image/svg+xml",
        buffer: Buffer.from(
          '<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>',
        ),
      });
    await expect(page.getByRole("alert")).toContainText("élément non autorisé");
    const uploaded = page.waitForResponse(
      (r) =>
        r.url().endsWith("/api/admin/media") && r.request().method() === "POST",
    );
    await page
      .getByLabel("Téléverser pour Symbole du site", { exact: true })
      .setInputFiles({
        name: "logo-recette.png",
        mimeType: "image/png",
        buffer: await readFile(
          join(__dirname,"../public/social-card.png"),
        ),
      });
    const response = await uploaded;
    expect(response.status()).toBe(201);
    asset = await response.json();
    await expect(
      page.getByRole("combobox", {name:"Image sélectionnée — Symbole du site",exact:true}),
    ).toHaveValue(asset.url);
    expect(
      (await (await page.request.get("/api/content")).json()).settings.logoIcon,
    ).toBe(original.settings.logoIcon);
    await page
      .getByRole("button", { name: "Enregistrer les modifications" })
      .click();
    await expect(page.getByRole("status")).toContainText(
      "enregistré et vérifié",
    );
    const visitor = await page.context().newPage();
    await visitor.goto("/");
    await expect(visitor.locator("header .brand img")).toHaveAttribute(
      "src",
      asset.url,
    );
    const image = await request.get(asset.url);
    expect(image.status()).toBe(200);
    expect(image.headers()["content-type"]).toContain("image/png");
    await expect(
      page.getByRole("button", {
        name: "Supprimer logo-recette.png",
        exact: true,
      }),
    ).toBeDisabled();
    const conflict = await page.request.delete("/api/admin/media/" + asset.id, {
      headers: { "X-XSRF-TOKEN": await token() },
    });
    expect(conflict.status()).toBe(409);
    for (const label of [
      "Logo de présentation",
      "Visuel d’accueil",
      "Image de partage",
    ])
      await page
        .getByRole("combobox", {name:"Image sélectionnée — " + label,exact:true})
        .selectOption(asset.url);
    await page
      .getByRole("button", { name: "Enregistrer les modifications" })
      .click();
    await expect(page.locator(".save-state")).toHaveText(
      "Aucune modification en attente",
    );
    await visitor.reload();
    await expect(visitor.locator(".hero-custom-image")).toHaveAttribute(
      "src",
      asset.url,
    );
    await expect(visitor.locator('meta[property="og:image"]')).toHaveAttribute(
      "content",
      new RegExp(asset.filename + "$"),
    );
    page.once("dialog", (dialog) => dialog.accept());
    await page
      .getByRole("button", {
        name: "Restaurer toutes les images d’origine",
        exact: true,
      })
      .click();
    await page
      .getByRole("button", { name: "Enregistrer les modifications" })
      .click();
    await expect(page.locator(".save-state")).toHaveText(
      "Aucune modification en attente",
    );
    await visitor.reload();
    await expect(visitor.locator("header .brand img")).toHaveAttribute(
      "src",
      "/logo.svg",
    );
    await expect(visitor.locator(".hero-custom-image")).toHaveCount(0);
    expect((await request.get("/logo.svg")).status()).toBe(200);
    await page.reload();
    await page.getByRole("tab", { name: "Contenu du site" }).click();
    await page
      .locator("summary")
      .filter({ hasText: "Images et logos" })
      .click();
    await expect(
      page.getByRole("button", {
        name: "Supprimer logo-recette.png",
        exact: true,
      }),
    ).toBeEnabled();
    page.once("dialog", (dialog) => dialog.accept());
    await page
      .getByRole("button", { name: "Supprimer logo-recette.png", exact: true })
      .click();
    await expect(
      page.locator(".media-asset").filter({ hasText: "logo-recette.png" }),
    ).toHaveCount(0);
    expect((await request.get(asset.url)).status()).toBe(404);
    await visitor.close();
  } finally {
    const csrf = await token();
    const restored = await page.request.put("/api/admin/content", {
      headers: { "X-XSRF-TOKEN": csrf },
      data: {
        headline: original.headline,
        intro: original.intro,
        available: original.available,
        settings: original.settings,
      },
    });
    expect(restored.ok()).toBeTruthy();
    if (asset) {
      const deleted = await page.request.delete(
        "/api/admin/media/" + asset.id,
        { headers: { "X-XSRF-TOKEN": csrf } },
      );
      expect([204, 404]).toContain(deleted.status());
    }
  }
});
