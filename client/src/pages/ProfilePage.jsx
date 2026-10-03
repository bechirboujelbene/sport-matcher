import React, { useState, useEffect } from 'react';
import { useAuth } from '../auth/AuthContext';
import Header from '../components/Header';
import { useNotification } from '../contexts/useNotification';
import '../styles/ProfilePage.css';
import { API_URL, isDemoMode, apiFetch } from '../config';

const daysOfWeek = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
const timeSlots = ['Morning (6-12 PM)', 'Afternoon (12-6 PM)', 'Evening (6-10 PM)'];

const initialState = {
    firstName: '',
    lastName: '',
    email: '',
    location: '',
    bio: '',
    sports: [],
    skillLevel: '',
    availability: daysOfWeek.reduce((acc, d) => ({ ...acc, [d]: [] }), {}),
    avatar: '',
};

const allSports = ['Hiking', 'Running', 'Cycling', 'Swimming', 'Tennis', 'Basketball'];
const otherSports = ['Soccer', 'Baseball', 'Skiing', 'Snowboarding', 'Skateboarding', 'Surfing', 'Rowing', 'Boxing', 'Martial Arts', 'Climbing', 'Golf', 'Dancing', 'Yoga', 'Pilates', 'CrossFit', 'Weightlifting', 'Badminton', 'Table Tennis', 'Horseback Riding', 'Fencing'];
const skillLevels = ['Beginner', 'Intermediate', 'Advanced'];


function ProfilePage() {
    const { user, getAccessTokenSilently } = useAuth();
    const { notify } = useNotification();
    const [original, setOriginal] = useState(null);
    const [form, setForm] = useState(initialState);
    const [location, setLocation] = useState({ lat: null, lon: null, address: '' });
    // Extra sports selected from the "Other sports" dropdown
    const [extraSports, setExtraSports] = useState([]);
    // Merge default and extra sports for rendering
    const sportsOptions = React.useMemo(
        () => Array.from(new Set([...allSports, ...extraSports])),
        [extraSports]
    );

    // populate with Auth0 data when available
    useEffect(() => {
        if (user) {
            setForm(prev => ({
                ...prev,
                firstName: user.given_name || user.name?.split(' ')[0] || '',
                lastName: user.family_name || user.name?.split(' ').slice(1).join(' ') || '',
                email: user.email || '',
                avatar: user.picture || '',
            }));
        }
    }, [user]);


    // fetch full profile from backend
    useEffect(() => {
        if (!user) return;
        (async () => {
            try {
                const token = await getAccessTokenSilently();
                const res = await apiFetch(`${API_URL}/user/${encodeURIComponent(user.sub)}`, {
                    headers: { Authorization: `Bearer ${token}` },
                });
                if (!res.ok) return;
                const data = await res.json();
                const nameParts = (data.name || '').split(' ');
                const updatedProfile = {
                    firstName: nameParts[0] || '',
                    lastName: nameParts.slice(1).join(' '),
                    email: data.email || user.email,
                    bio: data.bio || '',
                    skillLevel: data.skillLevel || '',
                    sports: data.sportInterests || [],
                    availability: { ...initialState.availability, ...(data.availability || {}) },
                    avatar: data.picture || '',
                };
                setForm(updatedProfile);
                setOriginal(updatedProfile);
                if (data.sportInterests) {
                    setExtraSports(prev => {
                        const extras = data.sportInterests.filter((s) => !allSports.includes(s));
                        return Array.from(new Set([...prev, ...extras]));
                    });
                }
            } catch (e) {
                console.error('Failed to fetch user profile', e);
            }
        })();
    }, [user, getAccessTokenSilently]);

    // fetch location from backend and reverse-geocode to a human-readable address
    useEffect(() => {
        if (!user) return;
        (async () => {
            try {
                const token = await getAccessTokenSilently();
                const res = await apiFetch(`${API_URL}/location/${encodeURIComponent(user.sub)}`, {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });
                if (!res.ok) return;
                const data = await res.json(); // { latitude, longitude }
                const { latitude, longitude } = data;
                if (isDemoMode) {
                    setLocation({ lat: latitude, lon: longitude, address: 'Munich, Germany' });
                    setForm(prev => ({ ...prev, location: 'Munich, Germany' }));
                    return;
                }
                let address = '';
                if (latitude && longitude) {
                    try {
                        const gRes = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}`);
                        if (gRes.ok) {
                            const gJson = await gRes.json();
                            const addrObj = gJson.address || {};
                            const street = addrObj.road || addrObj.pedestrian || '';
                            const houseNo = addrObj.house_number || '';
                            const locality = addrObj.city || addrObj.town || addrObj.village || addrObj.municipality || addrObj.county || '';
                            const state = addrObj.state || '';
                            const parts = [];
                            if (street) parts.push(street + (houseNo ? ` ${houseNo}` : ''));
                            if (locality) parts.push(locality);
                            if (state) parts.push(state);
                            const formatted = parts.join(', ').trim();
                            setLocation({ lat: latitude, lon: longitude, address: formatted || address, raw: address });
                            setForm(prev => ({ ...prev, location: formatted || address }));
                        }
                    } catch {
                        // ignore geocode errors
                    }

                }
            } catch (e) {
                console.error('Failed to load location', e);
            }
        })();
    }, [user, getAccessTokenSilently]);

    // basic validation requirements
    const isValid = React.useMemo(() => {
        const hasSports = form.sports.length > 0;
        const hasSkill = !!form.skillLevel;
        const hasAvailability = Object.values(form.availability).some((arr) => arr.length > 0);
        return hasSports && hasSkill && hasAvailability;
    }, [form]);

    // determine if form differs from original (ignore email as it is read-only)
    const isDirty = React.useMemo(() => {
        if (!original) return false;
        const stripReadOnly = (obj = {}) => Object.fromEntries(
            Object.entries(obj).filter(([key]) => !['email', 'location'].includes(key))
        );
        return JSON.stringify(stripReadOnly(form)) !== JSON.stringify(stripReadOnly(original));
    }, [form, original]);

    const handleInput = (e) => {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    };

    const handleCheckbox = (section, value) => {
        setForm((prev) => {
            const current = new Set(prev[section]);
            current.has(value) ? current.delete(value) : current.add(value);
            return { ...prev, [section]: Array.from(current) };
        });
    };

    const toggleTime = (day, slot) => {
        setForm(prev => {
            const current = new Set(prev.availability[day]);
            current.has(slot) ? current.delete(slot) : current.add(slot);
            return { ...prev, availability: { ...prev.availability, [day]: Array.from(current) } };
        });
    };

    const toggleDay = (day) => {
        setForm(prev => {
            const hasAny = prev.availability[day].length > 0;
            return { ...prev, availability: { ...prev.availability, [day]: hasAny ? [] : [...timeSlots] } };
        });
    };



    const handleSave = async (e) => {
        e.preventDefault();
        if (!isValid) {
            const issues = [];
            if (form.sports.length === 0) issues.push('at least one sport');
            if (!form.skillLevel) issues.push('a skill level');
            if (!Object.values(form.availability).some(arr => arr.length > 0)) issues.push('an availability slot');
            notify({ type: 'error', message: `Please provide ${issues.join(', ')}` });
            return;
        }
        try {
            const token = await getAccessTokenSilently();
            const body = {
                firstName: form.firstName,
                lastName: form.lastName,
                bio: form.bio,
                skillLevel: form.skillLevel,
                availability: form.availability,
                sports: form.sports,
            };
            const res = await apiFetch(`${API_URL}/user/${encodeURIComponent(user.sub)}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(body),
            });
            if (!res.ok) throw new Error(`Failed: ${res.status}`);
            notify({ type: 'success', message: 'Profile saved!' });
        } catch (err) {
            console.error('Save failed', err);
            notify({ type: 'error', message: 'Save failed' });
        }
    };

    return (
        <>
            <Header isAuth />
            <section className="profile-page container">
                {/* Sidebar */}
                <aside className="profile-sidebar card">
                    <div className="avatar-wrapper">
                        <img src={form.avatar || `${import.meta.env.BASE_URL}images/avatar-profile.png`} alt="User avatar" className="avatar-lg" />
                        <span className="status-badge" />
                    </div>
                    <h3 className="user-name">{`${form.firstName} ${form.lastName}`}</h3>
                    <p className="location-text">{location.address || form.location}</p>
                    <span className="status-text active">Active</span>

                    <div className="sidebar-section">
                        <h4>Preferred Sports</h4>
                        <div className="tags">
                            {form.sports.map((s) => (
                                <span key={s} className="tag">
                                    {s}
                                </span>
                            ))}
                        </div>
                    </div>

                    <div className="sidebar-section">
                        <h3>User Information</h3>
                        {location.address && (
                            <div className="location-info">
                                <h4>Current Location</h4>
                                <p className="address">{location.address}</p>
                                <p className="coords">({location.lat?.toFixed(5)}, {location.lon?.toFixed(5)})</p>
                            </div>
                        )}
                        <h4>Skill Level</h4>
                        <p>{form.skillLevel}</p>
                    </div>

                    <div className="sidebar-section">
                        <h4>Availability</h4>
                        <div className="tags">
                            {daysOfWeek.flatMap((d) =>
                                form.availability[d].map((slot) => (
                                    <span key={`${d}-${slot}`} className="tag avail-tag">
                                        🗓️ {d.slice(0, 3)} {slot.split(' ')[0]}
                                    </span>
                                ))
                            )}
                            {daysOfWeek.every((d) => form.availability[d].length === 0) && (<span className="small-text">—</span>)}
                        </div>
                    </div>
                </aside>

                {/* Form content */}
                <div className="profile-content card" style={{ position: 'relative' }}>
                    <button
                        type="button"
                        className="btn refresh-btn"
                        onClick={() => window.location.reload()}
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0114.13-3.36L23 10"></path><path d="M20.49 15a9 9 0 01-14.13 3.36L1 14"></path></svg><span style={{ lineHeight: '1' }}>Refresh</span>
                    </button>
                    <h2>Edit Profile</h2>
                    <p className="subtitle">Update your information to find better matches</p>
                    <form onSubmit={handleSave} className="profile-form">
                        <div className="two-col">
                            <div className="form-group">
                                <label htmlFor="firstName">First Name</label>
                                <input
                                    id="firstName"
                                    name="firstName"
                                    value={form.firstName}
                                    onChange={handleInput}
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="lastName">Last Name</label>
                                <input
                                    id="lastName"
                                    name="lastName"
                                    value={form.lastName}
                                    onChange={handleInput}
                                />
                            </div>
                        </div>

                        <div className="form-group">
                            <label htmlFor="email">Email (read-only)</label>
                            <input
                                id="email"
                                name="email"
                                value={form.email}
                                readOnly
                                disabled
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="location">Location (read-only)</label>
                            <input
                                id="location"
                                name="location"
                                value={form.location}
                                readOnly
                                disabled
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="bio">Bio</label>
                            <textarea
                                id="bio"
                                name="bio"
                                rows="3"
                                value={form.bio}
                                onChange={handleInput}
                            />
                        </div>

                        <fieldset className="form-group">
                            <legend>Preferred Sports</legend>

                            {/* Dropdown to add sports that are not in the default list */}
                            <div className="other-sports-select">
                                <label htmlFor="otherSport" style={{ marginRight: '0.5rem' }}>Add other sport:</label>
                                <select
                                    id="otherSport"
                                    defaultValue=""
                                    onChange={(e) => {
                                        const value = e.target.value;
                                        if (value) {
                                            // Add to selected sports and to the extra list so it appears as a checkbox
                                            handleCheckbox('sports', value);
                                            setExtraSports((prev) => (prev.includes(value) ? prev : [...prev, value]));
                                            // Reset the dropdown back to placeholder
                                            e.target.value = '';
                                        }
                                    }}
                                >
                                    <option value="" disabled>Select sport</option>
                                    {otherSports
                                        .filter((sport) => !sportsOptions.includes(sport))
                                        .map((sport) => (
                                            <option key={sport} value={sport}>
                                                {sport}
                                            </option>
                                        ))}
                                </select>
                            </div>

                            <div className="checkbox-grid" style={{ marginTop: '1rem' }}>
                                {sportsOptions.map((sport) => (
                                    <label
                                        key={sport}
                                        className={`checkbox-label ${form.sports.includes(sport) ? 'selected' : ''}`}
                                    >
                                        <input
                                            type="checkbox"
                                            checked={form.sports.includes(sport)}
                                            onChange={() => handleCheckbox('sports', sport)}
                                        />
                                        <span>{sport}</span>
                                    </label>
                                ))}
                            </div>
                        </fieldset>

                        <fieldset className="form-group">
                            <legend>Skill Level</legend>
                            <div className="radio-group">
                                {skillLevels.map((level) => (
                                    <label key={level} className={`radio-label ${form.skillLevel === level ? 'selected' : ''}`}>
                                        <input
                                            type="radio"
                                            name="skillLevel"
                                            value={level}
                                            checked={form.skillLevel === level}
                                            onChange={handleInput}
                                        />
                                        <span>{level}</span>
                                    </label>
                                ))}
                            </div>
                        </fieldset>

                        <fieldset className="form-group">
                            <legend>Availability</legend>
                            <div className="availability-list">
                                {daysOfWeek.map((day) => (
                                    <div key={day} className="day-row">
                                        <label className={`checkbox-label day-select ${form.availability[day].length > 0 ? 'selected' : ''}`}>
                                            <input
                                                type="checkbox"
                                                checked={form.availability[day].length > 0}
                                                onChange={() => toggleDay(day)}
                                            />
                                            <span>{day}</span>
                                        </label>
                                        {form.availability[day].length > 0 && (
                                            <div className="time-options">
                                                {timeSlots.map((slot) => (
                                                    <label
                                                        key={slot}
                                                        className={`checkbox-label ${form.availability[day].includes(slot) ? 'selected' : ''}`}
                                                    >
                                                        <input
                                                            type="checkbox"
                                                            checked={form.availability[day].includes(slot)}
                                                            onChange={() => toggleTime(day, slot)}
                                                        />
                                                        <span className="icon-schedule" role="img" aria-label="schedule">🗓️</span>
                                                        <span>{slot}</span>
                                                    </label>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </fieldset>

                        <div className="form-actions">
                            <button
                                type="submit"
                                className={`btn save btn-primary`}
                                disabled={!isDirty}
                                style={{
                                    opacity: !isDirty ? 0.5 : 1,
                                    cursor: !isDirty ? 'not-allowed' : 'pointer',
                                }}
                            >
                                Save Profile
                            </button>
                        </div>
                    </form>
                </div>
            </section>
        </>
    );
}

export default ProfilePage;
