/** Turns a flat equipment list (with parentId) into depth-first order with a depth value for indenting. */
export function flattenTree(items) {
  const byParent = new Map();
  items.forEach((item) => {
    const key = item.parentId ?? 0;
    if (!byParent.has(key)) byParent.set(key, []);
    byParent.get(key).push(item);
  });
  const out = [];
  const walk = (key, depth) =>
    (byParent.get(key) || []).forEach((item) => {
      out.push({ ...item, depth });
      walk(item.id, depth + 1);
    });
  walk(0, 0);
  return out;
}
