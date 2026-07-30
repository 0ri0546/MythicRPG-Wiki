import rawCatalog from '../data/generated/catalog.json';
import rawReport from '../data/generated/extraction-report.json';

export interface LocalizedText {
  fr: string;
  en: string;
}

export interface PerkNode {
  id: number;
  skill_id: string;
  slug: string;
  position: { x: number; y: number };
  parent_ids: number[];
  fork_id: number;
  branch_id: number;
  bonuses: Array<{ type: string; value: number | null }>;
  effects: Array<{ effect: string; amplifier: number }>;
  poison_on_hit: { amplifier: number; duration_ticks: number } | null;
  names: LocalizedText;
  descriptions: LocalizedText;
  extraction: { method: string; file: string };
}

export interface Skill {
  id: string;
  names: LocalizedText;
  status: string;
  introduced_in: string | null;
  visibility: string[];
  editorial: Record<string, { summary: string; body_markdown: string }>;
  nodes: PerkNode[];
  extraction: { method: string; file: string };
}

export interface ItemEntry {
  id: string;
  identifier: string;
  kind: string;
  names: LocalizedText;
  translation_key: string;
  model: string;
  texture: string | null;
  registration_evidence: boolean;
  registration_status: 'confirmed' | 'dynamic_probable' | 'model_only';
  evidence: {
    literal_item_registration: boolean;
    literal_block_registration: boolean;
    recipe_reference: boolean;
    translation: boolean;
    texture: boolean;
    blockstate: boolean;
  };
  recipe_available: boolean;
  extraction: { method: string; file: string };
}

export interface BlockEntry {
  id: string;
  identifier: string;
  names: LocalizedText;
  texture: string | null;
  registration_evidence: boolean;
  extraction: { method: string; file: string };
}

export interface RecipeEntry {
  id: string;
  type: string;
  result: { id: string; count: number; names: LocalizedText };
  ingredients: string[];
  pattern: string[] | null;
  group: string | null;
  extraction: { method: string; file: string };
}

export interface DocumentedValue {
  id: string;
  value: number | string | boolean;
  unit: string | null;
  status: string;
  label: LocalizedText;
  audiences: string[];
  formula_group: string | null;
  source: { file: string; symbol: string; expression: string };
}

export interface Catalog {
  schema_version: string;
  source: {
    canonical_source: string;
    received_archive: string;
    archive_sha256: string;
    tree_sha256: string;
    inspection: string;
    gradle_run: boolean;
    minecraft_run: boolean;
    source_root: string;
  };
  locales: Record<string, number>;
  skills: Skill[];
  items: ItemEntry[];
  blocks: BlockEntry[];
  recipes: RecipeEntry[];
  values: DocumentedValue[];
}

export const catalog = rawCatalog as unknown as Catalog;
export const extractionReport = rawReport as {
  status: string;
  counts: Record<string, number>;
  errors: string[];
  warnings: string[];
  static_inspection_only: boolean;
};

export const locale = 'fr' as const;

export function itemRegistrationLabel(status: ItemEntry['registration_status']): string {
  const labels: Record<ItemEntry['registration_status'], string> = {
    confirmed: 'Objet enregistré confirmé',
    dynamic_probable: 'Enregistrement dynamique probable',
    model_only: 'Modèle détecté, présence en jeu non confirmée'
  };
  return labels[status];
}
export const basePath = import.meta.env.BASE_URL.endsWith('/')
  ? import.meta.env.BASE_URL
  : `${import.meta.env.BASE_URL}/`;

export function sitePath(path: string): string {
  return `${basePath}${path.replace(/^\/+/, '')}`;
}

export function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    stable: 'Stable',
    beta: 'Bêta',
    stabilizing: 'En stabilisation',
    unknown: 'À qualifier'
  };
  return labels[status] ?? status;
}

export function readableIdentifier(value: string): string {
  return value.replace(/^#?minecraft:/, '').replace(/^mythicrpg:/, '').replaceAll('_', ' ');
}
