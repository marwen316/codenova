import { test, expect } from "@playwright/test";
for (const [status, message] of [
  [401, "Identifiant ou mot de passe incorrect"],
  [403, "jeton de sécurité"],
  [429, "Trop de tentatives"],
  [502, "temporairement indisponible"],
] as const) {
  test("login reports HTTP " + status + " accurately", async ({ page }) => {
    await page.route("**/api/login", (route) =>
      route.fulfill({ status, body: "{}", contentType: "application/json" }),
    );
    await page.goto("/admin");
    await page.getByLabel("Identifiant").fill("diagnostic");
    await page.getByLabel("Mot de passe").fill("not-a-real-password");
    await page.getByRole("button", { name: "Se connecter" }).click();
    await expect(page.getByRole("status")).toContainText(message);
    await expect(page.getByRole("status")).toContainText("HTTP " + status);
    await expect(
      page.getByRole("button", { name: "Se connecter" }),
    ).toBeEnabled();
  });
}
test("an unavailable CSRF endpoint stops login and reports a service error", async ({
  page,
}) => {
  let posted = false;
  await page.route("**/api/login", (route) => {
    posted = true;
    return route.abort();
  });
  await page.route("**/api/csrf", (route) =>
    route.fulfill({ status: 503, body: "{}", contentType: "application/json" }),
  );
  await page.goto("/admin");
  await page.getByLabel("Identifiant").fill("diagnostic");
  await page.getByLabel("Mot de passe").fill("not-a-real-password");
  await page.getByRole("button", { name: "Se connecter" }).click();
  await expect(page.getByRole("status")).toContainText("HTTP 503");
  expect(posted).toBe(false);
});
test("accepted credentials without a retained session get a distinct explanation", async ({
  page,
}) => {
  await page.route("**/api/login", (route) => route.fulfill({ status: 204 }));
  await page.goto("/admin");
  await page.getByLabel("Identifiant").fill("diagnostic");
  await page.getByLabel("Mot de passe").fill("not-a-real-password");
  await page.getByRole("button", { name: "Se connecter" }).click();
  await expect(page.getByRole("status")).toContainText(
    "session n’est pas conservée",
  );
  await expect(page.getByRole("status")).toContainText("HTTP 401");
});
