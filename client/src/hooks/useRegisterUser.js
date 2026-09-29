import { useEffect } from 'react';
import { useAuth } from '../auth/AuthContext';
import { API_URL, apiFetch } from '../config';

/**
 * After successful Auth0 authentication, this hook ensures the
 * corresponding user record exists in the backend database.
 * It runs once per session when the user becomes authenticated.
 */
export default function useRegisterUser() {
    const { isAuthenticated, user, getAccessTokenSilently } = useAuth();

    useEffect(() => {
        if (!isAuthenticated || !user) return;

        (async () => {
            try {
                const token = await getAccessTokenSilently();
                const res = await apiFetch(`${API_URL}/user/`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({
                        id: user.sub,
                        name: user.name || user.nickname || '',
                        email: user.email,
                        picture: user.picture,
                    }),
                });
                if (!res.ok) {
                    console.error('POST /user/ failed', res.status);
                }
            } catch (err) {
                console.error('Failed to register user in backend', err);
            }
        })();
    }, [isAuthenticated, user, getAccessTokenSilently]);
}
