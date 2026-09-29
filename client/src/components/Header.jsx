import React from 'react';
import { useAuth } from '../auth/AuthContext';
import { useNavigate } from 'react-router-dom';
import { NavLink } from 'react-router-dom';
import '../styles/LandingPage.css';

const Header = () => {
    const { isAuthenticated, user, logout, loginWithRedirect } = useAuth();
    const navigate = useNavigate();
    const authNav = [
        { path: '/home', label: 'Home' },
        { path: '/matches', label: 'Matching' },
        { path: '/profile', label: 'Profile' },
        { path: '/messages', label: 'Messages' },
    ];

    const guestNav = [
        { href: '#how-it-works', label: 'How It Works' },
        { href: '#testimonials', label: 'Testimonials' },
        { href: '#contact', label: 'Contact' }
    ];

    return (
        <header className="header">
            <div className="nav-container">
                <div className="brand">
                    <img src="/logo.png" alt="SportMatch logo" className="brand-logo" />
                    <span>SportMatch</span>
                </div>

                <nav>
                    {isAuthenticated ? (
                        <ul className="nav-links">
                            {authNav.map((item) => (
                                <li key={item.path}>
                                    <NavLink
                                        to={item.path}
                                        className={({ isActive }) => (isActive ? 'active' : '')}
                                    >
                                        {item.label}
                                    </NavLink>
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <ul className="nav-links">
                            {guestNav.map((item) => (
                                <li key={item.href}>
                                    <a href={item.href}>{item.label}</a>
                                </li>
                            ))}
                        </ul>
                    )}
                </nav>

                {isAuthenticated ? (
                    <div className="auth-links">
                        <button className="icon-btn" aria-label="Notifications">
                            {/* <img src="/images/icon-bell.svg" alt="Bell" /> */}
                        </button>
                        {user?.picture && (
                            <img
                                src={user.picture}
                                alt={user.name}
                                className="avatar-small"
                                style={{ width: 32, height: 32, borderRadius: '50%' }}
                                onClick={() => navigate('/profile')}
                            />
                        )}                        <button onClick={() => logout({ logoutParams: { returnTo: window.location.origin + '/logout' } })} className="btn btn-secondary">Log out</button>
                    </div>
                ) : (
                    <div className="auth-links">
                        <button onClick={() => loginWithRedirect()} className="btn btn-secondary">Login/Sign Up</button>
                        <button onClick={() => loginWithRedirect({ screen_hint: 'signup' })} className="btn btn-secondary">Get Started</button>
                    </div>
                )}
            </div>
        </header>
    );


};

export default Header;

