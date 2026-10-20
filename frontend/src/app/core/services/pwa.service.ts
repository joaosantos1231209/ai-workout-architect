import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class PwaService {
  private deferredPrompt: any = null;

  // Sinais de estado para a UI
  canInstall = signal<boolean>(false);
  isIOS = signal<boolean>(false);
  isStandalone = signal<boolean>(false);
  bannerDismissed = signal<boolean>(false);

  constructor() {
    this.checkStandalone();
    this.detectPlatform();
    this.listenForInstallPrompt();
  }

  private checkStandalone(): void {
    if (typeof window !== 'undefined') {
      const isStandaloneMode =
        window.matchMedia('(display-mode: standalone)').matches ||
        (window.navigator as any).standalone === true;
      this.isStandalone.set(isStandaloneMode);
    }
  }

  private detectPlatform(): void {
    if (typeof window !== 'undefined') {
      const userAgent = window.navigator.userAgent.toLowerCase();
      const isIosDevice = /iphone|ipad|ipod/.test(userAgent);
      this.isIOS.set(isIosDevice);
    }
  }

  private listenForInstallPrompt(): void {
    if (typeof window !== 'undefined') {
      window.addEventListener('beforeinstallprompt', (e: Event) => {
        // Previne o prompt padrão do Chrome para usarmos o nosso botão estilizado
        e.preventDefault();
        this.deferredPrompt = e;
        this.canInstall.set(true);
      });

      window.addEventListener('appinstalled', () => {
        this.canInstall.set(false);
        this.deferredPrompt = null;
        console.log('PWA instalada com sucesso no telemóvel!');
      });
    }
  }

  async promptInstall(): Promise<void> {
    if (!this.deferredPrompt) return;

    this.deferredPrompt.prompt();
    const { outcome } = await this.deferredPrompt.userChoice;
    console.log(`Resposta do utilizador à instalação: ${outcome}`);

    if (outcome === 'accepted') {
      this.canInstall.set(false);
    }
    this.deferredPrompt = null;
  }

  dismiss(): void {
    this.bannerDismissed.set(true);
  }
}
