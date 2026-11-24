// =====================================================================
// [DB LAYER] DBConnection  -  «singleton»  (same role as DBConnection.java)
// One shared PostgreSQL connection pool per server instance.
// =====================================================================
import { Pool, PoolClient, types } from 'pg';

// ---------------------------------------------------------------------
// [TIME ZONE] The database stores local Pakistan time (Asia/Karachi),
// same as the JavaFX app. Read TIMESTAMP / DATE as PKT, write as PKT.
// ---------------------------------------------------------------------
types.setTypeParser(1114, (s: string) => new Date(s.replace(' ', 'T') + '+05:00'));   // TIMESTAMP
types.setTypeParser(1082, (s: string) => new Date(s + 'T00:00:00+05:00'));            // DATE

const PKT_MS = 5 * 60 * 60 * 1000;
/** Date -> 'YYYY-MM-DD' in Pakistan time */
export const pkDate = (d: Date) => new Date(d.getTime() + PKT_MS).toISOString().slice(0, 10);
/** Date -> 'YYYY-MM-DD HH:mm:ss' in Pakistan time */
export const pkDateTime = (d: Date) => new Date(d.getTime() + PKT_MS).toISOString().slice(0, 19).replace('T', ' ');

export class DBConnection {
  private static instance: DBConnection | null = null;
  private readonly pool: Pool;

  private constructor() {
    // direct (unpooled) connection: keeps the database time zone (Asia/Karachi)
    const url = process.env.DATABASE_URL_UNPOOLED || process.env.DATABASE_URL;
    if (!url) throw new Error('DATABASE_URL is not set');
    this.pool = new Pool({ connectionString: url, max: 5, idleTimeoutMillis: 5_000, keepAlive: true });
    // Neon closes idle connections; drop them quietly instead of crashing the server
    this.pool.on('error', (e) => console.warn('[DB] idle connection closed:', e.message));
  }

  static getInstance(): DBConnection {
    // keep one pool across hot reloads in development
    const g = globalThis as unknown as { __gbDb?: DBConnection };
    if (!g.__gbDb) g.__gbDb = new DBConnection();
    DBConnection.instance = g.__gbDb;
    return DBConnection.instance;
  }

  getPool(): Pool { return this.pool; }
  getClient(): Promise<PoolClient> { return this.pool.connect(); }
}

/** Wraps every database failure so the UI never sees raw SQL errors. */
export class DataAccessException extends Error {
  constructor(message: string, readonly cause?: unknown) { super(message); }
}
