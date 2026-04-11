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
    const migrations = [
      "ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW()",
      "ALTER TABLE outlets ADD COLUMN IF NOT EXISTS owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE",
      "ALTER TABLE outlets ADD COLUMN IF NOT EXISTS activation_code VARCHAR(50)",
      "ALTER TABLE outlets ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW()",
      "ALTER TABLE devices ADD COLUMN IF NOT EXISTS owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE",
      "ALTER TABLE devices ADD COLUMN IF NOT EXISTS outlet_id INTEGER REFERENCES outlets(id) ON DELETE CASCADE",
      "ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_name VARCHAR(255)",
      "ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_id VARCHAR(255)",
      "ALTER TABLE devices ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'offline'",
      "ALTER TABLE devices ADD COLUMN IF NOT EXISTS last_seen TIMESTAMP",
      "ALTER TABLE devices ADD COLUMN IF NOT EXISTS refresh_token TEXT",
      "ALTER TABLE devices ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW()",
      "ALTER TABLE transactions ADD COLUMN IF NOT EXISTS outlet_id INTEGER REFERENCES outlets(id) ON DELETE CASCADE",
      "ALTER TABLE transactions ADD COLUMN IF NOT EXISTS customer VARCHAR(255)",
      "ALTER TABLE transactions ADD COLUMN IF NOT EXISTS service VARCHAR(255)",
      "ALTER TABLE transactions ADD COLUMN IF NOT EXISTS amount DECIMAL(10,2)",
      "ALTER TABLE transactions ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'diterima'",
      "ALTER TABLE transactions ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW()",
      "ALTER TABLE transactions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW()",
      "UPDATE users SET created_at = NOW() WHERE created_at IS NULL",
      "UPDATE outlets SET created_at = NOW() WHERE created_at IS NULL",
      "UPDATE devices SET status = COALESCE(status, 'offline'), created_at = COALESCE(created_at, NOW()) WHERE status IS NULL OR created_at IS NULL",
      "UPDATE transactions SET created_at = COALESCE(created_at, NOW()), updated_at = COALESCE(updated_at, created_at, NOW()), status = COALESCE(status, 'diterima') WHERE created_at IS NULL OR updated_at IS NULL OR status IS NULL",
    ];

    for (const migration of migrations) {
      try {
        await client.query(migration);
      } catch (err) {
        console.warn("[DB] Migrasi dilewati:", migration, err);
      }
    }
    console.log("[DB] Tabel berhasil dibuat/diverifikasi");
  } finally {
    client.release();
  }
}

export default pool;
