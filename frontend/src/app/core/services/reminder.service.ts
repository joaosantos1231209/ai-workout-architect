import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ReminderService {
  private readonly REMINDER_KEY = 'ai_workout_reminder_enabled';
  private timerId: any = null;

  isReminderEnabled = signal<boolean>(false);
  notificationPermission = signal<NotificationPermission>('default');

  constructor() {
    this.checkInitialStatus();
  }

  private checkInitialStatus(): void {
    if (typeof window !== 'undefined' && 'Notification' in window) {
      this.notificationPermission.set(Notification.permission);
      const saved = localStorage.getItem(this.REMINDER_KEY);
      if (saved === 'true' && Notification.permission === 'granted') {
        this.isReminderEnabled.set(true);
        this.scheduleNextReminder();
      }
    }
  }

  /**
   * Ativa ou desativa o lembrete diário para as 09:30.
   */
  async toggleReminder(): Promise<boolean> {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      alert('O teu navegador não suporta notificações web.');
      return false;
    }

    if (this.isReminderEnabled()) {
      // Desativar
      this.isReminderEnabled.set(false);
      localStorage.setItem(this.REMINDER_KEY, 'false');
      if (this.timerId) {
        clearTimeout(this.timerId);
        this.timerId = null;
      }
      return false;
    } else {
      // Ativar - Pedir permissão caso necessário
      let perm = Notification.permission;
      if (perm !== 'granted') {
        perm = await Notification.requestPermission();
        this.notificationPermission.set(perm);
      }

      if (perm === 'granted') {
        this.isReminderEnabled.set(true);
        localStorage.setItem(this.REMINDER_KEY, 'true');
        this.scheduleNextReminder();
        this.triggerNotification(
          'Lembrete Diário Ativado! 🔔',
          'Todos os dias às 09:30 serás notificado para fazer o teu treino em casa 💪'
        );
        return true;
      } else {
        alert('É necessário conceder permissão de notificações para receber o lembrete.');
        return false;
      }
    }
  }

  /**
   * Calcula o tempo exato até às 09:30 (de hoje ou de amanhã) e agenda o disparo.
   */
  private scheduleNextReminder(): void {
    if (this.timerId) {
      clearTimeout(this.timerId);
    }

    const now = new Date();
    const target = new Date();
    target.setHours(9, 30, 0, 0);

    // Se já passaram das 09:30 hoje, agenda para amanhã às 09:30
    if (now.getTime() >= target.getTime()) {
      target.setDate(target.getDate() + 1);
    }

    const delay = target.getTime() - now.getTime();

    this.timerId = setTimeout(() => {
      this.triggerNotification(
        'Hora do Teu Treino em Casa! 💪',
        'São 09:30! O teu treino personalizado de hoje está à tua espera na app. Vamos lá cumprir o objetivo?'
      );
      // Re-agendar para o dia seguinte
      this.scheduleNextReminder();
    }, delay);
  }

  /**
   * Dispara uma notificação web ou via Service Worker da PWA.
   */
  triggerNotification(title: string, body: string): void {
    if (typeof window === 'undefined' || !('Notification' in window)) return;
    if (Notification.permission !== 'granted') return;

    if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
      navigator.serviceWorker.ready.then((reg) => {
        reg.showNotification(title, {
          body,
          icon: '/favicon.ico',
          badge: '/favicon.ico',
          vibrate: [200, 100, 200]
        } as any);
      }).catch(() => {
        new Notification(title, { body, icon: '/favicon.ico' });
      });
    } else {
      new Notification(title, { body, icon: '/favicon.ico' });
    }
  }
}
