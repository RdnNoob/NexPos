import { Pool } from "pg";

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl:
    process.env.NODE_ENV === "production"
      ? { rejectUnauthorized: false }
      : false,
});

const migrations = [
  // Hapus NOT NULL di owner_id agar tidak crash jika tipe UUID (Railway legacy schema)
  "ALTER TABLE devices ALTER COLUMN owner_id DROP NOT NULL",
  "ALTER TABLE outlets ALTER COLUMN owner_id DROP NOT NULL",
  "ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW()",
  "ALTER TABLE outlets ADD COLUMN IF NOT EXISTS owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE",
  `DO $$
   DECLARE constraint_name text;
   BEGIN
     FOR constraint_name IN
       SELECT kcu.constraint_name
       FROM information_schema.key_column_usage kcu
       WHERE kcu.table_name = 'outlets'
         AND kcu.column_name = 'owner_id'
         AND kcu.table_schema = current_schema()
     LOOP
       EXECUTE format('ALTER TABLE outlets DROP CONSTRAINT IF EXISTS %I', constraint_name);
     END LOOP;
   END $$`,
  "ALTER TABLE outlets ALTER COLUMN owner_id TYPE VARCHAR(255) USING owner_id::text",
  "ALTER TABLE outlets ADD COLUMN IF NOT EXISTS activation_code VARCHAR(50)",
  "ALTER TABLE outlets ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW()",
  "ALTER TABLE devices ADD COLUMN IF NOT EXISTS owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE",
  `DO $$
   DECLARE constraint_name text;
   BEGIN
     FOR constraint_name IN
       SELECT kcu.constraint_name
       FROM information_schema.key_column_usage kcu
       WHERE kcu.table_name = 'devices'
         AND kcu.column_name = 'owner_id'
         AND kcu.table_schema = current_schema()
     LOOP
       EXECUTE format('ALTER TABLE devices DROP CONSTRAINT IF EXISTS %I', constraint_name);
     END LOOP;
   END $$`,
  "ALTER TABLE devices ALTER COLUMN owner_id TYPE VARCHAR(255) USING owner_id::text",
  `DO $$
   DECLARE outlet_id_type text;
   DECLARE constraint_name text;
   BEGIN
     SELECT data_type INTO outlet_id_type
     FROM information_schema.columns
     WHERE table_name = 'devices'
       AND column_name = 'outlet_id'
       AND table_schema = current_schema();

     IF outlet_id_type IS NOT NULL AND outlet_id_type <> 'integer' THEN
       FOR constraint_name IN
         SELECT tc.constraint_name
         FROM information_schema.table_constraints tc
         JOIN information_schema.key_column_usage kcu
           ON tc.constraint_name = kcu.constraint_name
          AND tc.table_schema = kcu.table_schema
         WHERE tc.table_name = 'devices'
           AND kcu.column_name = 'outlet_id'
           AND tc.table_schema = current_schema()
       LOOP
         EXECUTE format('ALTER TABLE devices DROP CONSTRAINT IF EXISTS %I', constraint_name);
       END LOOP;

       ALTER TABLE devices DROP COLUMN outlet_id;
       ALTER TABLE devices ADD COLUMN outlet_id INTEGER;
       RAISE NOTICE 'devices.outlet_id legacy type % diganti menjadi INTEGER', outlet_id_type;
     END IF;
   END $$`,
  "ALTER TABLE devices ADD COLUMN IF NOT EXISTS outlet_id INTEGER REFERENCES outlets(id) ON DELETE CASCADE",
  "ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_name VARCHAR(255)",
  "ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_id VARCHAR(255)",
  "ALTER TABLE devices ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'offline'",
  "ALTER TABLE devices ADD COLUMN IF NOT EXISTS last_seen TIMESTAMP",
  "ALTER TABLE devices ADD COLUMN IF NOT EXISTS refresh_token TEXT",
  "ALTER TABLE devices ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW()",
  `DO $$
   DECLARE outlet_id_type text;
   DECLARE constraint_name text;
   BEGIN
     SELECT data_type INTO outlet_id_type
     FROM information_schema.columns
     WHERE table_name = 'transactions'
       AND column_name = 'outlet_id'
       AND table_schema = current_schema();

     IF outlet_id_type IS NOT NULL AND outlet_id_type <> 'integer' THEN
       FOR constraint_name IN
         SELECT tc.constraint_name
         FROM information_schema.table_constraints tc
         JOIN information_schema.key_column_usage kcu
           ON tc.constraint_name = kcu.constraint_name
          AND tc.table_schema = kcu.table_schema
         WHERE tc.table_name = 'transactions'
           AND kcu.column_name = 'outlet_id'
           AND tc.table_schema = current_schema()
       LOOP
         EXECUTE format('ALTER TABLE transactions DROP CONSTRAINT IF EXISTS %I', constraint_name);
       END LOOP;

       ALTER TABLE transactions DROP COLUMN outlet_id;
       ALTER TABLE transactions ADD COLUMN outlet_id INTEGER;
       RAISE NOTICE 'transactions.outlet_id legacy type % diganti menjadi INTEGER', outlet_id_type;
     END IF;
   END $$`,
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
  "ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_code VARCHAR(6)",
  "ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_expires_at TIMESTAMP",
  "ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token VARCHAR(128)",
  "ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_expires_at TIMESTAMP",
];

export async function ensureRuntimeSchema() {
  for (const migration of migrations) {
    try {
      await pool.query(migration);
    } catch (err) {
      console.warn("[DB] Migrasi dilewati:", migration, err);
    }
  }
}

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
        owner_id VARCHAR(255) NOT NULL,
        name VARCHAR(255) NOT NULL,
        activation_code VARCHAR(50) UNIQUE NOT NULL,
        created_at TIMESTAMP DEFAULT NOW()
      );

      CREATE TABLE IF NOT EXISTS devices (
        id SERIAL PRIMARY KEY,
        owner_id VARCHAR(255) NOT NULL,
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
    await ensureRuntimeSchema();
    console.log("[DB] Tabel berhasil dibuat/diverifikasi");
  } finally {
    client.release();
  }
}

export default pool;
