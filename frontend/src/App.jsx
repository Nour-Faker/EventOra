import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { HOME_BY_ROLE, RequireRole, useAuth } from './auth'
import Layout from './components/Layout'
import AuthPage from './pages/AuthPage'
import VerifyEmail from './pages/VerifyEmail'
import ForgotPassword from './pages/ForgotPassword'
import Home from './pages/Home'
import EventDetail from './pages/EventDetail'
import MySpace from './pages/MySpace'
import StandMap from './pages/StandMap'
import MyRequests from './pages/MyRequests'
import Checkin from './pages/Checkin'
import Profile from './pages/Profile'
import Dashboard from './pages/admin/Dashboard'
import Events from './pages/admin/Events'
import EventManage from './pages/admin/EventManage'
import Categories from './pages/admin/Categories'
import Requests from './pages/admin/Requests'
import Users from './pages/admin/Users'

// Pages de verification / mot de passe oublie : seulement si le backend envoie des emails
function EmailOnly({ children }) {
  const { emailActif } = useAuth()
  if (emailActif === false) return <Navigate to="/connexion" replace />
  return children
}

// Un utilisateur deja connecte n'a rien a faire sur la page de connexion
function GuestOnly({ children }) {
  const { user } = useAuth()
  const location = useLocation()
  if (user) return <Navigate to={location.state?.from || HOME_BY_ROLE[user.role] || '/'} replace />
  return children
}

const ADMIN = ['ADMINISTRATEUR']

export default function App() {
  return (
    <Routes>
      <Route path="/connexion" element={<GuestOnly><AuthPage /></GuestOnly>} />
      <Route path="/inscription" element={<GuestOnly><AuthPage /></GuestOnly>} />
      <Route path="/verification" element={<GuestOnly><EmailOnly><VerifyEmail /></EmailOnly></GuestOnly>} />
      <Route path="/mot-de-passe-oublie" element={<GuestOnly><EmailOnly><ForgotPassword /></EmailOnly></GuestOnly>} />

      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="evenements/:id" element={<EventDetail />} />
        <Route path="profil" element={<RequireRole><Profile /></RequireRole>} />

        <Route path="mon-espace" element={<RequireRole roles={['VISITEUR']}><MySpace /></RequireRole>} />
        <Route path="stands" element={<RequireRole roles={['EXPOSANT', 'ADMINISTRATEUR']}><StandMap /></RequireRole>} />
        <Route path="mes-demandes" element={<RequireRole roles={['EXPOSANT']}><MyRequests /></RequireRole>} />
        <Route path="checkin" element={<RequireRole roles={['AGENT_ACCUEIL']}><Checkin /></RequireRole>} />

        <Route path="admin" element={<RequireRole roles={ADMIN}><Dashboard /></RequireRole>} />
        <Route path="admin/evenements" element={<RequireRole roles={ADMIN}><Events /></RequireRole>} />
        <Route path="admin/evenements/:id" element={<RequireRole roles={ADMIN}><EventManage /></RequireRole>} />
        <Route path="admin/demandes" element={<RequireRole roles={ADMIN}><Requests /></RequireRole>} />
        <Route path="admin/categories" element={<RequireRole roles={ADMIN}><Categories /></RequireRole>} />
        <Route path="admin/utilisateurs" element={<RequireRole roles={ADMIN}><Users /></RequireRole>} />

        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}

function NotFound() {
  return (
    <div className="not-found">
      <p className="not-found-code gradient-text">404</p>
      <h1>Cette allee est vide</h1>
      <p className="muted">La page demandee n'existe pas.</p>
      <Link to="/" className="btn btn--primary">Retour a l'accueil</Link>
    </div>
  )
}
