import type { Movie } from '../types';
import { posterStyleFor, posterUrlFor, trailerKeyFor } from '../utils/movieArtwork';

interface TrailerBackdropProps {
  movie: Movie;
  className?: string;
  showPosterBase?: boolean;
  startSeconds?: number;
}

export default function TrailerBackdrop({ movie, className = '', showPosterBase = true, startSeconds = 0 }: TrailerBackdropProps) {
  const trailerKey = trailerKeyFor(movie.movie_id);
  const posterUrl = posterUrlFor(movie.movie_id);

  if (trailerKey) {
    const params = new URLSearchParams({
      autoplay: '1',
      mute: '1',
      controls: '0',
      loop: '1',
      playlist: trailerKey,
      playsinline: '1',
      rel: '0',
      modestbranding: '1',
      disablekb: '1',
      fs: '0',
      iv_load_policy: '3',
      cc_load_policy: '0'
    });
    if (startSeconds > 0) {
      params.set('start', String(startSeconds));
    }

    return (
      <div className={`media-backdrop media-backdrop-video ${className}`.trim()} aria-hidden="true">
        {showPosterBase && posterUrl && <img className="media-backdrop-base" src={posterUrl} alt="" />}
        <iframe
          title=""
          src={`https://www.youtube.com/embed/${trailerKey}?${params.toString()}`}
          allow="autoplay; encrypted-media; picture-in-picture"
          loading="eager"
          referrerPolicy="strict-origin-when-cross-origin"
          tabIndex={-1}
        />
      </div>
    );
  }

  if (posterUrl) {
    return (
      <div className={`media-backdrop media-backdrop-poster ${className}`.trim()} aria-hidden="true">
        <img src={posterUrl} alt="" />
      </div>
    );
  }

  return <div className={`media-backdrop media-backdrop-art ${className}`.trim()} style={posterStyleFor(movie)} aria-hidden="true" />;
}
