import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { api, session, setUnauthorizedHandler } from './api'
import { useToast } from './components/Toast'

const AuthContext = createContext(null)

export const ROLE_LABEL = {
  ADMINISTRATEUR: 'Administrateur',
  EXPOSANT: 'Exposant',
  VISITEUR: 'Visiteur',
  AGENT_ACCUEIL: "Agent d'accueil",
}

// Page d'accueil de chaque role apres connexion
export const HOME_BY_ROLE = {
  ADMINISTRATEUR: '/admin',
  EXPOSANT: '/stands',
  VISITEUR: '/mon-espace',
  AGENT_ACCUEIL: '/checkin',
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(session.user)
  const toast = useToast()

  const logout = useCallback(() => { session.clear(); setUser(null) }, [])

  useEffect(() => {
    setUnauthorizedHandler((message) => {
      logout()
      toast.error(message || 'Session expiree, reconnectez-vous.')
    })
  }, [logout, toast])

  const login = useCallback(async (email, password) => {
    const res = await api.login(email, password)
    session.save(res.token, res.user)
    setUser(res.user)
    return res.user
  }, [])

  // L'inscription ne connecte pas : il faut d'abord verifier l'email avec le code recu
  // Fonctions par email actives cote backend ? (null tant que la reponse n'est pas arrivee)
  const [emailActif, setEmailActif] = useState(null)
  useEffect(() => {
    api.config().then((c) => setEmailActif(!!c?.emailActif), () => setEmailActif(false))
  }, [])

  // Emails actifs : pas de jeton, il faut saisir le code recu. Sinon : connexion directe.
  const register = useCallback(async (data) => {
    const res = await api.register(data)
    if (res.token) { session.save(res.token, res.user); setUser(res.user) }
    return res
  }, [])

  const verifyEmail = useCallback(async (email, code) => {
    const res = await api.verifyEmail(email, code)
    session.save(res.token, res.user)
    setUser(res.user)
    return res.user
  }, [])

  const refresh = useCallback((u) => { session.updateUser(u); setUser(u) }, [])

  const value = useMemo(() => ({ user, emailActif, login, register, verifyEmail, logout, refresh }),
    [user, emailActif, login, register, verifyEmail, logout, refresh])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() { return useContext(AuthContext) }

// Route protegee : connexion obligatoire, et role parmi `roles` si precise
export function RequireRole({ roles, children }) {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) return <Navigate to="/connexion" replace state={{ from: location.pathname }} />
  if (roles && !roles.includes(user.role)) return <Navigate to={HOME_BY_ROLE[user.role] || '/'} replace />
  return children
}
