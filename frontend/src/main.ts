import { Component, ChangeDetectorRef, inject, OnInit } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
interface Message { id:number; name:string; email:string; company:string; subject:string; message:string; createdAt:string; read:boolean }
@Component({selector:'app-root',standalone:true,imports:[FormsModule],templateUrl:'./app.html'})
export class App implements OnInit {
 private cdr=inject(ChangeDetectorRef);
 ready=false; menu=false; admin=typeof window!=='undefined' && window.location.pathname==='/admin'; legal=''; year=new Date().getFullYear();
 content={headline:'Votre ambition. Notre code.',intro:'Nous transformons vos défis numériques en solutions utiles, solides et durables. Du premier conseil à la dernière ligne de code.',available:true};
 form={name:'',email:'',company:'',subject:'Développement web',message:'',consent:false,website:''};
 busy=false; notice=''; error=false; authenticated=false; username=''; password=''; messages:Message[]=[]; filter='all'; selected:Message|null=null; csrf='';
 closeMenu(): void { this.menu=false; }
 get visible(){return this.messages.filter(m=>this.filter!=='unread'||!m.read)}
 get unread(){return this.messages.filter(m=>!m.read).length}
 async ngOnInit(){if(typeof window==='undefined')return;this.ready=true;if(this.admin){const meta=document.createElement('meta');meta.name='robots';meta.content='noindex, nofollow';document.head.appendChild(meta);}try{const r=await fetch('/api/content');if(r.ok)this.content=await r.json();}catch{} if(this.admin){await this.token();await this.load(false);}this.cdr.markForCheck();}
 async token(){try{const r=await fetch('/api/csrf');if(r.ok)this.csrf=(await r.json()).token;}catch{}}
 async send(){this.busy=true;this.notice='';try{const r=await fetch('/api/contact',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(this.form)});if(!r.ok)throw Error();this.notice='Merci ! Votre message a bien été envoyé. Nous reviendrons vers vous prochainement.';this.error=false;this.form={name:'',email:'',company:'',subject:'Développement web',message:'',consent:false,website:''};}catch{this.error=true;this.notice='L’envoi est indisponible pour le moment. Veuillez réessayer dans quelques instants.';}finally{this.busy=false;this.cdr.markForCheck();}}
 async login(){this.busy=true;this.notice='';try{await this.token();const r=await fetch('/api/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded','X-XSRF-TOKEN':this.csrf},body:new URLSearchParams({username:this.username,password:this.password})});if(!r.ok)throw Error();this.password='';await this.token();await this.load();}catch{this.notice='Connexion impossible. Vérifiez vos identifiants et la disponibilité du service.';}finally{this.busy=false;this.cdr.markForCheck();}}
 async load(report=true){try{const r=await fetch('/api/admin/messages');if(!r.ok)throw Error();this.messages=await r.json();this.authenticated=true;}catch{this.authenticated=false;if(report)this.notice='Session expirée ou service indisponible.';}finally{this.cdr.markForCheck();}}
 async open(m:Message){this.selected=m;if(!m.read){try{const r=await fetch('/api/admin/messages/'+m.id+'/read',{method:'PATCH',headers:{'X-XSRF-TOKEN':this.csrf}});if(!r.ok)throw Error();m.read=true;}catch{this.notice='Impossible de marquer ce message comme lu.';}finally{this.cdr.markForCheck();}}}
 async remove(m:Message){if(!confirm('Supprimer définitivement ce message ?'))return;try{const r=await fetch('/api/admin/messages/'+m.id,{method:'DELETE',headers:{'X-XSRF-TOKEN':this.csrf}});if(!r.ok)throw Error();this.selected=null;await this.load();}catch{this.notice='La suppression a échoué.';}finally{this.cdr.markForCheck();}}
 async save(){try{const r=await fetch('/api/admin/content',{method:'PUT',headers:{'Content-Type':'application/json','X-XSRF-TOKEN':this.csrf},body:JSON.stringify(this.content)});if(!r.ok)throw Error();this.notice='Le contenu du site a été enregistré.';}catch{this.notice='Impossible d’enregistrer le contenu.';}finally{this.cdr.markForCheck();}}
 async logout(){await fetch('/api/logout',{method:'POST',headers:{'X-XSRF-TOKEN':this.csrf}});this.authenticated=false;this.messages=[];await this.token();this.cdr.markForCheck();}
}
