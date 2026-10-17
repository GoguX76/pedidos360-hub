/**
 * Categorías destacadas del catálogo.
 *
 * Se usan como lista curada en la página de inicio (pública, sin login),
 * así que no dependen de la API. El `id` viaja como query param a /menu
 * (`/menu?categoria=pasteleria`) para pre-filtrar el catálogo.
 *
 * Hoy el negocio cubre tres rubros: pastelería, panadería y cafetería.
 *
 * El ícono es una clase de Tabler Icons (`ti ti-<icon>`).
 */
export interface CatalogCategory {
  /** Slug usado en la URL (query param `categoria`). */
  id: string;
  /** Nombre visible. */
  name: string;
  /** Ícono de Tabler Icons, sin el prefijo `ti-`. */
  icon: string;
  /** Frase corta que acompaña a la categoría. */
  blurb: string;
}

export const CATALOG_CATEGORIES: CatalogCategory[] = [
  { id: 'pasteleria', name: 'Pastelería', icon: 'cake', blurb: 'Tortas y dulces del día' },
  { id: 'panaderia', name: 'Panadería', icon: 'bread', blurb: 'Pan recién horneado' },
  { id: 'cafeteria', name: 'Cafetería', icon: 'coffee', blurb: 'Café y algo para acompañar' },
];

/**
 * Íconos por categoría. Es un mapa más amplio que `CATALOG_CATEGORIES`:
 * las tarjetas del menú pueden mostrar cualquier categoría que venga del
 * backend, no solo las tres destacadas del inicio.
 */
const ICON_BY_SLUG: Record<string, string> = {
  pasteleria: 'cake',
  panaderia: 'bread',
  cafeteria: 'coffee',
  cafe: 'coffee',
  pizzas: 'pizza',
  pizza: 'pizza',
  postres: 'ice-cream',
  postre: 'ice-cream',
  salados: 'meat',
  bebidas: 'cup',
  bebida: 'cup',
  ensaladas: 'salad',
  ensalada: 'salad',
};

/**
 * Normaliza un nombre de categoría a slug: sin acentos, minúsculas y
 * sin espacios sobrantes. Así "Pastelería" y "pasteleria" coinciden.
 */
export function categorySlug(value: string | null | undefined): string {
  return (value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLowerCase();
}

/** Devuelve la categoría destacada por `id` (o `undefined` si no existe). */
export function findCategory(id: string | null | undefined): CatalogCategory | undefined {
  const slug = categorySlug(id);
  if (!slug) return undefined;
  return CATALOG_CATEGORIES.find((c) => c.id === slug);
}

/** Ícono de Tabler para un nombre de categoría dado; `package` por defecto. */
export function categoryIcon(name: string | null | undefined): string {
  const slug = categorySlug(name);
  const direct = ICON_BY_SLUG[slug];
  if (direct) return direct;
  const curated = CATALOG_CATEGORIES.find(
    (c) => c.id === slug || categorySlug(c.name) === slug,
  );
  return curated ? curated.icon : 'package';
}
