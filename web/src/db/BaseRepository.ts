// =====================================================================
// [DB LAYER] BaseRepository (abstract) - JDBC-style helpers inherited
// by every repository: query / queryOne / insert / execute / transaction
// =====================================================================
import type { PoolClient } from 'pg';
import { DBConnection, DataAccessException } from './DBConnection';

type Executor = { query: PoolClient['query'] };

export abstract class BaseRepository {
  protected get db() { return DBConnection.getInstance().getPool(); }

  protected async query<T>(sql: string, params: unknown[] = [], map: (r: any) => T, ex: Executor = this.db): Promise<T[]> {
    try {
      const res = await ex.query(sql, params as any[]);
      return res.rows.map(map);
    } catch (e) {
      throw new DataAccessException('Database read failed. Please try again.', e);
    }
  }

  protected async queryOne<T>(sql: string, params: unknown[], map: (r: any) => T, ex?: Executor): Promise<T | null> {
    const rows = await this.query(sql, params, map, ex);
    return rows[0] ?? null;
  }

  protected async count(sql: string, params: unknown[] = []): Promise<number> {
    return (await this.queryOne(sql, params, (r) => Number(r.count))) ?? 0;
  }

  /** INSERT ... RETURNING <idColumn> */
  protected async insert(sql: string, params: unknown[], ex: Executor = this.db): Promise<number> {
    try {
      const res = await ex.query(sql, params as any[]);
      return Number(Object.values(res.rows[0])[0]);
    } catch (e) {
      throw new DataAccessException('Database write failed. Please try again.', e);
    }
  }

  protected async execute(sql: string, params: unknown[] = [], ex: Executor = this.db): Promise<number> {
    try {
      return (await ex.query(sql, params as any[])).rowCount ?? 0;
    } catch (e) {
      throw new DataAccessException('Database update failed. Please try again.', e);
    }
  }

  /** Runs several statements atomically. */
  protected async inTransaction<T>(work: (c: PoolClient) => Promise<T>): Promise<T> {
    const c = await DBConnection.getInstance().getClient();
    try {
      await c.query('BEGIN');
      const r = await work(c);
      await c.query('COMMIT');
      return r;
    } catch (e) {
      await c.query('ROLLBACK').catch(() => {});
      if (e instanceof DataAccessException) throw e;
      throw new DataAccessException('Database transaction failed.', e);
    } finally {
      c.release();
    }
  }
}
