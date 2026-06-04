import type { ReactNode } from 'react';

interface LoadStateProps {
  loading?: boolean;
  error?: string | null;
  children: ReactNode;
}

export default function LoadState({ loading, error, children }: LoadStateProps) {
  if (loading) {
    return <div className="state-box">Loading...</div>;
  }

  if (error) {
    return <div className="state-box error">{error}</div>;
  }

  return <>{children}</>;
}
