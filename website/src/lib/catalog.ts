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
  icon: {
    item_id: string | null;
    identifier: string | null;
    texture: string | null;
    names: LocalizedText;
    mapping: 'specific' | 'fallback';
    matched_terms: string[];
    source: string;
  };
  extraction: { method: string; file: string };
}

export interface SkillAnalysis {
  roots: number[];
  leaves: number[];
  bonus_types: string[];
  branch_counts: Record<string, number>;
  fork_count: number;
  max_depth_hint: number;
}

export interface SkillEditorialLocale {
  summary: string;
  body_markdown: string;
  key_systems: string[];
  xp_sources: string[];
  multiplayer: string;
}

export interface Skill {
  id: string;
  names: LocalizedText;
  status: string;
  coverage: 'deep' | 'structured' | string;
  introduced_in: string | null;
  visibility: string[];
  editorial: Record<string, SkillEditorialLocale>;
  nodes: PerkNode[];
  analysis: SkillAnalysis;
  related_content: {
    items: string[];
    recipes: string[];
    method: string;
    confidence: string;
  };
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

export interface RecipeVisualItem {
  id: string;
  names: LocalizedText;
  texture: string | null;
}

export interface RecipeIngredient {
  kind: 'item' | 'tag' | 'unknown';
  id: string;
  names: LocalizedText;
  representative: RecipeVisualItem;
  variants: RecipeVisualItem[];
  variant_count: number;
  variants_complete: boolean;
}

export interface RecipeEntry {
  id: string;
  type: string;
  result: { id: string; count: number; names: LocalizedText; texture: string | null };
  ingredients: string[];
  pattern: string[] | null;
  group: string | null;
  category: string | null;
  visual: {
    kind: 'shaped' | 'shapeless' | 'station';
    station: string;
    slots: Array<RecipeIngredient | null>;
    ingredients: RecipeIngredient[];
    pattern_width: number | null;
    pattern_height: number | null;
    shiftable: boolean;
  };
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

export interface ProgressionLevel {
  level: number;
  required: number;
  cumulative: number;
}

export interface ProgressionSystem {
  formula: {
    kind: string;
    coefficient: number;
    exponent: number;
    minimum: number;
    expression: string;
  };
  max_level: number;
  max_skill_points: number;
  node_unlock_cost: number;
  points_per_level: number;
  levels: ProgressionLevel[];
  reference_levels: number[];
  status: string;
  extraction: { method: string; files: string[] };
}

export interface FossilRarity {
  id: string;
  names: LocalizedText;
  rank: number;
  generation_weight: number;
  generation_percent: number;
  cleaning_ticks: number;
  cleaning_seconds: number;
  incubation_ticks: number;
  incubation_minutes: number;
}

export interface MiningSystem {
  fossils: {
    families: Array<{ id: string; names: LocalizedText }>;
    rarities: FossilRarity[];
    site_generation: { min_size: number; max_size: number; min_y: number; max_y: number };
  };
  area_effects: {
    vein_mining: { max_extra_blocks: number; toggle_default: boolean; server_authoritative: boolean };
    mining_3x3: { extra_positions: number; independent_toggle: boolean; server_authoritative: boolean };
  };
  extraction: { method: string; files: string[] };
}

export interface CookingRecipeEntry {
  id: string;
  kind: 'fixed' | 'generic';
  names: LocalizedText;
  category: string;
  rarity: string;
  shelf_life_days: number;
  ingredients: Array<{ item: string; category: string }>;
  ingredient_hints?: string[];
}

export interface EatingSystem {
  dish_categories: Array<{ id: string; names: LocalizedText }>;
  dish_rarities: Array<{ id: string; names: LocalizedText; rank: number; saturation: number }>;
  food_categories: Array<{ id: string; names: LocalizedText }>;
  ingredients: Array<{ id: string; names: LocalizedText; score: number; categories: string[] }>;
  cooking_recipes: CookingRecipeEntry[];
  dynamic_ingredient_sources: Array<{ id: string; description: LocalizedText }>;
  extraction: { method: string; files: string[] };
}

export interface FishingRarityEntry {
  id: string;
  names: LocalizedText;
  rank: number;
  base_weight: number;
  xp: number;
}

export interface SeaMonsterEntry {
  id: string;
  names: LocalizedText;
  weather: string;
  max_health: number;
  attack_damage: number;
  attack_radius: number;
  attack_interval_ticks: number;
  attack_interval_seconds: number;
  slime_size: number;
  horizontal_knockback: number;
  vertical_knockback: number;
  material_item: string;
  charm_item: string;
  title_id: string;
}

export interface FishingSystem {
  families: Array<{ id: string; names: LocalizedText; dimension_rule: string }>;
  rarities: FishingRarityEntry[];
  rarity_distributions: Array<{
    id: string;
    weights: Record<string, number>;
    percentages: Record<string, number>;
    source_symbol: string;
  }>;
  rarity_rune_shifts: Record<string, number>;
  family_distribution: {
    overworld_primary_percent: number;
    overworld_other_percent: number;
    nether: string;
    end: string;
  };
  mini_games: Array<{ rarity: string; game: string }>;
  weather: {
    modes: string[];
    base_radius: number;
    harmonized_radius: number;
    base_duration_ticks: number;
    sealed_duration_ticks: number;
  };
  sea_monsters: {
    max_gauge: number;
    normal_gauge_gain: number;
    sealed_gauge_gain: number;
    owner_xp: number;
    assistant_xp: number;
    base_hook_damage: number;
    sharpness_damage_per_level: number;
    types: SeaMonsterEntry[];
  };
  inventories: { fish_net_max_slots: number; fishing_boat_capacity: number };
  extraction: { method: string; files: string[] };
}


export interface ExtractedConstant {
  symbol: string;
  value: number | boolean | number[];
  unit: string;
  source: string;
  seconds?: number;
}

export interface BaronReward {
  identifier: string;
  names: LocalizedText;
  chance: number | null;
  count: number;
  note: LocalizedText;
}

export interface BaronTypeEntry {
  id: string;
  names: LocalizedText;
  tag: string;
  base_entities: string[];
  behavior: {
    summary: LocalizedText;
    file: string;
    constants: ExtractedConstant[];
  };
  rewards: BaronReward[];
}

export interface FightingSystem {
  xp: {
    normal_formula: string;
    baron_formula: string;
    health_divisor: number;
    normal_min: number;
    normal_max: number;
    baron_base_multiplier: number;
    baron_flat_bonus: number;
    vanilla_mob_xp_estimates: Array<{ group: string; xp: number; approximate: boolean }>;
    source_file: string;
  };
  barons: {
    promotion: {
      minimum_fighting_level_for_multiplayer_protection: number;
      player_search_radius: number;
      low_level_protection_radius: number;
      generic_scale_bonus: number;
      special_type_chance: number;
      chance_tiers: Array<{ min_level: number; max_level: number; chance: number }>;
      checked_once_per_entity: boolean;
      maximum_entity_age_ticks: number;
      lucky_block_forced_entity_types: string[];
    };
    scaling: {
      min_level: number;
      max_level: number;
      health_per_level: number;
      damage_per_level: number;
      xp_reward_per_level: number;
      samples: Array<{
        level: number;
        health_multiplier: number;
        damage_multiplier: number;
        xp_multiplier: number;
      }>;
    };
    types: BaronTypeEntry[];
  };
  legendary_items: Array<{
    id: string;
    identifier: string;
    names: LocalizedText;
    summary: LocalizedText;
    constants: ExtractedConstant[];
    source_file: string;
  }>;
  multiplayer: {
    promotion_authority: string;
    damage_authority: string;
    reward_authority: string;
    nearest_player_sets_spawn_level: boolean;
    low_level_nearby_player_can_block_promotion: boolean;
  };
  extraction: { method: string; files: string[] };
}

export interface CraftingSystem {
  xp: {
    score_multiplier: number;
    max_per_action: number;
    formula: string;
    processing_order: string[];
    night_window: { start_tick: number; end_tick: number };
    midnight_multiplier: number;
    green_vanilla_xp_ratio: number;
    green_vanilla_xp_cap: number;
  };
  craft_score: {
    item_points: Array<{ identifier: string; points: number }>;
    mythicrpg_item_default: number;
    other_item_default: number;
    excluded_systems: string[];
  };
  stations: Array<{
    id: string;
    names: LocalizedText;
    numeric_id: number;
    finite_durability: boolean;
    max_durability: number | null;
  }>;
  interface: {
    property_count: number;
    craft_result_slot: number;
    craft_input_start: number;
    craft_input_end: number;
    transformation_input_slot: number;
    transformation_output_slot: number;
  };
  bonuses: {
    portable_durability: number;
    table_durability: number;
    repair_kit_base_power: number;
    resource_save: { handled_server_side: boolean };
    reinforced_craft: { applies_unbreaking_level: number; handled_server_side: boolean };
    craft_charge: {
      charge_per_craft_xp: number;
      max_charge_per_craft: number;
      completion_threshold: number;
      bonus_next_level_ratio: number;
    };
    mythic_inspiration_multiplier: number;
    first_craft_multiplier: number;
    experience_charm_ratio: number;
    craft_mastery: {
      transfer_ratio: number;
      target_level_cap_ratio: number;
      target_skills: string[];
    };
  };
  recycling: {
    groups: Array<{
      id: string;
      result: string;
      inputs: string[];
      result_names: LocalizedText;
      input_names: LocalizedText[];
    }>;
    requires_perk: number;
    grants_crafting_xp: boolean;
  };
  transformations: {
    pairs: Array<{
      input: string;
      output: string;
      input_names: LocalizedText;
      output_names: LocalizedText;
    }>;
    requires_perk: number;
    charge_per_item: number;
    charge_source_expression: string;
  };
  lucky_blocks: {
    luck_min: number;
    luck_max: number;
    block_state_offset: number;
    category_formula: { positive: string; neutral: string; negative: string };
    category_parameters: {
      positive_base: number;
      positive_step: number;
      neutral_threshold: number;
      neutral_extreme: number;
      neutral_standard: number;
    };
    chance_samples: Array<{ luck: number; positive: number; neutral: number; negative: number }>;
    categories: Array<{
      id: string;
      events: Array<{ id: string; weight: number; names: LocalizedText; within_category_percent: number }>;
      total_weight: number;
    }>;
    ore_pool: Array<{ identifier: string; weight: number }>;
    infusion_rules: Array<{ delta: number; items: string[] }>;
    infusion_ingredient_count: number;
    craft_perk: number;
    infusion_perk: number;
    break_event_authority: string;
  };
  multiplayer: {
    craft_result_authority: string;
    persistent_player_states: string[];
    lucky_block_authority: string;
    portable_interface_is_client_view: boolean;
  };
  extraction: { method: string; files: string[] };
}

export type TravelingSystem = Record<string, any>;
export type BuildingSystem = Record<string, any>;
export type FarmingSystem = Record<string, any>;
export type WoodcuttingSystem = Record<string, any>;

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
  systems: {
    progression: ProgressionSystem;
    mining: MiningSystem;
    eating: EatingSystem;
    fishing: FishingSystem;
    fighting: FightingSystem;
    crafting: CraftingSystem;
    traveling: TravelingSystem;
    building: BuildingSystem;
    farming: FarmingSystem;
    woodcutting: WoodcuttingSystem;
  };
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

export function readableToken(value: string): string {
  return value.replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}
