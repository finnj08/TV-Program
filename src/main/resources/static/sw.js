// Service Worker: macht die App schneller und zeigt zuletzt geladene Daten auch ohne Verbindung.
// Wichtig: Bei Änderungen an index.html o. ä. die Versionsnummer erhöhen, dann wird der Cache erneuert.
const VERSION = 'v1';
const SHELL_CACHE = 'tv-shell-' + VERSION;
const DATA_CACHE = 'tv-data-' + VERSION;
const SHELL_FILES = ['/', '/manifest.json', '/icons/icon-192.png', '/icons/icon-512.png'];

// Wartezeit, bis bei langsamer Verbindung auf zwischengespeicherte Daten zurückgegriffen wird.
// Auf dem kostenlosen Render-Server dauert das Aufwachen bis zu einer Minute.
const NETWORK_TIMEOUT_MS = 4000;

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(SHELL_CACHE).then(cache => cache.addAll(SHELL_FILES)).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys
        .filter(k => k !== SHELL_CACHE && k !== DATA_CACHE)
        .map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  if (url.pathname.startsWith('/api/')) {
    event.respondWith(networkFirst(req, url));
  } else {
    event.respondWith(staleWhileRevalidate(req));
  }
});

// Oberfläche (HTML, Icons): sofort aus dem Cache, im Hintergrund aktualisieren.
async function staleWhileRevalidate(req) {
  const cache = await caches.open(SHELL_CACHE);
  const cached = await cache.match(req);
  const update = fetch(req)
    .then(res => { if (res.ok) cache.put(req, res.clone()); return res; })
    .catch(() => null);
  if (cached) return cached;
  return (await update) || new Response('Offline', { status: 503, statusText: 'Offline' });
}

// Daten (API): erst das Netz, bei Fehler oder Trödeln der letzte gespeicherte Stand.
async function networkFirst(req, url) {
  const cache = await caches.open(DATA_CACHE);
  const cached = await cache.match(req);

  const network = fetch(req).then(async res => {
    if (res.ok && await worthCaching(res.clone(), url)) {
      cache.put(req, res.clone());
    }
    // Fehlerantwort (z. B. 502 beim Aufwachen): lieber den alten Stand zeigen, falls vorhanden
    if (!res.ok && cached) return cached;
    return res;
  });

  if (!cached) return network;
  const timeout = new Promise(resolve => setTimeout(() => resolve(cached), NETWORK_TIMEOUT_MS));
  return Promise.race([network.catch(() => cached), timeout]);
}

// Eine leere Senderliste nicht speichern: Sie entsteht, solange der Server noch Daten lädt,
// und würde sonst beim nächsten Start "Keine Sender" aus dem Cache liefern.
async function worthCaching(res, url) {
  if (url.pathname !== '/api/channels') return true;
  try {
    const data = await res.json();
    return Array.isArray(data) && data.length > 0;
  } catch (e) {
    return false;
  }
}
