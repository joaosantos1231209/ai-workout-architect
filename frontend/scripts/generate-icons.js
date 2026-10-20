const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// CRC32 table for PNG chunk checksums
const crcTable = [];
for (let n = 0; n < 256; n++) {
  let c = n;
  for (let k = 0; k < 8; k++) {
    c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
  }
  crcTable[n] = c >>> 0;
}

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    c = crcTable[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  }
  return (c ^ 0xffffffff) >>> 0;
}

function createChunk(type, data) {
  const len = data.length;
  const buf = Buffer.alloc(4 + 4 + len + 4);
  buf.writeUInt32BE(len, 0);
  buf.write(type, 4, 4, 'ascii');
  data.copy(buf, 8);
  const typeAndData = buf.subarray(4, 8 + len);
  buf.writeUInt32BE(crc32(typeAndData), 8 + len);
  return buf;
}

function generatePng(size) {
  const width = size;
  const height = size;

  // Raw pixel data: 1 byte filter type (0) + width * 4 (RGBA)
  const rowSize = 1 + width * 4;
  const rawData = Buffer.alloc(rowSize * height);

  const cx = width / 2;
  const cy = height / 2;
  const radius = width * 0.46;

  for (let y = 0; y < height; y++) {
    const rowOffset = y * rowSize;
    rawData[rowOffset] = 0; // Filter 0 (None)

    for (let x = 0; x < width; x++) {
      const pxOffset = rowOffset + 1 + x * 4;
      const dx = x - cx;
      const dy = y - cy;
      const dist = Math.sqrt(dx * dx + dy * dy);

      // Rounded squircle / icon background
      const qx = Math.abs(dx) / (width * 0.44);
      const qy = Math.abs(dy) / (height * 0.44);
      const inIcon = Math.pow(qx, 4) + Math.pow(qy, 4) <= 1.0;

      if (!inIcon) {
        // Transparent
        rawData[pxOffset] = 0;
        rawData[pxOffset + 1] = 0;
        rawData[pxOffset + 2] = 0;
        rawData[pxOffset + 3] = 0;
      } else {
        // Emerald / Teal gradient (#059669 -> #0f766e -> #042f2e)
        const t = (x + y) / (width + height);
        const r = Math.round(5 * (1 - t) + 15 * t);
        const g = Math.round(150 * (1 - t) + 118 * t);
        const b = Math.round(105 * (1 - t) + 110 * t);

        // Draw a stylized lightning bolt / dumbbell shape in center
        const nx = (x - cx) / (width * 0.5);
        const ny = (y - cy) / (height * 0.5);

        // Stylized emblem in white/mint
        let isEmblem = false;

        // Center house / dumbbell glyph
        // Bar
        if (Math.abs(ny) < 0.08 && Math.abs(nx) < 0.45) isEmblem = true;
        // Left plate
        if (Math.abs(nx + 0.45) < 0.08 && Math.abs(ny) < 0.3) isEmblem = true;
        if (Math.abs(nx + 0.35) < 0.06 && Math.abs(ny) < 0.22) isEmblem = true;
        // Right plate
        if (Math.abs(nx - 0.45) < 0.08 && Math.abs(ny) < 0.3) isEmblem = true;
        if (Math.abs(nx - 0.35) < 0.06 && Math.abs(ny) < 0.22) isEmblem = true;
        // Center home roof triangle
        if (ny < 0 && Math.abs(nx) < 0.25 && (ny + 0.45 + Math.abs(nx) * 1.2 < 0.2)) {
          isEmblem = true;
        }

        if (isEmblem) {
          rawData[pxOffset] = 255;
          rawData[pxOffset + 1] = 255;
          rawData[pxOffset + 2] = 255;
          rawData[pxOffset + 3] = 255;
        } else {
          rawData[pxOffset] = r;
          rawData[pxOffset + 1] = g;
          rawData[pxOffset + 2] = b;
          rawData[pxOffset + 3] = 255;
        }
      }
    }
  }

  const compressed = zlib.deflateSync(rawData);

  // IHDR chunk
  const ihdrData = Buffer.alloc(13);
  ihdrData.writeUInt32BE(width, 0);
  ihdrData.writeUInt32BE(height, 4);
  ihdrData[8] = 8; // 8 bits per channel
  ihdrData[9] = 6; // RGBA
  ihdrData[10] = 0; // Deflate
  ihdrData[11] = 0; // Filter
  ihdrData[12] = 0; // No interlace

  const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  const ihdr = createChunk('IHDR', ihdrData);
  const idat = createChunk('IDAT', compressed);
  const iend = createChunk('IEND', Buffer.alloc(0));

  return Buffer.concat([signature, ihdr, idat, iend]);
}

const iconsDir = path.join(__dirname, '../public/icons');
if (!fs.existsSync(iconsDir)) {
  fs.mkdirSync(iconsDir, { recursive: true });
}

const sizes = [72, 96, 128, 144, 152, 192, 384, 512];
for (const size of sizes) {
  const buf = generatePng(size);
  const filePath = path.join(iconsDir, `icon-${size}x${size}.png`);
  fs.writeFileSync(filePath, buf);
  console.log(`Generated icon ${size}x${size}`);
}

// Apple touch icon
fs.copyFileSync(path.join(iconsDir, 'icon-192x192.png'), path.join(iconsDir, 'apple-touch-icon.png'));
console.log('Icons generated successfully.');
