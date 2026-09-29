const runtimeConfig = window.__SPORT_MATCHER_CONFIG__ || {};
const getConfig = (key) => runtimeConfig[key] ?? import.meta.env[key] ?? '';
const defaultApiUrl = window.location.hostname === 'localhost'
  ? 'http://localhost:80'
  : `https://api.${window.location.hostname}`;

export const AUTH0_DOMAIN = getConfig('VITE_AUTH0_DOMAIN');
export const AUTH0_CLIENT_ID = getConfig('VITE_AUTH0_CLIENT_ID');
export const AUTH0_AUDIENCE = getConfig('VITE_AUTH0_AUDIENCE');
export const API_URL = getConfig('VITE_API_URL') || defaultApiUrl;
export const WS_URL = getConfig('VITE_WS_URL') || `${API_URL}/ws`;
export const isDemoMode = getConfig('VITE_DEMO_MODE') === 'true';
export const AUTH_MODE = getConfig('VITE_AUTH_MODE') || (isDemoMode ? 'demo' : 'auth0');

export function apiFetch(input, init) {
  if (isDemoMode) {
    return import('./demoApi').then(({ demoApiFetch }) => demoApiFetch(input, init));
  }
  return fetch(input, init);
}
