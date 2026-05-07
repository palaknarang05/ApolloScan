// Node.js
import fs from 'fs';
import path from 'path';

// Read the input file
const input = fs.readFileSync('filenames.txt', 'utf8');

// Split by file blocks
const fileBlocks = input.split('----------------------').map(block => block.trim()).filter(Boolean);

fileBlocks.forEach(block => {
    // Extract filename
    const match = block.match(/^File:\s*(.+)\n([\s\S]*)$/);
    if (!match) return;
    const filePath = match[1].replace(/\\/g, '/').trim();
    const content = match[2].trim();

    // Ensure directory exists
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }

    // Write file
    fs.writeFileSync(filePath, content, 'utf8');
    console.log(`Created: ${filePath}`);
});