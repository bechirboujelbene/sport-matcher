import { useEffect } from 'react';
import { useAuth } from '../auth/AuthContext';

export default function SignUpPage() {
    const { loginWithRedirect, isLoading } = useAuth();

    useEffect(() => {
        loginWithRedirect({ screen_hint: 'signup' });
    }, [loginWithRedirect]);

    return (
        <main style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh' }}>
            {isLoading ? 'Redirecting to Auth0…' : 'Loading…'}
        </main>
    );
}