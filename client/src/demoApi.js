const demoUserId = 'demo|alice';
const demoCandidates = [
  {
    id: 'demo|sam',
    name: 'Sam Rivera',
    firstName: 'Sam',
    lastName: 'Rivera',
    email: 'sam@example.invalid',
    picture: '/images/avatar2.png',
    bio: 'Weekend hiker who enjoys exploring new trails.',
    sportInterests: ['Hiking', 'Cycling'],
    skillLevel: 'Intermediate',
    availability: { Saturday: ['Morning (6-12 PM)'], Sunday: ['Morning (6-12 PM)'] },
  },
  {
    id: 'demo|jordan',
    name: 'Jordan Kim',
    firstName: 'Jordan',
    lastName: 'Kim',
    email: 'jordan@example.invalid',
    picture: '/images/avatar3.png',
    bio: 'I like tennis, running, and training outdoors.',
    sportInterests: ['Tennis', 'Running'],
    skillLevel: 'Advanced',
    availability: { Monday: ['Evening (6-10 PM)'], Saturday: ['Morning (6-12 PM)'] },
  },
  {
    id: 'demo|taylor',
    name: 'Taylor Morgan',
    firstName: 'Taylor',
    lastName: 'Morgan',
    email: 'taylor@example.invalid',
    picture: '/images/avatar4.png',
    bio: 'New to the city and keen to try climbing.',
    sportInterests: ['Climbing', 'Hiking'],
    skillLevel: 'Beginner',
    availability: { Friday: ['Evening (6-10 PM)'], Sunday: ['Afternoon (12-6 PM)'] },
  },
];

const initialProfile = {
  id: demoUserId,
  name: 'Alice Demo',
  firstName: 'Alice',
  lastName: 'Demo',
  email: 'alice@example.invalid',
  picture: '/images/avatar1.png',
  bio: 'Enjoys hiking and finding new outdoor routes.',
  sportInterests: ['Hiking', 'Running'],
  skillLevel: 'Intermediate',
  availability: { Saturday: ['Morning (6-12 PM)'], Sunday: ['Morning (6-12 PM)'] },
};

const storageKey = 'sport-matcher-demo-state-v1';

function resolveUserId(init) {
  const headers = init.headers || {};
  const authorization = typeof headers.get === 'function'
    ? headers.get('Authorization') || headers.get('authorization')
    : headers.Authorization || headers.authorization;
  const token = authorization && authorization.match(/^[Bb]earer\s+(.+)$/)?.[1];
  if (!token) return demoUserId;
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return payload.sub || demoUserId;
  } catch {
    return demoUserId;
  }
}

function readState() {
  try {
    return {
      profile: initialProfile,
      location: { latitude: 48.1372, longitude: 11.5756 },
      history: [],
      contacts: [],
      conversations: {},
      ...JSON.parse(localStorage.getItem(storageKey) || '{}'),
    };
  } catch {
    return {
      profile: initialProfile,
      location: { latitude: 48.1372, longitude: 11.5756 },
      history: [],
      contacts: [],
      conversations: {},
    };
  }
}

function writeState(state) {
  localStorage.setItem(storageKey, JSON.stringify(state));
}

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function scoreCandidate(profile, candidate) {
  const ownSports = new Set((profile.sportInterests || []).map((sport) => sport.trim().toLowerCase()));
  const candidateSports = candidate.sportInterests || [];
  const candidateSportSet = new Set(candidateSports.map((sport) => sport.trim().toLowerCase()));
  const commonNormalized = new Set([...ownSports].filter((sport) => candidateSportSet.has(sport)));
  const common = [...new Set(candidateSports.filter((sport) => commonNormalized.has(sport.trim().toLowerCase())))].sort((left, right) => left.localeCompare(right));
  const union = new Set([...ownSports, ...candidateSportSet]);
  const sportScore = union.size ? commonNormalized.size / union.size : 0;
  const levels = ['beginner', 'intermediate', 'advanced', 'expert'];
  const ownLevel = levels.indexOf((profile.skillLevel || '').trim().toLowerCase());
  const candidateLevel = levels.indexOf((candidate.skillLevel || '').trim().toLowerCase());
  const skillScore = ownLevel < 0 || candidateLevel < 0
    ? 0.5
    : 1 - Math.abs(ownLevel - candidateLevel) / (levels.length - 1);
  const stopwords = new Set(['a', 'an', 'and', 'are', 'as', 'at', 'be', 'for', 'from', 'i', 'in', 'is', 'it', 'my', 'of', 'on', 'or', 'the', 'to', 'we', 'with']);
  const tokens = (bio) => new Set((bio || '').toLowerCase().match(/[a-z0-9]+/g)?.filter((token) => token.length > 1 && !stopwords.has(token)) || []);
  const ownBio = tokens(profile.bio);
  const candidateBio = tokens(candidate.bio);
  const bioUnion = new Set([...ownBio, ...candidateBio]);
  const bioScore = bioUnion.size ? [...ownBio].filter((token) => candidateBio.has(token)).length / bioUnion.size : 0;
  const score = Math.round((0.55 * sportScore + 0.25 * skillScore + 0.20 * bioScore) * 10000) / 10000;
  const reasons = [];
  if (common.length) reasons.push(`shared sports: ${common.join(', ')}`);
  if (skillScore >= 0.8) reasons.push('compatible skill levels');
  else if (skillScore >= 0.5) reasons.push('partially compatible skill levels');
  if (bioScore > 0) reasons.push('similar profile interests');

  return {
    id: candidate.id,
    score,
    explanation: reasons.join('; ') || 'No strong shared preferences yet',
    common_preferences: common,
  };
}

export function demoApiFetch(input, init = {}) {
  const url = new URL(typeof input === 'string' ? input : input instanceof URL ? input.href : input.url, window.location.origin);
  const method = (init.method || 'GET').toUpperCase();
  const path = url.pathname;
  const state = readState();
  const userId = resolveUserId(init);

  if (path === '/user/' && method === 'POST') {
    state.profile = { ...state.profile, id: userId };
    writeState(state);
    return jsonResponse({ id: userId }, 201);
  }
  if (path.startsWith('/user/') && method === 'GET') {
    const id = decodeURIComponent(path.slice('/user/'.length));
    const person = id === userId ? { ...state.profile, id: userId } : demoCandidates.find((candidate) => candidate.id === id);
    return person ? jsonResponse(person) : jsonResponse({ detail: 'User not found' }, 404);
  }
  if (path.startsWith('/user/') && method === 'PUT') {
    const body = JSON.parse(init.body || '{}');
    const { firstName, lastName, sports, ...profileFields } = body;
    state.profile = {
      ...state.profile,
      ...profileFields,
      id: userId,
      name: `${firstName || ''} ${lastName || ''}`.trim(),
      sportInterests: sports || state.profile.sportInterests,
    };
    writeState(state);
    return jsonResponse(state.profile);
  }
  if (path === '/location/update' && method === 'POST') {
    state.location = {
      latitude: Number(url.searchParams.get('latitude')) || state.location.latitude,
      longitude: Number(url.searchParams.get('longitude')) || state.location.longitude,
    };
    writeState(state);
    return jsonResponse({ status: 'ok' });
  }
  if (path.startsWith('/location/') && method === 'GET') return jsonResponse(state.location);
  if (path.startsWith('/matching/history/') && method === 'GET') return jsonResponse(state.history);
  if (path.startsWith('/matching/partners/') && method === 'POST') {
    const ranked = demoCandidates
      .map((candidate) => ({ candidate, match: scoreCandidate(state.profile, candidate) }))
      .sort((left, right) => right.match.score - left.match.score || left.candidate.id.localeCompare(right.candidate.id));
    state.history = ranked.map(({ match }) => ({
      matchedUserId: match.id,
      score: match.score,
      explanation: match.explanation,
      commonPreferences: match.common_preferences,
    }));
    writeState(state);
    return jsonResponse(ranked.map(({ candidate }) => candidate));
  }
  if (path === '/messaging/contact' && method === 'POST') {
    const contactId = url.searchParams.get('contactId');
    if (contactId && !state.contacts.includes(contactId)) state.contacts.push(contactId);
    writeState(state);
    return jsonResponse({ status: 'ok' });
  }
  if (path.startsWith('/messaging/contacts/') && method === 'GET') {
    return jsonResponse(state.contacts.map((id) => demoCandidates.find((candidate) => candidate.id === id)).filter(Boolean));
  }
  if (path === '/messaging/conversation' && method === 'GET') {
    const contactId = url.searchParams.get('userA') === userId
      ? url.searchParams.get('userB')
      : url.searchParams.get('userA');
    return jsonResponse({ content: state.conversations[contactId] || [] });
  }
  if (path === '/messaging/send' && method === 'POST') {
    const message = { ...JSON.parse(init.body || '{}'), timestamp: new Date().toISOString() };
    state.conversations[message.toUserId] = [...(state.conversations[message.toUserId] || []), message];
    writeState(state);
    return jsonResponse(message, 201);
  }

  return jsonResponse({ detail: 'Not found' }, 404);
}
