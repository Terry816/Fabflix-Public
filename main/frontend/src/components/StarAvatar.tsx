import { UserRound } from 'lucide-react';
import { profileUrlFor } from '../utils/movieArtwork';

interface StarAvatarProps {
  starId: string;
  name: string;
  size?: 'chip' | 'hero';
}

export default function StarAvatar({ starId, name, size = 'chip' }: StarAvatarProps) {
  const profileUrl = profileUrlFor(starId);

  if (profileUrl) {
    return <img className={`star-avatar star-avatar-${size}`} src={profileUrl} alt={`${name} profile`} loading="lazy" />;
  }

  return (
    <span className={`star-avatar star-avatar-${size} star-avatar-fallback`} aria-hidden="true">
      <UserRound size={size === 'hero' ? 42 : 18} />
    </span>
  );
}
