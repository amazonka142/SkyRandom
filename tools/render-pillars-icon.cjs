const fs = require("fs");
const path = require("path");
const sharp = require("sharp");

const root = path.resolve(__dirname, "..");
const svgPath = path.join(root, "assets", "skyrandom-pillars-icon.svg");
const pngPath = path.join(root, "assets", "skyrandom-pillars-icon.png");

const W = 1024;
const H = 1024;
const ORIGIN_X = 512;
const ORIGIN_Y = 586;
const TILE_W = 34;
const TILE_H = 18;
const BLOCK_H = 16;

const blocks = [];
const players = [];
const bridge = [];

function iso(x, y, z) {
  return {
    x: ORIGIN_X + (x - z) * TILE_W / 2,
    y: ORIGIN_Y + (x + z) * TILE_H / 2 - y * BLOCK_H,
  };
}

function hash(x, y, z) {
  let n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791);
  n = Math.imul(n ^ (n >>> 13), 1274126177);
  return Math.abs(n ^ (n >>> 16));
}

function shade(hex, amount) {
  const value = hex.replace("#", "");
  const r = Math.max(0, Math.min(255, parseInt(value.slice(0, 2), 16) + amount));
  const g = Math.max(0, Math.min(255, parseInt(value.slice(2, 4), 16) + amount));
  const b = Math.max(0, Math.min(255, parseInt(value.slice(4, 6), 16) + amount));
  return `rgb(${r},${g},${b})`;
}

function addBlock(x, y, z, material = "bedrock") {
  blocks.push({ x, y, z, material });
}

function addPillar(cx, cz) {
  for (let y = 0; y < 11; y += 1) {
    addBlock(cx, y, cz, "bedrock");
  }
  for (let dx = -1; dx <= 1; dx += 1) {
    for (let dz = -1; dz <= 1; dz += 1) {
      addBlock(cx + dx, 11, cz + dz, "bedrock");
    }
  }
}

function addBridge(fromX, fromZ, toX, toZ) {
  const steps = 6;
  for (let i = 0; i < steps; i += 1) {
    const t = (i + 1) / (steps + 1);
    bridge.push({
      x: Math.round(fromX + (toX - fromX) * t),
      z: Math.round(fromZ + (toZ - fromZ) * t),
      y: 11,
      material: "stone",
    });
  }
}

function polygon(points, attrs = "") {
  return `<polygon points="${points.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(" ")}" ${attrs}/>`;
}

function line(x1, y1, x2, y2, attrs = "") {
  return `<line x1="${x1.toFixed(1)}" y1="${y1.toFixed(1)}" x2="${x2.toFixed(1)}" y2="${y2.toFixed(1)}" ${attrs}/>`;
}

function drawCube({ x, y, z, material }) {
  const base = material === "stone" ? "#8c9296" : "#3b3d42";
  const jitter = (hash(x, y, z) % 17) - 8;
  const top = shade(base, 28 + jitter);
  const right = shade(base, -10 + jitter);
  const left = shade(base, -28 + jitter);
  const stroke = material === "stone" ? "#4f5559" : "#16181c";
  const h = y + 1;
  const a = iso(x, h, z);
  const b = iso(x + 1, h, z);
  const c = iso(x + 1, h, z + 1);
  const d = iso(x, h, z + 1);
  const e = iso(x + 1, y, z);
  const f = iso(x + 1, y, z + 1);
  const g = iso(x, y, z + 1);
  const speckA = iso(x + 0.28, h + 0.002, z + 0.28);
  const speckB = iso(x + 0.68, h + 0.002, z + 0.58);
  const speck = material === "bedrock"
    ? `<circle cx="${speckA.x.toFixed(1)}" cy="${speckA.y.toFixed(1)}" r="1.7" fill="#777b82" opacity=".55"/>
       <circle cx="${speckB.x.toFixed(1)}" cy="${speckB.y.toFixed(1)}" r="1.3" fill="#101216" opacity=".7"/>`
    : "";
  return `
    ${polygon([d, c, f, g], `fill="${left}" stroke="${stroke}" stroke-width="1.1"`)}
    ${polygon([b, c, f, e], `fill="${right}" stroke="${stroke}" stroke-width="1.1"`)}
    ${polygon([a, b, c, d], `fill="${top}" stroke="${stroke}" stroke-width="1.2"`)}
    ${speck}
  `;
}

function drawPlayer(x, z, color, pose = "idle", flip = false) {
  const p = iso(x + 0.5, 12.25, z + 0.5);
  const sx = p.x;
  const sy = p.y;
  const dir = flip ? -1 : 1;
  const armSwing = pose === "fight" ? 19 : pose === "build" ? -12 : 4;
  const weapon = pose === "fight"
    ? `<rect x="${sx + dir * 15}" y="${sy - 45}" width="5" height="44" rx="1.5" transform="rotate(${dir * 43} ${sx + dir * 15} ${sy - 45})" fill="#d8dde4" stroke="#343941" stroke-width="2"/>`
    : pose === "build"
      ? `<polygon points="${sx + dir * 22},${sy - 37} ${sx + dir * 39},${sy - 47} ${sx + dir * 50},${sy - 31} ${sx + dir * 31},${sy - 22}" fill="#8f949a" stroke="#2f3339" stroke-width="2"/>`
      : "";
  return `
    <g filter="url(#avatarShadow)">
      <ellipse cx="${sx}" cy="${sy + 2}" rx="19" ry="8" fill="#000" opacity=".18"/>
      <rect x="${sx - 10}" y="${sy - 32}" width="20" height="28" rx="4" fill="${color}" stroke="#1d2530" stroke-width="2"/>
      <rect x="${sx - 11}" y="${sy - 53}" width="22" height="22" rx="4" fill="#d8a070" stroke="#1d2530" stroke-width="2"/>
      <rect x="${sx - 10}" y="${sy - 55}" width="21" height="8" rx="3" fill="#3a2419"/>
      <rect x="${sx + dir * 8}" y="${sy - 29}" width="9" height="26" rx="4" transform="rotate(${dir * armSwing} ${sx + dir * 8} ${sy - 29})" fill="${color}" stroke="#1d2530" stroke-width="2"/>
      <rect x="${sx - dir * 17}" y="${sy - 29}" width="8" height="22" rx="4" transform="rotate(${-dir * 12} ${sx - dir * 17} ${sy - 29})" fill="${color}" stroke="#1d2530" stroke-width="2"/>
      <rect x="${sx - 10}" y="${sy - 7}" width="8" height="21" rx="3" fill="#242b35" stroke="#1d2530" stroke-width="2"/>
      <rect x="${sx + 2}" y="${sy - 7}" width="8" height="21" rx="3" fill="#242b35" stroke="#1d2530" stroke-width="2"/>
      <rect x="${sx + 4}" y="${sy - 45}" width="3.5" height="4" fill="#143357"/>
      ${weapon}
    </g>
  `;
}

function drawDrop(kind, x, y, trail) {
  const trailSvg = `<path d="M${x - 8} ${y - 110} C${x + 22} ${y - 76}, ${x - 24} ${y - 45}, ${x} ${y - 12}" fill="none" stroke="${trail}" stroke-width="8" opacity=".45" stroke-linecap="round"/>`;
  if (kind === "tnt") {
    return `${trailSvg}<g transform="translate(${x} ${y}) rotate(-12)"><rect x="-18" y="-18" width="36" height="36" rx="3" fill="#d92929" stroke="#3a1515" stroke-width="4"/><rect x="-18" y="-4" width="36" height="9" fill="#fff" opacity=".9"/><text x="0" y="3" text-anchor="middle" font-size="11" font-family="Arial Black, Arial" fill="#1d1d1d">TNT</text></g>`;
  }
  if (kind === "potion") {
    return `${trailSvg}<g transform="translate(${x} ${y}) rotate(16)"><rect x="-8" y="-25" width="16" height="12" rx="3" fill="#f6d6ff" stroke="#472257" stroke-width="3"/><path d="M-15,-11 L15,-11 L10,18 L-10,18 Z" fill="#dc3cff" stroke="#472257" stroke-width="3"/><circle cx="-4" cy="1" r="4" fill="#fff" opacity=".55"/></g>`;
  }
  if (kind === "sword") {
    return `${trailSvg}<g transform="translate(${x} ${y}) rotate(44)"><rect x="-4" y="-36" width="8" height="54" fill="#dce5ef" stroke="#263240" stroke-width="3"/><rect x="-18" y="9" width="36" height="8" fill="#6c4526" stroke="#263240" stroke-width="3"/><rect x="-5" y="16" width="10" height="22" fill="#3b2514" stroke="#263240" stroke-width="3"/></g>`;
  }
  if (kind === "swirl") {
    return `${trailSvg}<g transform="translate(${x} ${y})"><path d="M-28 4 C-20 -20, 17 -25, 24 -1 C29 18, 6 30, -10 18 C-25 7, -10 -8, 7 -4" fill="none" stroke="#8df6ff" stroke-width="8" stroke-linecap="round"/><path d="M-18 8 C-10 -4, 9 -9, 14 2" fill="none" stroke="#f4ffff" stroke-width="4" stroke-linecap="round"/></g>`;
  }
  return `${trailSvg}<g transform="translate(${x} ${y}) rotate(12)"><polygon points="0,-27 25,-14 0,0 -25,-14" fill="#aaaeb4" stroke="#383c42" stroke-width="3"/><polygon points="-25,-14 0,0 0,29 -25,13" fill="#686e76" stroke="#383c42" stroke-width="3"/><polygon points="25,-14 0,0 0,29 25,13" fill="#555b63" stroke="#383c42" stroke-width="3"/></g>`;
}

for (const [cx, cz] of [[-8, -8], [8, -8], [8, 8], [-8, 8]]) {
  addPillar(cx, cz);
}

addBridge(-7, -8, 7, -8);

players.push(drawPlayer(-8, -9, "#1e88e5", "build", false));
players.push(drawPlayer(8, -9, "#e53935", "idle", true));
players.push(drawPlayer(7.1, 8, "#43a047", "fight", false));
players.push(drawPlayer(8.2, 7.8, "#f9a825", "fight", true));

const sortedBlocks = [...blocks, ...bridge].sort((a, b) => {
  const da = a.x + a.z + a.y * 0.02;
  const db = b.x + b.z + b.y * 0.02;
  return da - db;
});

const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">
  <defs>
    <radialGradient id="sun" cx="16%" cy="24%" r="55%">
      <stop offset="0%" stop-color="#fff6b8"/>
      <stop offset="25%" stop-color="#8be8ff"/>
      <stop offset="100%" stop-color="#0574d9"/>
    </radialGradient>
    <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#0496f6"/>
      <stop offset="62%" stop-color="#51c7ff"/>
      <stop offset="100%" stop-color="#f8fcff"/>
    </linearGradient>
    <filter id="softShadow" x="-30%" y="-30%" width="160%" height="180%">
      <feDropShadow dx="0" dy="18" stdDeviation="18" flood-color="#00325f" flood-opacity=".34"/>
    </filter>
    <filter id="avatarShadow" x="-60%" y="-70%" width="220%" height="240%">
      <feDropShadow dx="0" dy="7" stdDeviation="4" flood-color="#001c34" flood-opacity=".38"/>
    </filter>
  </defs>
  <rect width="1024" height="1024" fill="url(#sky)"/>
  <circle cx="138" cy="250" r="170" fill="url(#sun)" opacity=".82"/>
  <g opacity=".8" fill="#fff">
    <ellipse cx="154" cy="742" rx="190" ry="42"/>
    <ellipse cx="377" cy="815" rx="245" ry="62"/>
    <ellipse cx="718" cy="777" rx="295" ry="70"/>
    <ellipse cx="902" cy="671" rx="155" ry="42"/>
    <ellipse cx="96" cy="142" rx="75" ry="20"/>
    <ellipse cx="902" cy="168" rx="96" ry="22"/>
  </g>
  <g opacity=".34">
    <path d="M218 92 L256 118 L205 133 Z" fill="#fff"/>
    <path d="M824 96 L880 126 L811 147 Z" fill="#fff"/>
    <path d="M63 478 L132 500 L48 527 Z" fill="#fff"/>
  </g>
  <g>
    ${drawDrop("stone", 364, 205, "#83f3ff")}
    ${drawDrop("tnt", 483, 216, "#ff7b58")}
    ${drawDrop("potion", 589, 188, "#ff71fd")}
    ${drawDrop("sword", 696, 235, "#fff082")}
    ${drawDrop("swirl", 789, 191, "#87f8ff")}
  </g>
  <g filter="url(#softShadow)">
    ${sortedBlocks.map(drawCube).join("\n")}
    ${players.join("\n")}
  </g>
</svg>
`;

fs.writeFileSync(svgPath, svg);

sharp(Buffer.from(svg))
  .png()
  .toFile(pngPath)
  .then(() => {
    console.log(pngPath);
  })
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
