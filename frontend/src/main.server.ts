import { bootstrapApplication, BootstrapContext, provideClientHydration } from '@angular/platform-browser';
import { provideServerRendering, withRoutes, RenderMode } from '@angular/ssr';
import { App } from './main';
export default (context: BootstrapContext) => bootstrapApplication(App, {providers:[provideClientHydration(),provideServerRendering(withRoutes([{path:'',renderMode:RenderMode.Prerender},{path:'admin',renderMode:RenderMode.Client}]))]}, context);
