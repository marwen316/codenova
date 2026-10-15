import { bootstrapApplication, BootstrapContext } from '@angular/platform-browser';
import { provideServerRendering, withRoutes, RenderMode } from '@angular/ssr';
import { App } from './main';
export default (context: BootstrapContext) => bootstrapApplication(App, {providers:[provideServerRendering(withRoutes([{path:'',renderMode:RenderMode.Prerender},{path:'admin',renderMode:RenderMode.Client}]))]}, context);
