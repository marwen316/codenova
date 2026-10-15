import {test, expect} from '@playwright/test';

test.use({reducedMotion:'reduce'});

const sections = [
  {label:'Nos expertises', id:'expertises'},
  {label:'Notre approche', id:'approche'},
  {label:'À propos', id:'apropos'},
];

for (const width of [1440, 900, 390]) {
  test(`header links scroll to their sections at ${width}px`, async ({page}) => {
    await page.setViewportSize({width,height:900});
    const links = width <= 700 ? [...sections, {label:'Contact →',id:'contact'}] : sections;
    for (const link of links) {
      await page.goto('/');
      // Wait for Angular's click handlers, rather than testing only the static HTML.
      await expect(page.locator('header .menu-button')).toBeEnabled();
      if (width <= 1100) await page.getByRole('button',{name:'Ouvrir la navigation'}).click();
      await page.locator('header nav').getByRole('link',{name:link.label,exact:true}).click();
      await expect(page).toHaveURL(new RegExp('#'+link.id+'$'));
      await expect.poll(() => page.locator('#'+link.id).evaluate(element => {
        const top = element.getBoundingClientRect().top;
        return top >= 0 && top < window.innerHeight;
      })).toBe(true);
      if (width <= 1100) await expect(page.getByRole('button',{name:'Ouvrir la navigation'})).toHaveAttribute('aria-expanded','false');
    }
  });
}
