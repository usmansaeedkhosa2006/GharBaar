// =====================================================================
// [BL - SERVICE] Notifier «interface» + InAppNotifier
// Protected Variations: controllers only know the interface.
// =====================================================================
import type { User } from '../domain/users';
import type { NotificationRepository } from '../../db/repositories';

export interface Notifier {
  notify(recipient: User | number, message: string): Promise<void>;
  notifyAdmins(message: string): Promise<void>;
}

export class InAppNotifier implements Notifier {
  constructor(private repo: NotificationRepository) {}

  async notify(recipient: User | number, message: string) {
    await this.repo.save(typeof recipient === 'number' ? recipient : recipient.getUserID(), message);
  }

  async notifyAdmins(message: string) { await this.repo.saveForRole('ADMIN', message); }
}
