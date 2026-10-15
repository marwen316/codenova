import { Component, ChangeDetectorRef, inject, OnInit } from "@angular/core";
import { SiteContent, contentDefaults, contentGroups } from "./content-model";
import { FormsModule } from "@angular/forms";
interface Message {
  id: number;
  name: string;
  email: string;
  company: string;
  subject: string;
  message: string;
  createdAt: string;
  read: boolean;
}
@Component({
  selector: "app-root",
  standalone: true,
  imports: [FormsModule],
  templateUrl: "./app.html",
})
export class App implements OnInit {
  private cdr = inject(ChangeDetectorRef);
  ready = false;
  menu = false;
  admin =
    typeof window !== "undefined" && window.location.pathname === "/admin";
  legal = "";
  year = new Date().getFullYear();
  content: SiteContent = {
    headline: "Votre ambition. Notre code.",
    intro:
      "Nous transformons vos défis numériques en solutions utiles, solides et durables. Du premier conseil à la dernière ligne de code.",
    available: true,
    settings: { ...contentDefaults },
  };
  draft: SiteContent = structuredClone(this.content);
  adminTab: "messages" | "content" = "messages";
  groups = contentGroups;
  contentSearch = "";
  saving = false;
  contentLoaded = false;
  form = {
    name: "",
    email: "",
    company: "",
    subject: contentDefaults["service2Title"],
    message: "",
    consent: false,
    website: "",
  };
  busy = false;
  notice = "";
  error = false;
  authenticated = false;
  username = "";
  password = "";
  messages: Message[] = [];
  filter = "all";
  selected: Message | null = null;
  csrf = "";
  closeMenu(): void {
    this.menu = false;
  }
  get visible() {
    return this.messages.filter((m) => this.filter !== "unread" || !m.read);
  }
  get unread() {
    return this.messages.filter((m) => !m.read).length;
  }
  async ngOnInit() {
    if (typeof window === "undefined") return;
    this.ready = true;
    if (this.admin) {
      const meta = document.createElement("meta");
      meta.name = "robots";
      meta.content = "noindex, nofollow";
      document.head.appendChild(meta);
    }
    await this.reloadContent();
    if (this.admin) {
      await this.token();
      await this.load(false);
    }
    this.cdr.markForCheck();
  }
  t(key: string): string {
    return this.content.settings[key] ?? contentDefaults[key] ?? "";
  }
  get mapUrl() {
    return (
      "https://www.openstreetmap.org/search?query=" +
      encodeURIComponent(
        [
          this.t("street"),
          this.t("postalCode"),
          this.t("city"),
          this.t("country"),
        ].join(" "),
      )
    );
  }
  get phoneUrl() {
    return "tel:" + this.t("phone").replace(/[^+\d]/g, "");
  }
  get filteredGroups() {
    const query = this.contentSearch.trim().toLocaleLowerCase("fr");
    return this.groups
      .map((group) => ({
        ...group,
        fields: group.fields.filter(
          (field) =>
            !query ||
            (group.title + " " + field.label)
              .toLocaleLowerCase("fr")
              .includes(query),
        ),
      }))
      .filter((group) => group.fields.length);
  }
  switchTab(tab: "messages" | "content", focus = false): void {
    this.adminTab = tab;
    if (focus) document.getElementById("tab-" + tab)?.focus();
  }
  resetDraft(): void {
    if (confirm("Annuler les modifications non enregistrées ?")) {
      this.draft = structuredClone(this.content);
      this.notice = "Modifications annulées.";
    }
  }
  async reloadContent() {
    try {
      const r = await fetch("/api/content");
      if (!r.ok) throw Error();
      const data = await r.json();
      this.content = {
        headline: data.headline,
        intro: data.intro,
        available: data.available,
        settings: { ...contentDefaults, ...data.settings },
      };
      this.draft = structuredClone(this.content);
      this.contentLoaded = true;
      if (!this.admin) this.form.subject = this.t("service2Title");
      this.updateMetadata();
    } catch {
      if (this.admin)
        this.notice =
          "Impossible de charger le contenu. Réessayez avant de modifier le site.";
    } finally {
      this.cdr.markForCheck();
    }
  }
  updateMetadata() {
    document.title = this.t("seoTitle");
    for (const [selector, value] of [
      ['meta[name="description"]', this.t("seoDescription")],
      ['meta[property="og:title"]', this.t("seoTitle")],
      ['meta[property="og:description"]', this.t("seoDescription")],
    ])
      document.querySelector(selector)?.setAttribute("content", value);
    const icon = document.querySelector('link[rel="icon"]');
    icon?.setAttribute("href", this.t("logoIcon"));
    const schema = document.querySelector('script[type="application/ld+json"]');
    if (schema)
      schema.textContent = JSON.stringify({
        "@context": "https://schema.org",
        "@type": "ProfessionalService",
        name: this.t("companyName"),
        description: this.t("seoDescription"),
        email: this.t("email"),
        telephone: this.t("phone"),
        address: {
          "@type": "PostalAddress",
          streetAddress: this.t("street"),
          postalCode: this.t("postalCode"),
          addressLocality: this.t("city"),
          addressCountry: this.t("country"),
        },
      });
  }
  async token() {
    try {
      const r = await fetch("/api/csrf");
      if (r.ok) this.csrf = (await r.json()).token;
    } catch {}
  }
  async send() {
    this.busy = true;
    this.notice = "";
    try {
      const r = await fetch("/api/contact", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(this.form),
      });
      if (!r.ok) throw Error();
      this.notice =
        "Merci ! Votre message a bien été envoyé. Nous reviendrons vers vous prochainement.";
      this.error = false;
      this.form = {
        name: "",
        email: "",
        company: "",
        subject: this.t("service2Title"),
        message: "",
        consent: false,
        website: "",
      };
    } catch {
      this.error = true;
      this.notice =
        "L’envoi est indisponible pour le moment. Veuillez réessayer dans quelques instants.";
    } finally {
      this.busy = false;
      this.cdr.markForCheck();
    }
  }
  async login() {
    this.busy = true;
    this.notice = "";
    try {
      await this.token();
      const r = await fetch("/api/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "X-XSRF-TOKEN": this.csrf,
        },
        body: new URLSearchParams({
          username: this.username,
          password: this.password,
        }),
      });
      if (!r.ok) throw Error();
      this.password = "";
      await this.token();
      await this.load();
    } catch {
      this.notice =
        "Connexion impossible. Vérifiez vos identifiants et la disponibilité du service.";
    } finally {
      this.busy = false;
      this.cdr.markForCheck();
    }
  }
  async load(report = true) {
    try {
      const r = await fetch("/api/admin/messages");
      if (!r.ok) throw Error();
      this.messages = await r.json();
      this.authenticated = true;
    } catch {
      this.authenticated = false;
      if (report) this.notice = "Session expirée ou service indisponible.";
    } finally {
      this.cdr.markForCheck();
    }
  }
  async open(m: Message) {
    this.selected = m;
    if (!m.read) {
      try {
        const r = await fetch("/api/admin/messages/" + m.id + "/read", {
          method: "PATCH",
          headers: { "X-XSRF-TOKEN": this.csrf },
        });
        if (!r.ok) throw Error();
        m.read = true;
      } catch {
        this.notice = "Impossible de marquer ce message comme lu.";
      } finally {
        this.cdr.markForCheck();
      }
    }
  }
  async remove(m: Message) {
    if (!confirm("Supprimer définitivement ce message ?")) return;
    try {
      const r = await fetch("/api/admin/messages/" + m.id, {
        method: "DELETE",
        headers: { "X-XSRF-TOKEN": this.csrf },
      });
      if (!r.ok) throw Error();
      this.selected = null;
      await this.load();
    } catch {
      this.notice = "La suppression a échoué.";
    } finally {
      this.cdr.markForCheck();
    }
  }
  async save() {
    if (!this.contentLoaded || this.saving) return;
    this.saving = true;
    this.notice = "";
    try {
      const r = await fetch("/api/admin/content", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "X-XSRF-TOKEN": this.csrf,
        },
        body: JSON.stringify(this.draft),
      });
      if (!r.ok) throw Error();
      const data = await r.json();
      this.content = {
        headline: data.headline,
        intro: data.intro,
        available: data.available,
        settings: { ...contentDefaults, ...data.settings },
      };
      this.draft = structuredClone(this.content);
      this.updateMetadata();
      this.notice = "Le contenu du site a été enregistré.";
    } catch {
      this.notice =
        "Impossible d’enregistrer. Vérifiez les champs et votre connexion ; vos modifications sont conservées.";
    } finally {
      this.saving = false;
      this.cdr.markForCheck();
    }
  }
  async logout() {
    await fetch("/api/logout", {
      method: "POST",
      headers: { "X-XSRF-TOKEN": this.csrf },
    });
    this.authenticated = false;
    this.messages = [];
    await this.token();
    this.cdr.markForCheck();
  }
}
