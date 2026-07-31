export interface NumberInputOptions {
  minimum?: number;
  maximum?: number;
  integer?: boolean;
}

const clamp = (value: number, minimum: number, maximum: number) => Math.min(maximum, Math.max(minimum, value));

export function readNumberInput(input: HTMLInputElement, options: NumberInputOptions = {}): number | null {
  const raw = input.value.trim();
  if (raw === '') return null;
  const parsed = Number(raw);
  if (!Number.isFinite(parsed)) return null;
  const minimum = options.minimum ?? Number.NEGATIVE_INFINITY;
  const maximum = options.maximum ?? Number.POSITIVE_INFINITY;
  const bounded = clamp(parsed, minimum, maximum);
  return options.integer ? Math.trunc(bounded) : bounded;
}

export function commitNumberInput(
  input: HTMLInputElement,
  fallback: number,
  options: NumberInputOptions = {},
): number {
  const value = readNumberInput(input, options) ?? clamp(
    fallback,
    options.minimum ?? Number.NEGATIVE_INFINITY,
    options.maximum ?? Number.POSITIVE_INFINITY,
  );
  input.value = String(options.integer ? Math.trunc(value) : value);
  return value;
}
