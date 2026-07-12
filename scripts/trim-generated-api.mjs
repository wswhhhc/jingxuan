import { readdir, readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(
  new URL("../frontend/src/shared/api/generated/", import.meta.url),
);

async function normalize(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const file = join(directory, entry.name);
    if (entry.isDirectory()) {
      await normalize(file);
    } else if (entry.name.endsWith(".ts")) {
      const source = await readFile(file, "utf8");
      const normalized = `${source.replace(/[\t ]*\r?\n(?:[\t ]*\r?\n)*$/, "")}\n`;
      if (source !== normalized) await writeFile(file, normalized);
    }
  }
}

await normalize(root);
