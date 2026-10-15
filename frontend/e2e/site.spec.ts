import {test,expect} from '@playwright/test';
test('homepage, location and contact validation',async({page})=>{
 await page.goto('/');
 await expect(page.getByRole('heading',{level:1})).toContainText('Le numérique');
 await page.getByRole('link',{name:'Parlons de votre projet'}).first().click();
 await expect(page.locator('address')).toContainText('78500 Sartrouville');
 await expect(page.getByRole('button',{name:'Envoyer mon message'})).toBeDisabled();
 await page.getByLabel('Votre nom *',{exact:true}).fill('Test navigateur');
 await page.getByLabel('Votre adresse e-mail *').fill('test@example.com');
 await page.getByLabel('Quelques mots sur votre projet *').fill('Un projet de développement et de conseil.');
 await page.getByRole('checkbox').check();
 await expect(page.getByRole('button',{name:'Envoyer mon message'})).toBeEnabled();
 await page.getByRole('button',{name:'Politique de confidentialité',exact:true}).click();
 await expect(page.getByRole('dialog')).toBeVisible();
 await page.getByRole('button',{name:'Fermer',exact:true}).click();
 await page.screenshot({path:'/tmp/codenova-desktop.png',fullPage:true});
});
test('mobile layout and navigation',async({page})=>{
 await page.setViewportSize({width:390,height:844});await page.goto('/');
 await page.getByRole('button',{name:'Ouvrir la navigation'}).click();
 await page.getByRole('link',{name:'Nos expertises',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Vos enjeux. Notre terrain de jeu.'})).toBeVisible();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
 await page.screenshot({path:'/tmp/codenova-mobile.png',fullPage:true});
});
test('administrator login is separate',async({page})=>{await page.goto('/admin');await expect(page.getByLabel('Mot de passe')).toBeVisible();await expect(page.getByRole('button',{name:'Se connecter'})).toBeDisabled();});
