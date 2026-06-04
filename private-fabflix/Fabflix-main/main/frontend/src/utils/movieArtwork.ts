import type { CSSProperties } from 'react';
import type { Movie } from '../types';
import { generatedPosterManifest } from '../posters/generatedPosterManifest';
import { generatedProfileManifest } from '../posters/generatedProfileManifest';
import { generatedTrailerManifest } from '../posters/generatedTrailerManifest';

const palettes = [
  ['#f04438', '#111827', '#f59e0b'],
  ['#0ea5e9', '#111827', '#f97316'],
  ['#14b8a6', '#111827', '#f43f5e'],
  ['#8b5cf6', '#111827', '#22c55e'],
  ['#f59e0b', '#111827', '#38bdf8'],
  ['#ef4444', '#0f172a', '#a3e635'],
  ['#06b6d4', '#171717', '#fb7185'],
  ['#84cc16', '#111827', '#eab308']
];

function hash(value: string) {
  return [...value].reduce((total, char) => (total * 31 + char.charCodeAt(0)) >>> 0, 7);
}

export function posterUrlFor(movieId: string) {
  return generatedPosterManifest[movieId] ?? null;
}

export function profileUrlFor(starId: string) {
  return generatedProfileManifest[starId] ?? null;
}

export function trailerKeyFor(movieId: string) {
  return generatedTrailerManifest[movieId] ?? null;
}

export function posterStyleFor(movie: Movie): CSSProperties {
  const selected = palettes[hash(`${movie.movie_id}-${movie.title}`) % palettes.length];
  return {
    '--poster-a': selected[0],
    '--poster-b': selected[1],
    '--poster-c': selected[2]
  } as CSSProperties;
}

export function splitList(value: string | null, limit = Number.MAX_SAFE_INTEGER) {
  return value ? value.split(/\s*,\s*/).filter(Boolean).slice(0, limit) : [];
}
