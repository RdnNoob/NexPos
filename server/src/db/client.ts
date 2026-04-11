import { Pool } from "pg";

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl:
    process.env.NODE_ENV === "production"
      ? { rejectUnauthorized: false }
      : false,
});

export async function initDb() {
  const client = await pool.connect();
  try {
    await client.query(`
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        email VARCHAR(255) UNIQUE NOT NULL,
        password VARCHAR(255) NOT NULL,
        name VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT NOW()
      );

      CREATE TABLE IF NOT EXISTS outlets (
        id SERIAL PRIMARY KEY,
        owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        name VARCHAR(255) NOT NULL,
        activation_code VARCHAR(50) UNIQUE NOT NULL,
        created_at TIMESTAMP DEFAULT NOW()
      );

      CREATE TABLE IF NOT EXISTS devices (
        id SERIAL PRIMARY KEY,
        owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        outlet_id INTEGER REFERENCES outlets(id) ON DELETE CASCADE,
        device_name VARCHAR(255) NOT NULL,
        device_id VARCHAR(255) UNIQUE NOT NULL,
        status VARCHAR(20) DEFAULT 'offline',
        last_seen TIMESTAMP,
        refresh_token TEXT,
        created_at TIMESTAMP DEFAULT NOW()
      );

      CREATE TABLE IF NOT EXISTS transactions (
        id SERIAL PRIMARY KEY,
        outlet_id INTEGER REFERENCES outlets(id) ON DELETE CASCADE,
        customer VARCHAR(255) NOT NULL,
        service VARCHAR(255) NOT NULL,
        amount DECIMAL(10,2) NOT NULL,
        status VARCHAR(50) DEFAULT 'diterima',
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
      );
    `);
    await client.query(`
      ALTER TABLE transactions
      ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

      UPDATE transactions
      SET updated_at = COALESCE(updated_at, created_at, NOW())
      WHERE updated_at IS NULL;
    `);
    console.log("[DB] Tabel berhasil dibuat/diverifikasi");
  } finally {
    client.release();
  }
}

export default pool;
