import { test, expect } from "@playwright/test";
test("admin creates, edits, links and deletes expertise cards persistently", async ({page, request}) => {
  const original = await (await request.get('/api/content')).json();
  await page.goto('/admin');
  await page.getByLabel('Identifiant').fill(process.env['ADMIN_USERNAME']!);
  await page.getByLabel('Mot de passe').fill(process.env['ADMIN_PASSWORD']!);
  await page.getByRole('button', {name:'Se connecter'}).click();
  await page.getByRole('tab', {name:'Contenu du site'}).click();
  const save = async () => {
    await page.getByRole('button', {name:'Enregistrer les modifications'}).click();
    await expect(page.getByRole('status')).toContainText('enregistré et vérifié');
  };
  try {
    await expect(page.getByRole('button', {name:'Ajouter une rubrique',exact:true})).toBeVisible();
    const count = await page.getByLabel('Titre de la rubrique', {exact:true}).count();
    await page.getByRole('button', {name:'Ajouter une rubrique',exact:true}).click();
    await expect(page.getByLabel('Titre de la rubrique', {exact:true})).toHaveCount(count+1);
    await page.getByLabel('Titre de la rubrique', {exact:true}).last().fill('Audit sécurité test');
    await page.getByLabel('Contenu de la rubrique', {exact:true}).last().fill('Une prestation personnalisée pour votre entreprise.');
    await page.getByLabel('Lien de la rubrique', {exact:true}).last().fill('javascript:alert(1)');
    await expect(page.getByRole('button', {name:'Enregistrer les modifications'})).toBeDisabled();
    await page.getByLabel('Lien de la rubrique', {exact:true}).last().fill('https://example.com/audit');
    await save();
    await page.reload();
    await page.getByRole('tab', {name:'Contenu du site'}).click();
    await expect(page.getByLabel('Titre de la rubrique', {exact:true})).toHaveCount(count+1);
    const visitor = await page.context().newPage();
    await visitor.goto('/');
    const card = visitor.locator('article.service').filter({hasText:'Audit sécurité test'});
    await expect(card).toContainText('Une prestation personnalisée');
    await expect(card.getByRole('link')).toHaveAttribute('href','https://example.com/audit');
    page.once('dialog', dialog=>dialog.accept());
    await page.getByRole('button', {name:'Supprimer cette rubrique',exact:true}).last().click();
    await save();
    await visitor.reload();
    await expect(visitor.locator('article.service')).toHaveCount(count);
    await visitor.close();
  } finally {
    const csrf = await (await page.request.get('/api/csrf')).json();
    const restored = await page.request.put('/api/admin/content', {headers:{'X-XSRF-TOKEN':csrf.token},data:{headline:original.headline,intro:original.intro,available:original.available,settings:original.settings}});
    expect(restored.ok()).toBeTruthy();
  }
});
