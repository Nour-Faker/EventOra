import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import Logo from './Logo'
import { ROLE_LABEL, useAuth } from '../auth'
import { ThemeToggle } from '../theme'

const LINKS = {
  ADMINISTRATEUR: [
    ['/admin', 'Tableau de bord'], ['/admin/evenements', 'Evenements'], ['/admin/demandes', 'Demandes'],
    ['/admin/categories', 'Categories'], ['/admin/utilisateurs', 'Utilisateurs'],
  ],
  EXPOSANT: [['/', 'Evenements'], ['/stands', 'Plan des stands'], ['/mes-demandes', 'Mes demandes']],
  VISITEUR: [['/', 'Evenements'], ['/mon-espace', 'Mon espace']],
  AGENT_ACCUEIL: [['/checkin', 'Controle des entrees']],
}

// Fond anime : halos de couleur qui derivent lentement + grille discrete
export function Background() {
  return (
    <div className="bg" aria-hidden="true">
      <span className="blob blob--1" /><span className="blob blob--2" /><span className="blob blob--3" />
      <span className="bg-grid" />
    </div>
  )
}

export default function Layout() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => { setOpen(false); window.scrollTo({ top: 0 }) }, [location.pathname])
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const links = user ? LINKS[user.role] || [] : [['/', 'Evenements']]
  const initials = user ? `${user.prenom?.[0] || ''}${user.nom?.[0] || ''}`.toUpperCase() : ''

  return (
    <>
      <Background />
      <nav className={`nav${scrolled ? ' nav--scrolled' : ''}`}>
        <div className="nav-inner">
          <NavLink to="/" className="nav-logo" aria-label="Accueil EventOra"><Logo size={34} animated /></NavLink>
          <div className={`nav-links${open ? ' is-open' : ''}`}>
            {links.map(([to, label]) => (
              <NavLink key={to} to={to} end={to === '/' || to === '/admin'} className="nav-link">{label}</NavLink>
            ))}
            {user ? (
              <div className="nav-user">
                <NavLink to="/profil" className="user-chip" title="Mon profil">
                  <span className="avatar">{initials}</span>
                  <span className="user-chip-text">
                    <strong>{user.prenom}</strong>
                    <small>{ROLE_LABEL[user.role]}</small>
                  </span>
                </NavLink>
                <button className="btn btn--ghost btn--sm" onClick={() => { logout(); navigate('/') }}>Deconnexion</button>
              </div>
            ) : (
              <NavLink to="/connexion" className="btn btn--primary btn--sm">Connexion</NavLink>
            )}
          </div>
          <ThemeToggle />
          <button className={`nav-burger${open ? ' is-open' : ''}`} onClick={() => setOpen((o) => !o)} aria-label="Menu" aria-expanded={open}>
            <span /><span /><span />
          </button>
        </div>
      </nav>
      <main className="main">
        <div key={location.pathname} className="page-enter">
          <Outlet />
        </div>
      </main>
      <footer className="footer">
        <Logo size={22} /> <span>Projet federateur ENICarthage · Gestion de salons et festivals</span>
      </footer>
    </>
  )
}
