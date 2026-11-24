// =====================================================================
// [BL - SERVICE] FileStorage «interface» + DatabaseFileStorage
// Photos and proof documents are stored in table stored_files so both
// the web app and the JavaFX app can show them. Path format "db:<id>".
// =====================================================================
import type { FileRepository } from '../../db/repositories';

export interface StoredFile { fileName: string; contentType: string; data: Buffer; adminOnly: boolean }

export interface FileStorage {
  isSupportedImage(files: File[]): boolean;
  isSupportedDocument(file: File | null): boolean;
  /** stores listing photos, returns paths joined by ';' */
  store(files: File[], uploaderId: number): Promise<string>;
  /** stores one admin-only proof document */
  storeDocument(file: File, uploaderId: number): Promise<string>;
  load(path: string): Promise<StoredFile | null>;
}

const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const DOC_TYPES = ['application/pdf', 'image/jpeg', 'image/png'];
export const MAX_BYTES = 4 * 1024 * 1024;

export class DatabaseFileStorage implements FileStorage {
  constructor(private repo: FileRepository) {}

  isSupportedImage(files: File[]) {
    return files.length > 0 && files.every((f) => IMAGE_TYPES.includes(f.type) && f.size <= MAX_BYTES);
  }

  isSupportedDocument(file: File | null) {
    return !!file && DOC_TYPES.includes(file.type) && file.size <= MAX_BYTES;
  }

  async store(files: File[], uploaderId: number) {
    const paths: string[] = [];
    for (const f of files) paths.push(await this.save(f, false, uploaderId));
    return paths.join(';');
  }

  storeDocument(file: File, uploaderId: number) { return this.save(file, true, uploaderId); }

  private async save(f: File, adminOnly: boolean, uploaderId: number) {
    const id = await this.repo.insertFile(f.name.slice(0, 200), f.type, Buffer.from(await f.arrayBuffer()), adminOnly, uploaderId);
    return `db:${id}`;
  }

  async load(path: string) {
    if (!path?.startsWith('db:')) return null;
    return this.repo.findById(Number(path.slice(3)));
  }
}
