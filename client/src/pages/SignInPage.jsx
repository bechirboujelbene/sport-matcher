import { useEffect } from 'react';
import { useAuth } from '../auth/AuthContext';

export default function SignInPage() {
    const { loginWithRedirect, isLoading } = useAuth();

    useEffect(() => {
        loginWithRedirect();
    }, [loginWithRedirect]);

    return (
        <main style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh' }}>
            <p>{isLoading ? 'Redirecting to Auth0…' : 'Loading…'}</p>
        </main>
    );
}


