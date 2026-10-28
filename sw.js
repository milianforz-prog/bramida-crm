// Bramida CRM service worker: app shell offline, data always from the network.
const V = 'bramida-v6';
const ASSETS = ['./', 'index.html', 'manifest.webmanifest', 'vendor/supabase.js?v=6', 'fonts/fonts.css?v=6', 'icons/icon-192.png', 'icons/favicon-32.png', 'icons/apple-touch-icon.png', 'fonts/jetbrains-mono-cyrillic-500-normal.woff2', 'fonts/jetbrains-mono-cyrillic-600-normal.woff2', 'fonts/jetbrains-mono-latin-500-normal.woff2', 'fonts/jetbrains-mono-latin-600-normal.woff2', 'fonts/jetbrains-mono-latin-ext-500-normal.woff2', 'fonts/jetbrains-mono-latin-ext-600-normal.woff2', 'fonts/onest-cyrillic-400-normal.woff2', 'fonts/onest-cyrillic-500-normal.woff2', 'fonts/onest-cyrillic-600-normal.woff2', 'fonts/onest-cyrillic-700-normal.woff2', 'fonts/onest-latin-400-normal.woff2', 'fonts/onest-latin-500-normal.woff2', 'fonts/onest-latin-600-normal.woff2', 'fonts/onest-latin-700-normal.woff2', 'fonts/onest-latin-ext-400-normal.woff2', 'fonts/onest-latin-ext-500-normal.woff2', 'fonts/onest-latin-ext-600-normal.woff2', 'fonts/onest-latin-ext-700-normal.woff2', 'fonts/pt-serif-cyrillic-400-normal.woff2', 'fonts/pt-serif-cyrillic-700-normal.woff2', 'fonts/pt-serif-latin-400-normal.woff2', 'fonts/pt-serif-latin-700-normal.woff2', 'fonts/pt-serif-latin-ext-400-normal.woff2', 'fonts/pt-serif-latin-ext-700-normal.woff2', 'fonts/unbounded-cyrillic-500-normal.woff2', 'fonts/unbounded-cyrillic-600-normal.woff2', 'fonts/unbounded-cyrillic-700-normal.woff2', 'fonts/unbounded-latin-500-normal.woff2', 'fonts/unbounded-latin-600-normal.woff2', 'fonts/unbounded-latin-700-normal.woff2', 'fonts/unbounded-latin-ext-500-normal.woff2', 'fonts/unbounded-latin-ext-600-normal.woff2', 'fonts/unbounded-latin-ext-700-normal.woff2'];
self.addEventListener('install', e => { e.waitUntil(caches.open(V).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting())); });
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== V).map(k => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', e => {
  const req = e.request; const u = new URL(req.url);
  if (req.method !== 'GET' || u.origin !== location.origin) return;
  if (req.mode === 'navigate' || u.pathname.endsWith('/') || u.pathname.endsWith('/index.html')) {
    e.respondWith(fetch(req, { cache: 'no-store' }).then(r => { const c = r.clone(); caches.open(V).then(ca => ca.put('index.html', c)); return r; }).catch(() => caches.match('index.html')));
    return;
  }
  e.respondWith(caches.match(req).then(hit => hit || fetch(req).then(r => { if (r.ok) { const c = r.clone(); caches.open(V).then(ca => ca.put(req, c)); } return r; })));
});
