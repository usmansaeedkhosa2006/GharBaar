// =====================================================================
// [BL] AppContext - composition root (wires repositories, services and
// controllers once). API routes only ask AppContext for a controller.
// =====================================================================
import {
  FileRepository, ListingRepository, NotificationRepository, ReportRepository, TenancyRepository,
  UserRepository, VisitRequestRepository,
} from '../db/repositories';
import { PasswordHasher } from './service/PasswordHasher';
import { InAppNotifier } from './service/Notifier';
import { DatabaseFileStorage } from './service/FileStorage';
import {
  AdminController, AuthController, FileController, ListingController, NotificationController, PropertySearchController,
  ReportController, TenancyController, VerificationController, VisitController,
} from './controllers';

class AppContext {
  // ---- DB layer ----
  private userRepo = new UserRepository();
  private listingRepo = new ListingRepository();
  private reportRepo = new ReportRepository();
  private visitRepo = new VisitRequestRepository(this.listingRepo);
  private tenancyRepo = new TenancyRepository(this.listingRepo);
  private notificationRepo = new NotificationRepository();
  private fileRepo = new FileRepository();

  // ---- services ----
  private hasher = new PasswordHasher();
  private notifier = new InAppNotifier(this.notificationRepo);
  private storage = new DatabaseFileStorage(this.fileRepo);

  // ---- controllers ----
  readonly auth = new AuthController(this.userRepo, this.hasher);
  readonly listings = new ListingController(this.listingRepo, this.storage, this.notifier);
  readonly search = new PropertySearchController(this.listingRepo, this.tenancyRepo);
  readonly visits = new VisitController(this.listingRepo, this.visitRepo, this.notifier);
  readonly tenancy = new TenancyController(this.tenancyRepo, this.listingRepo, this.userRepo, this.storage, this.notifier);
  readonly reports = new ReportController(this.reportRepo, this.notifier);
  readonly verification = new VerificationController(this.listingRepo, this.userRepo, this.tenancyRepo, this.storage, this.notifier);
  readonly admin = new AdminController(this.userRepo, this.listingRepo, this.reportRepo, this.tenancyRepo, this.notifier);
  readonly notifications = new NotificationController(this.notificationRepo);
  readonly files = new FileController(this.storage);
}

let instance: AppContext | null = null;
export const app = () => (instance ??= new AppContext());
