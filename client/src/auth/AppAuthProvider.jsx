import { Auth0Provider, useAuth0 } from '@auth0/auth0-react';
import { AuthContext } from './AuthContext';
import { AUTH0_AUDIENCE, AUTH0_CLIENT_ID, AUTH0_DOMAIN, AUTH_MODE } from '../config';

const demoUser = {
  sub: 'demo|alice',
  name: 'Demo Alice',
  nickname: 'alice',
  email: 'alice@example.invalid',
  picture: '/images/avatar1.png',
};

const devUser = {
  sub: 'dev|local',
  name: 'Local Dev',
  nickname: 'local-dev',
  email: 'dev@localhost.invalid',
  picture: '/images/avatar1.png',
};

function Auth0Bridge({ children }) {
  const auth = useAuth0();
  return <AuthContext.Provider value={auth}>{children}</AuthContext.Provider>;
}

export function AppAuthProvider({ children }) {
  if (AUTH_MODE !== 'auth0') {
    const localAuth = {
      isAuthenticated: true,
      isLoading: false,
      user: AUTH_MODE === 'dev' ? devUser : demoUser,
      getAccessTokenSilently: async () => (AUTH_MODE === 'dev' ? `dev-${devUser.sub}` : ''),
      loginWithRedirect: async () => {},
      logout: () => window.location.assign('/home'),
    };
    return <AuthContext.Provider value={localAuth}>{children}</AuthContext.Provider>;
  }

  return (
    <Auth0Provider
      domain={AUTH0_DOMAIN}
      clientId={AUTH0_CLIENT_ID}
      authorizationParams={{
        redirect_uri: window.location.origin + '/callback',
        audience: AUTH0_AUDIENCE,
      }}
    >
      <Auth0Bridge>{children}</Auth0Bridge>
    </Auth0Provider>
  );
}
