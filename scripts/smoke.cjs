// Optional real-HTTP verification. Requires Node 18+, Java and the packaged JAR.
// Uses isolated target/ storage, starts its own Java process, and stops it on exit.
const { spawn, spawnSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const directory = fs.mkdtempSync(path.resolve('target', 'http-smoke-'));
const port = 20000 + Math.floor(Math.random() * 20000);
const origin = 'http://127.0.0.1:' + port;
// Resolve the actual JVM, bypassing Windows javapath launchers that spawn another process.
const settings = spawnSync('java', ['-XshowSettings:properties', '-version'], { encoding: 'utf8', windowsHide: true });
const javaHome = (settings.stderr || '').match(/java\.home\s*=\s*([^\r\n]+)/)?.[1].trim();
if (!javaHome) throw Error('Unable to locate the Java installation. Check java -version.');
const java = path.join(javaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
const idFromLocation = location => new URL(location, origin).pathname.split('/').at(-1).split(';')[0];
let server;
let output = '';
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
async function start() {
  output = '';
  server = spawn(java, ['-jar', 'target/reservation-1.0.0.jar', '--server.port=' + port,
    '--hotel.data-dir=' + directory, '--debug=false', '--logging.level.root=WARN'], { windowsHide: true });
  server.stdout.on('data', data => { output += data; });
  server.stderr.on('data', data => { output += data; });
  for (let attempt = 0; attempt < 120; attempt++) {
    if (server.exitCode !== null) throw Error('Server exited: ' + output);
    try { if ((await fetch(origin)).ok) return; } catch {}
    await delay(500);
  }
  throw Error('Server did not become ready: ' + output);
}
async function stop() {
  if (!server || server.exitCode !== null) return;
  const closed = new Promise(resolve => server.once('exit', resolve));
  server.kill();
  await closed;
}
async function post(route, values, expected = 302) {
  const response = await fetch(origin + route, { method: 'POST', body: new URLSearchParams(values), redirect: 'manual' });
  assert.equal(response.status, expected, route + ': ' + (expected === response.status ? '' : await response.text()));
  return response.headers.get('location');
}
async function run() {
  await start();
  for (const route of ['/', '/rooms', '/book', '/customers/register', '/admin', '/admin/rooms', '/admin/customers', '/reservations', '/css/style.css', '/js/app.js']) {
    assert.equal((await fetch(origin + route)).status, 200, route);
  }
  const roomData = { category: 'DELUXE', number: 'T901', capacity: '3', basePrice: '100.00', description: 'HTTP verification room' };
  await post('/admin/rooms', roomData);
  const records = fs.readFileSync(path.join(directory, 'rooms.txt'), 'utf8').trim().split(/\r?\n/).map(JSON.parse);
  const room = records.find(record => record.number === 'T901');
  assert.ok(room);
  await post('/admin/rooms', { ...roomData, id: room.id, description: 'Updated HTTP room' });
  const profile = await post('/customers', { name: 'HTTP Guest', email: 'http@example.com', phone: '0771234567' });
  const customerId = idFromLocation(profile);
  await post('/customers', { id: customerId, name: 'Updated Guest', email: 'http@example.com', phone: '0771234567' });
  const values = { roomId: room.id, customerId, checkIn: '2099-01-10', checkOut: '2099-01-12', guests: '2' };
  const confirmation = await post('/book', values);
  const reservationId = idFromLocation(confirmation);
  assert.match(await (await fetch(new URL(confirmation, origin))).text(), /240\.00/);
  await post('/book', values, 400);
  await post('/admin/rooms/' + room.id + '/delete', {}, 400);
  await post('/admin/customers/' + customerId + '/delete', {}, 400);
  await post('/book', { ...values, id: reservationId, checkOut: '2099-01-13' });
  await stop();
  await start();
  const restored = await (await fetch(new URL(confirmation, origin))).text();
  assert.match(restored, /360\.00/);
  assert.match(restored, /Updated Guest/);
  assert.match(await (await fetch(origin + '/book?id=' + reservationId)).text(), /value="2099-01-10"/);
  await post('/book', values, 400);
  await post('/reservations/' + reservationId + '/cancel', {});
  const replacement = await post('/book', values);
  await post('/reservations/' + reservationId + '/delete', {});
  const replacementId = idFromLocation(replacement);
  await post('/reservations/' + replacementId + '/cancel', {});
  await post('/reservations/' + replacementId + '/delete', {});
  await post('/admin/rooms/' + room.id + '/delete', {});
  await post('/admin/customers/' + customerId + '/delete', {});
  console.log('PASS: real HTTP pages, room/customer/reservation CRUD, double-booking rejection, price calculation, cancellation and full process restart persistence.');
}
run().catch(error => { console.error(error); process.exitCode = 1; }).finally(stop);
