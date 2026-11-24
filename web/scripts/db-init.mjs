// =====================================================================
// GharBaar - DATABASE INITIALISER + SEED DATA
// Usage:  npm run db:init        (create tables, seed if empty)
//         npm run db:reset       (DROP everything, recreate, reseed)
// Needs DATABASE_URL and ADMIN_PASSWORD in web/.env.local
// =====================================================================
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import pg from 'pg';
import bcrypt from 'bcryptjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const schemaPath = path.resolve(here, '../../database/schema.sql');
const reset = process.argv.includes('--reset');

if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL missing');
if (!process.env.ADMIN_PASSWORD) throw new Error('ADMIN_PASSWORD missing');

// direct connection so the Asia/Karachi time zone applies (pooled ones use GMT)
const client = new pg.Client({ connectionString: process.env.DATABASE_URL_UNPOOLED || process.env.DATABASE_URL });
await client.connect();

// ---------------------------------------------------------------------
// [SECTION] Store all times as Pakistan local time (desktop + web agree)
// ---------------------------------------------------------------------
const dbName = (await client.query('SELECT current_database() AS d')).rows[0].d;
await client.query(`ALTER DATABASE "${dbName}" SET timezone TO 'Asia/Karachi'`);
await client.query(`SET timezone TO 'Asia/Karachi'`);

// ---------------------------------------------------------------------
// [SECTION] Reset (drop all tables)
// ---------------------------------------------------------------------
if (reset) {
  console.log('Dropping all tables...');
  await client.query(`DROP TABLE IF EXISTS notifications, reports, facility_confirmations, facility_reviews,
    tenancy_claims, visit_requests, listing_deletions, facilities, listing_photos, property_listings,
    stored_files, users CASCADE`);
}

// ---------------------------------------------------------------------
// [SECTION] Create schema
// ---------------------------------------------------------------------
await client.query(fs.readFileSync(schemaPath, 'utf8'));
console.log('Schema ready.');

const { rows: [{ count }] } = await client.query('SELECT COUNT(*)::int AS count FROM users');
if (count > 0) {
  console.log(`Users already exist (${count}); skipping seed.`);
  await client.end();
  process.exit(0);
}

// ---------------------------------------------------------------------
// [SECTION] Seed users (demo accounts - test values only)
//   Demo customer/agent password: Demo@1234
//   Admin password comes from ADMIN_PASSWORD env variable
// ---------------------------------------------------------------------
const demoHash = bcrypt.hashSync('Demo@1234', 10);
const adminHash = bcrypt.hashSync(process.env.ADMIN_PASSWORD, 10);

async function addUser(username, hash, name, phone, role, extra = {}) {
  const r = await client.query(
    `INSERT INTO users (username, password_hash, name, phone, role, cnic, tenant_status)
     VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING user_id`,
    [username, hash, name, phone, role, extra.cnic ?? null, extra.tenantStatus ?? null]);
  return r.rows[0].user_id;
}

const admin  = await addUser('admin', adminHash, 'GharBaar Admin', '03000000000', 'ADMIN');
const agent1 = await addUser('agent_mahmud', demoHash, 'Mahmud Khan', '03001234567', 'AGENT');
const agent2 = await addUser('agent_shelem', demoHash, 'Shelem Khan', '03111234567', 'AGENT');
const cust1  = await addUser('customer_sara', demoHash, 'Sara Ahmed', '03211234567', 'CUSTOMER');
const tenant = await addUser('tenant_bilal', demoHash, 'Bilal Raza', '03331234567', 'CUSTOMER',
  { cnic: '61101-1234567-1', tenantStatus: 'VERIFIED' });
const tenant2 = await addUser('tenant_hina', demoHash, 'Hina Malik', '03451234567', 'CUSTOMER',
  { cnic: '61101-7654321-2', tenantStatus: 'PENDING' });

// ---------------------------------------------------------------------
// [SECTION] Seed property listings
// ---------------------------------------------------------------------
const U = (id) => `https://images.unsplash.com/photo-${id}?w=1200&q=80`;
const listings = [
  { agent: agent1, title: 'Single sunny room apartment', address: 'Street 12, F-7/2', type: 'Apartment', sector: 'F-7', rent: 45000, furn: true, occ: 'Individual',
    desc: 'Bright single-bedroom apartment with large windows, close to Jinnah Super market. Ideal for working professionals.',
    photos: ['1505691938895-1758d7feb511','1522708323590-d24dbb6b0267','1484154218962-a197022b5858','1584622650111-993a426fbf0a'],
    fac: ['Wi-Fi','TV','Furniture','Elevator','Air Conditioning','Generator'] },
  { agent: agent2, title: 'Modern family portion with lawn', address: 'House 44, Street 9, F-10/3', type: 'Portion', sector: 'F-10', rent: 120000, furn: false, occ: 'Family',
    desc: 'Upper portion with 3 bedrooms, separate gate, lawn access and covered parking. Quiet family street.',
    photos: ['1568605114967-8130f3a36994','1600607687939-ce8a6c25118c','1556909114-f6e7ad7d3136'],
    fac: ['Parking','Gas','Wi-Fi','Security','Generator'] },
  { agent: agent1, title: 'Cozy furnished flat near Centaurus', address: 'Block B, Silver Oaks, F-10 Markaz', type: 'Flat', sector: 'F-10', rent: 85000, furn: true, occ: 'Family',
    desc: 'Two bedroom furnished flat, 5 minutes from F-10 Markaz with 24/7 security and backup power.',
    photos: ['1502672260266-1c1ef2d93688','1493809842364-78817add7ffb','1540518614846-7eded433c457'],
    fac: ['Wi-Fi','TV','Furniture','Elevator','Air Conditioning','Security','Parking'] },
  { agent: agent2, title: 'Girls hostel room - shared', address: 'Plot 7, G-11 Markaz', type: 'Hostel Room', sector: 'G-11', rent: 18000, furn: true, occ: 'Shared',
    desc: 'Shared room for female students and professionals. Meals, laundry and Wi-Fi included.',
    photos: ['1555854877-bab0e564b8d5','1631049307264-da0ec9d70304'],
    fac: ['Wi-Fi','Furniture','Meals','Laundry','Security'] },
  { agent: agent1, title: 'Luxury apartment with city view', address: 'Tower 2, The Centaurus, F-8', type: 'Apartment', sector: 'F-8', rent: 250000, furn: true, occ: 'Family',
    desc: 'Three bedroom luxury apartment on the 14th floor with Margalla hills view, gym and pool.',
    photos: ['1600585154340-be6161a56a0c','1616594039964-ae9021a400a0','1617806118233-18e1de247200','1615874959474-d609969a20ed'],
    fac: ['Wi-Fi','TV','Furniture','Elevator','Air Conditioning','Generator','Parking','Gym','Security'] },
  { agent: agent2, title: 'Ground portion for small family', address: 'House 210, Street 31, G-13/1', type: 'Portion', sector: 'G-13', rent: 60000, furn: false, occ: 'Family',
    desc: 'Ground floor portion with 2 bedrooms, drawing room and car porch. Near park and mosque.',
    photos: ['1570129477492-45c003edd2be','1586023492125-27b2c045efd7'],
    fac: ['Gas','Parking','Wi-Fi'] },
  { agent: agent1, title: 'Studio flat for bachelors', address: 'Office Tower, I-8 Markaz', type: 'Flat', sector: 'I-8', rent: 38000, furn: true, occ: 'Individual',
    desc: 'Compact studio flat with attached kitchen, best for students and young professionals.',
    photos: ['1554995207-c18c203602cb','1513694203232-719a280e022f'],
    fac: ['Wi-Fi','Furniture','Air Conditioning','Elevator'] },
  { agent: agent2, title: 'Spacious upper portion E-11', address: 'House 5, Street 2, E-11/3', type: 'Portion', sector: 'E-11', rent: 95000, furn: false, occ: 'Family',
    desc: 'Newly built 3 bed upper portion with terrace, separate entrance and solar backup.',
    photos: ['1600596542815-ffad4c1539a9','1598928506311-c55ded91a20c'],
    fac: ['Gas','Parking','Generator','Security'] },
  // a pending listing so the Admin has something to verify
  { agent: agent1, title: 'Two bed flat in G-9 (new)', address: 'Karachi Company, G-9 Markaz', type: 'Flat', sector: 'G-9', rent: 55000, furn: false, occ: 'Family',
    desc: 'Two bedroom flat above the market, walking distance from metro bus.', status: 'PENDING_VERIFICATION',
    photos: ['1560448204-e02f11c3d0e2','1512917774080-9991f1c4c750'],
    fac: ['Gas','Wi-Fi','Elevator'] },
];

const listingIds = [];
for (const l of listings) {
  const r = await client.query(
    `INSERT INTO property_listings (agent_id,title,address,property_type,cda_sector,rent,description,furnishing,occupancy_type,photo_path,listing_status)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11) RETURNING listing_id`,
    [l.agent, l.title, l.address, l.type, l.sector, l.rent, l.desc, l.furn, l.occ, U(l.photos[0]), l.status ?? 'VERIFIED']);
  const id = r.rows[0].listing_id;
  listingIds.push(id);
  for (let i = 0; i < l.photos.length; i++)
    await client.query('INSERT INTO listing_photos (listing_id, photo_path, sort_order) VALUES ($1,$2,$3)', [id, U(l.photos[i]), i]);
  for (const f of l.fac)
    await client.query('INSERT INTO facilities (listing_id, name) VALUES ($1,$2)', [id, f]);
}

// ---------------------------------------------------------------------
// [SECTION] Seed tenancy history (verified claim + facility review)
// ---------------------------------------------------------------------
const proofPng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==', 'base64');
const pf = await client.query(
  `INSERT INTO stored_files (file_name, content_type, data, admin_only, uploaded_by) VALUES ('rent_agreement.png','image/png',$1,TRUE,$2) RETURNING file_id`,
  [proofPng, tenant]);
const proof = `db:${pf.rows[0].file_id}`;

const claim = await client.query(
  `INSERT INTO tenancy_claims (tenant_id, listing_id, start_date, end_date, proof_document, status)
   VALUES ($1,$2,'2025-01-01','2025-12-31',$3,'VERIFIED') RETURNING claim_id`, [tenant, listingIds[0], proof]);
const review = await client.query(
  `INSERT INTO facility_reviews (claim_id, comments, submitted_date) VALUES ($1,$2,'2026-01-05') RETURNING review_id`,
  [claim.rows[0].claim_id, 'Great place overall. Wi-Fi was fast, but the generator only covered lights and fans, not the AC.']);
const facs = await client.query('SELECT facility_id, name FROM facilities WHERE listing_id=$1', [listingIds[0]]);
for (const f of facs.rows)
  await client.query('INSERT INTO facility_confirmations (review_id, facility_id, confirmed) VALUES ($1,$2,$3)',
    [review.rows[0].review_id, f.facility_id, f.name !== 'Generator']);

// a pending claim so the Admin can demo "Verify Tenant"
await client.query(
  `INSERT INTO tenancy_claims (tenant_id, listing_id, start_date, end_date, proof_document, status)
   VALUES ($1,$2,'2025-06-01','2026-05-31',$3,'PENDING')`, [tenant2, listingIds[2], proof]);

// ---------------------------------------------------------------------
// [SECTION] Seed a visit request, an open report, notifications
// ---------------------------------------------------------------------
await client.query(`INSERT INTO visit_requests (customer_id, listing_id, proposed_datetime, notes)
  VALUES ($1,$2, NOW() + INTERVAL '3 days', 'Can I visit in the evening after 6pm?')`, [cust1, listingIds[0]]);
await client.query(`INSERT INTO reports (reporter_id, type, description)
  VALUES ($1,'Fake Listing','The photos of a listing in G-13 do not match the actual house.')`, [cust1]);
await client.query(`INSERT INTO notifications (recipient_id, message) VALUES ($1,'Welcome to GharBaar!'),($2,'Welcome to GharBaar!')`, [cust1, agent1]);

console.log('Seed complete. Admin id', admin);
await client.end();
