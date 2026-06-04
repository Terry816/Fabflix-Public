import type { Movie } from '../types';
import { posterStyleFor, posterUrlFor } from '../utils/movieArtwork';

interface MoviePosterProps {
  movie: Movie;
  size?: 'card' | 'hero' | 'detail';
}

export default function MoviePoster({ movie, size = 'card' }: MoviePosterProps) {
  const posterUrl = posterUrlFor(movie.movie_id);

  if (posterUrl) {
    return (
      <img
        className={`movie-poster movie-poster-${size}`}
        src={posterUrl}
        alt={`${movie.title} poster`}
        loading={size === 'hero' ? 'eager' : 'lazy'}
      />
    );
  }

  return (
    <div className={`movie-poster poster-art movie-poster-${size}`} style={posterStyleFor(movie)}>
      <span className="poster-kicker">Fabflix</span>
      <strong>{movie.title}</strong>
      <small>{movie.year}</small>
    </div>
  );
}
