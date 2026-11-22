const base = new URL("./", self.location);
const version = "vanguardnav-shell-f4208ec738d1";
const entry = new URL("index.html", base).href;
self.addEventListener("install", event => event.waitUntil((async () => {
  const cache = await caches.open(version);
  await cache.addAll(["index.html","manifest.webmanifest","icon.svg"].map(path =>
    new Request(new URL(path,base), {cache:"reload"})));
  await self.skipWaiting();
})()));
self.addEventListener("activate", event => event.waitUntil((async () => {
  await Promise.all((await caches.keys()).filter(k => k.startsWith("vanguardnav-shell-") && k !== version).map(k=>caches.delete(k)));
  await self.clients.claim();
})()));
self.addEventListener("fetch", event => {
  const u = new URL(event.request.url);
  if(event.request.method !== "GET" || u.origin !== base.origin) return;
  const address = u.origin + u.pathname;
  const key = address === base.href ? entry : address;
  if (![entry, new URL("manifest.webmanifest",base).href,new URL("icon.svg",base).href].includes(key)) return;
  event.respondWith((async () => (await (await caches.open(version)).match(key)) || fetch(event.request))());
});
