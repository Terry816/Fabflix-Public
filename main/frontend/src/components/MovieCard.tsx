import { Link } from 'react-router-dom';
import { Plus, Star } from 'lucide-react';
import type { Movie } from '../types';
import MoviePoster from './MoviePoster';
import { splitList } from '../utils/movieArtwork';

interface MovieCardProps {
  movie: Movie;
  variant?: 'rail' | 'grid';
  onAdd?: (movieId: string) => void;
}

export default function MovieCard({ movie, variant = 'grid', onAdd }: MovieCardProps) {
  const genres = splitList(movie.genres, 2);

  return (
    <article className={`movie-card movie-card-${variant}`}>
      <Link className="poster-link" to={`/movies/${movie.movie_id}`} aria-label={`View ${movie.title}`}>
        <MoviePoster movie={movie} />
      </Link>
      <div className="movie-card-body">
        <div className="movie-meta-line">
          <span>{movie.year}</span>
          <span className="rating-pill">
            <Star size={13} fill="currentColor" />
            {movie.rating?.toFixed(1) ?? 'N/A'}
          </span>
        </div>
        <Link className="movie-title-link" to={`/movies/${movie.movie_id}`}>
          {movie.title}
        </Link>
        <p>{movie.director}</p>
        <div className="movie-card-tags">
          {genres.map((genre) => (
            <Link key={genre} to={`/movies?genre=${encodeURIComponent(genre)}&minYear=2015`}>
              {genre}
            </Link>
          ))}
        </div>
        {onAdd && (
          <button className="small-button add-button" type="button" onClick={() => onAdd(movie.movie_id)}>
            <Plus size={15} />
            Add
          </button>
        )}
      </div>
    </article>
  );
}
