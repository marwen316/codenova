import { bootstrapApplication, provideClientHydration } from '@angular/platform-browser';
import { App } from './main';
bootstrapApplication(App, {providers:[provideClientHydration()]}).catch(console.error);
