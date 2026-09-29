import React from 'react';
import { useAuth } from '../auth/AuthContext';
import { Navigate, useLocation } from 'react-router-dom';
import LoadingSpinner from './LoadingSpinner';

export default function RequireAuth({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) return <LoadingSpinner text="Checking authentication…" />;
  if (!isAuthenticated) return <Navigate to="/signin" state={{ from: location }} replace />;
  return children;
}
