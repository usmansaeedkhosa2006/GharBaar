// =====================================================================
// [BL - DOMAIN] Report  (UC-05 Report Issue, UC-08 Resolve Report)
// Expert: validates and resolves itself.
// =====================================================================
import { BusinessException } from '../BusinessException';
import { REPORT_TYPES, type ReportStatus } from './constants';
import type { User } from './users';

export class Report {
  private reportID = 0;
  private status: ReportStatus = 'OPEN';
  private resolutionRemarks: string | null = null;
  private dateTime = new Date();

  private constructor(private reporter: User, private type: string, private description: string) {}

  /** created through User.fileReport() */
  static create(reporter: User, type: string, description: string): Report {
    if (!REPORT_TYPES.includes(type)) throw new BusinessException('Please select an issue type.');
    if (!description?.trim()) throw new BusinessException('Please describe the issue.');            // ext. 4a
    if (description.trim().length < 10) throw new BusinessException('Description must be at least 10 characters.');
    if (description.length > 2000) throw new BusinessException('Description must be under 2000 characters.');
    return new Report(reporter, type, description.trim());
  }

  static restore(id: number, reporter: User, type: string, description: string, status: ReportStatus,
                 remarks: string | null, dateTime: Date) {
    const r = new Report(reporter, type, description);
    Object.assign(r, { reportID: id, status, resolutionRemarks: remarks, dateTime });
    return r;
  }

  resolve(remarks: string) {
    if (!this.isOpen()) throw new BusinessException('This report is already resolved.');
    if (!remarks?.trim()) throw new BusinessException('Please enter the resolution remarks.');      // ext. 5a
    this.resolutionRemarks = remarks.trim();
    this.status = 'RESOLVED';
  }

  isOpen() { return this.status === 'OPEN'; }
  assignId(id: number) { if (!this.reportID) this.reportID = id; }
  getReportID() { return this.reportID; }
  getReporter() { return this.reporter; }
  getType() { return this.type; }
  getDescription() { return this.description; }
  getStatus() { return this.status; }
  getResolutionRemarks() { return this.resolutionRemarks; }

  toJSON() {
    return {
      reportID: this.reportID, type: this.type, description: this.description, status: this.status,
      resolutionRemarks: this.resolutionRemarks, dateTime: this.dateTime,
      reporter: { name: this.reporter.getName(), username: this.reporter.getUsername(), phone: this.reporter.getPhone(),
        role: this.reporter.getRole() },
    };
  }
}
