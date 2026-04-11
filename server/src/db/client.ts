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
      ALTER TABLE users
      ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

      ALTER TABLE outlets
      ADD COLUMN IF NOT EXISTS owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
      ADD COLUMN IF NOT EXISTS activation_code VARCHAR(50),
      ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

      ALTER TABLE devices
      ADD COLUMN IF NOT EXISTS owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
      ADD COLUMN IF NOT EXISTS outlet_id INTEGER REFERENCES outlets(id) ON DELETE CASCADE,
      ADD COLUMN IF NOT EXISTS device_name VARCHAR(255),
      ADD COLUMN IF NOT EXISTS device_id VARCHAR(255),
      ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'offline',
      ADD COLUMN IF NOT EXISTS last_seen TIMESTAMP,
      ADD COLUMN IF NOT EXISTS refresh_token TEXT,
      ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

      ALTER TABLE transactions
      ADD COLUMN IF NOT EXISTS outlet_id INTEGER REFERENCES outlets(id) ON DELETE CASCADE,
      ADD COLUMN IF NOT EXISTS customer VARCHAR(255),
      ADD COLUMN IF NOT EXISTS service VARCHAR(255),
      ADD COLUMN IF NOT EXISTS amount DECIMAL(10,2),
      ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'diterima',
      ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
      ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

      UPDATE users
      SET created_at = NOW()
      WHERE created_at IS NULL;

      UPDATE outlets
      SET created_at = NOW()
      WHERE created_at IS NULL;

      UPDATE devices
      SET status = COALESCE(status, 'offline'),
          created_at = COALESCE(created_at, NOW())
      WHERE status IS NULL OR created_at IS NULL;

      UPDATE transactions
      SET created_at = COALESCE(created_at, NOW()),
          updated_at = COALESCE(updated_at, created_at, NOW()),
          status = COALESCE(status, 'diterima')
      WHERE created_at IS NULL OR updated_at IS NULL OR status IS NULL;

      CREATE UNIQUE INDEX IF NOT EXISTS outlets_activation_code_unique_idx
      ON outlets (activation_code)
      WHERE activation_code IS NOT NULL;

      CREATE UNIQUE INDEX IF NOT EXISTS devices_device_id_unique_idx
      ON devices (device_id)
      WHERE device_id IS NOT NULL;
    `);
    console.log("[DB] Tabel berhasil dibuat/diverifikasi");
  } finally {
    client.release();
  }
}

export default pool;
