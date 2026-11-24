// Client de l'API EventOra (backend Java, voir README du backend).
// L'adresse se change avec VITE_API_URL dans .env.local (ex. http://localhost:8081).
export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '')

const TOKEN_KEY = 'eventora.token'
const USER_KEY = 'eventora.user'

export const session = {
  get token() { try { return localStorage.getItem(TOKEN_KEY) } catch { return null } },
  get user() {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null') } catch { return null }
  },
  save(token, user) {
    try { localStorage.setItem(TOKEN_KEY, token); localStorage.setItem(USER_KEY, JSON.stringify(user)) } catch { /* stockage indisponible */ }
  },
  updateUser(user) { try { localStorage.setItem(USER_KEY, JSON.stringify(user)) } catch { /* idem */ } },
  clear() { try { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY) } catch { /* idem */ } },
}

export class ApiError extends Error {
  // code : precision envoyee par le backend, ex. EMAIL_NON_VERIFIE
  constructor(status, message, code) { super(message); this.status = status; this.code = code }
}

// Appele quand le jeton est refuse (expire, compte bloque) : l'AuthProvider deconnecte.
let onUnauthorized = () => {}
export function setUnauthorizedHandler(fn) { onUnauthorized = fn }

async function request(method, path, body, { blob = false } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (session.token) headers.Authorization = `Bearer ${session.token}`

  let res
  try {
    res = await fetch(API_URL + path, {
      method, headers, body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, `Serveur injoignable (${API_URL}). Le backend est-il lance ?`)
  }

  if (!res.ok) {
    let message = `Erreur ${res.status}`
    let code
    try { const data = await res.json(); if (data?.erreur) message = data.erreur; code = data?.code } catch { /* corps vide */ }
    if (res.status === 401 && session.token) onUnauthorized(message)
    throw new ApiError(res.status, message, code)
  }
  if (blob) return res.blob()
  if (res.status === 204) return null
  const text = await res.text()
  return text ? JSON.parse(text) : null
}

const get = (p) => request('GET', p)
const post = (p, b) => request('POST', p, b)
const put = (p, b) => request('PUT', p, b)
const del = (p) => request('DELETE', p)

export const api = {
  // Comptes et profil
  config: () => get('/auth/config'),
  login: (email, motDePasse) => post('/auth/login', { email, motDePasse }),
  register: (data) => post('/auth/register', data),
  verifyEmail: (email, code) => post('/auth/verify-email', { email, code }),
  resendCode: (email) => post('/auth/resend-code', { email }),
  forgotPassword: (email) => post('/auth/forgot-password', { email }),
  resetPassword: (email, code, nouveauMotDePasse) => post('/auth/reset-password', { email, code, nouveauMotDePasse }),
  me: () => get('/me'),
  updateMe: (nom, prenom) => put('/me', { nom, prenom }),
  changePassword: (ancienMotDePasse, nouveauMotDePasse) =>
    put('/me/password', { ancienMotDePasse, nouveauMotDePasse }),

  // Evenements et programme
  events: () => get('/events'),
  event: (id) => get(`/events/${id}`),
  createEvent: (e) => post('/events', e),
  updateEvent: (id, e) => put(`/events/${id}`, e),
  deleteEvent: (id) => del(`/events/${id}`),
  sessions: (eventId) => get(`/events/${eventId}/sessions`),
  createSession: (eventId, s) => post(`/events/${eventId}/sessions`, s),
  updateSession: (id, s) => put(`/sessions/${id}`, s),
  deleteSession: (id) => del(`/sessions/${id}`),
  joinSession: (id) => post(`/sessions/${id}/registration`),
  leaveSession: (id) => del(`/sessions/${id}/registration`),
  mySessions: () => get('/me/sessions'),

  // Categories et stands
  categories: () => get('/categories'),
  createCategory: (c) => post('/categories', c),
  updateCategory: (id, c) => put(`/categories/${id}`, c),
  deleteCategory: (id) => del(`/categories/${id}`),
  stands: (eventId) => get(`/events/${eventId}/stands`),
  createStand: (eventId, s) => post(`/events/${eventId}/stands`, s),
  updateStand: (id, s) => put(`/stands/${id}`, s),
  deleteStand: (id) => del(`/stands/${id}`),

  // Demandes de stand
  requestStand: (standId) => post('/stand-requests', { standId }),
  myRequests: () => get('/me/stand-requests'),
  allRequests: () => get('/stand-requests'),
  decide: (id, decision, motif) => put(`/stand-requests/${id}`, { decision, motif }),

  // Billetterie
  ticketTypes: (eventId) => get(`/events/${eventId}/ticket-types`),
  createTicketType: (eventId, t) => post(`/events/${eventId}/ticket-types`, t),
  updateTicketType: (id, t) => put(`/ticket-types/${id}`, t),
  deleteTicketType: (id) => del(`/ticket-types/${id}`),
  buyTicket: (typeBilletId) => post('/tickets', { typeBilletId }),
  myTickets: () => get('/me/tickets'),
  cancelTicket: (id) => del(`/tickets/${id}`),
  ticketPdf: (id) => request('GET', `/tickets/${id}/pdf`, undefined, { blob: true }),
  checkin: (code) => post('/checkin', { code }),

  // Administration
  users: () => get('/users'),
  createAgent: (a) => post('/users/agents', a),
  blockUser: (id, bloque) => put(`/users/${id}/block`, { bloque }),
  deleteUser: (id) => del(`/users/${id}`),
  stats: () => get('/stats'),
}
