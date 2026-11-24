import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Store, Ticket } from 'lucide-react'
import AuthShell from '../components/AuthShell'
import { HOME_BY_ROLE, useAuth } from '../auth'
import { useToast } from '../components/Toast'

// Connexion / inscription : un panneau colore glisse d'un formulaire a l'autre.
// La meme page sert /connexion et /inscription : React la conserve, d'ou l'animation.
export default function AuthPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const isRegister = location.pathname === '/inscription'
  const go = (to) => navigate(to, { replace: true, state: location.state })

  return (
    <AuthShell footer={
      <p className="auth-switch-mobile">
        {isRegister
          ? <>Deja un compte ? <button className="link-btn" onClick={() => go('/connexion')}>Se connecter</button></>
          : <>Pas encore de compte ? <button className="link-btn" onClick={() => go('/inscription')}>Creer un compte</button></>}
      </p>
    }>
      <div className={`auth-card${isRegister ? ' is-register' : ''}`}>
        <LoginForm active={!isRegister} />
        <RegisterForm active={isRegister} />

        <div className="auth-overlay" aria-hidden="true">
          <div className="auth-overlay-inner">
            <div className="auth-overlay-panel auth-overlay-panel--left">
              <h2>Deja inscrit ?</h2>
              <p>Retrouvez vos billets, vos stands et le programme de vos salons.</p>
              <button className="btn btn--outline-light" tabIndex={-1} onClick={() => go('/connexion')}>Se connecter</button>
            </div>
            <div className="auth-overlay-panel auth-overlay-panel--right">
              <h2>Nouveau sur EventOra ?</h2>
              <p>Creez un compte visiteur pour reserver vos billets, ou exposant pour louer un stand.</p>
              <button className="btn btn--outline-light" tabIndex={-1} onClick={() => go('/inscription')}>Creer un compte</button>
            </div>
          </div>
        </div>
      </div>
    </AuthShell>
  )
}

export function PasswordInput({ value, onChange, autoComplete, placeholder = 'Mot de passe' }) {
  const [show, setShow] = useState(false)
  return (
    <div className="input-wrap">
      <input type={show ? 'text' : 'password'} value={value} onChange={onChange} placeholder={placeholder}
             autoComplete={autoComplete} required minLength={autoComplete === 'new-password' ? 8 : undefined} />
      <button type="button" className="input-eye" onClick={() => setShow((s) => !s)}
              aria-label={show ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}>
        {show ? <EyeOff size={18} /> : <Eye size={18} />}
      </button>
    </div>
  )
}

function LoginForm({ active }) {
  const { login, emailActif } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState(location.state?.email || '')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [shake, setShake] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      const user = await login(email, password)
      toast.success(`Bienvenue ${user.prenom} !`)
      navigate(location.state?.from || HOME_BY_ROLE[user.role] || '/', { replace: true })
    } catch (err) {
      if (err.code === 'EMAIL_NON_VERIFIE') {
        // Mot de passe correct mais adresse pas encore confirmee : on envoie vers la saisie du code
        toast.info('Confirmez d\'abord votre adresse email.')
        navigate('/verification', { state: { email: email.trim(), resend: true, from: location.state?.from } })
        return
      }
      toast.error(err.message)
      setShake(true); setTimeout(() => setShake(false), 500)
    } finally { setBusy(false) }
  }

  return (
    <form className={`auth-form auth-form--login${active ? ' is-active' : ''}${shake ? ' shake' : ''}`}
          onSubmit={submit} aria-hidden={!active} inert={!active}>
      <h1 className="stagger" style={{ '--i': 0 }}>Connexion</h1>
      <p className="muted stagger" style={{ '--i': 1 }}>Heureux de vous revoir.</p>
      <label className="field stagger" style={{ '--i': 2 }}>
        <span>Email</span>
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
               placeholder="vous@exemple.tn" autoComplete="email" required />
      </label>
      <label className="field stagger" style={{ '--i': 3 }}>
        <span>Mot de passe</span>
        <PasswordInput value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
      </label>
      {emailActif && (
        <Link to="/mot-de-passe-oublie" state={{ email }} className="forgot-link stagger" style={{ '--i': 4 }}>Mot de passe oublie ?</Link>
      )}
      <button className="btn btn--primary btn--block stagger" style={{ '--i': 5 }} disabled={busy}>
        {busy ? <span className="spinner" /> : 'Se connecter'}
      </button>
    </form>
  )
}

function strength(p) {
  let s = 0
  if (p.length >= 8) s++
  if (p.length >= 12) s++
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) s++
  if (/\d/.test(p)) s++
  if (/[^A-Za-z0-9]/.test(p)) s++
  return Math.min(s, 4)
}
const STRENGTH = ['Trop court', 'Faible', 'Moyen', 'Bon', 'Excellent']

export function StrengthMeter({ password }) {
  const s = password ? strength(password) : 0
  return <span className={`strength strength--${s}`}><span className="strength-bar" /><small>{password ? STRENGTH[s] : ' '}</small></span>
}

function RegisterForm({ active }) {
  const { register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [f, setF] = useState({ role: 'VISITEUR', prenom: '', nom: '', email: '', telephone: '', motDePasse: '', nomEntreprise: '', description: '' })
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF((x) => ({ ...x, [k]: e.target.value }))
  const exposant = f.role === 'EXPOSANT'

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      const data = { ...f }
      if (!exposant) { delete data.nomEntreprise; delete data.description }
      const res = await register(data)
      if (!res.verificationRequise) { // verification par email desactivee : deja connecte
        toast.success(`Compte cree. Bienvenue ${res.user.prenom} !`)
        navigate(location.state?.from || HOME_BY_ROLE[res.user.role] || '/', { replace: true })
        return
      }
      if (res.emailEnvoye) toast.success('Compte cree ! Un code vient de vous etre envoye par email.')
      else toast.info("Compte cree. L'email n'a pas pu partir : demandez un nouveau code.")
      navigate('/verification', { state: { email: res.email, sent: res.emailEnvoye, from: location.state?.from } })
    } catch (err) {
      toast.error(err.message)
    } finally { setBusy(false) }
  }

  return (
    <form className={`auth-form auth-form--register${active ? ' is-active' : ''}`} onSubmit={submit}
          aria-hidden={!active} inert={!active}>
      <h1 className="stagger" style={{ '--i': 0 }}>Inscription</h1>
      <div className="segmented stagger" style={{ '--i': 1 }} role="radiogroup" aria-label="Type de compte">
        <span className="segmented-thumb" style={{ transform: `translateX(${exposant ? 100 : 0}%)` }} />
        <button type="button" role="radio" aria-checked={!exposant} className={!exposant ? 'is-on' : ''}
                onClick={() => setF((x) => ({ ...x, role: 'VISITEUR' }))}><Ticket size={16} /> Visiteur</button>
        <button type="button" role="radio" aria-checked={exposant} className={exposant ? 'is-on' : ''}
                onClick={() => setF((x) => ({ ...x, role: 'EXPOSANT' }))}><Store size={16} /> Exposant</button>
      </div>
      <div className="row stagger" style={{ '--i': 2 }}>
        <label className="field"><span>Prenom</span><input value={f.prenom} onChange={set('prenom')} autoComplete="given-name" required /></label>
        <label className="field"><span>Nom</span><input value={f.nom} onChange={set('nom')} autoComplete="family-name" required /></label>
      </div>
      <div className="row stagger" style={{ '--i': 3 }}>
        <label className="field"><span>Email</span><input type="email" value={f.email} onChange={set('email')} autoComplete="email" required /></label>
        <label className="field"><span>Telephone</span><input value={f.telephone} onChange={set('telephone')} autoComplete="tel" placeholder="Optionnel" /></label>
      </div>
      <div className={`collapse${exposant ? ' is-open' : ''}`}>
        <div className="collapse-inner">
          <label className="field"><span>Nom de l'entreprise</span>
            <input value={f.nomEntreprise} onChange={set('nomEntreprise')} required={exposant} tabIndex={exposant ? 0 : -1} />
          </label>
          <label className="field"><span>Description</span>
            <input value={f.description} onChange={set('description')} placeholder="Votre activite (optionnel)" tabIndex={exposant ? 0 : -1} />
          </label>
        </div>
      </div>
      <label className="field stagger" style={{ '--i': 4 }}>
        <span>Mot de passe</span>
        <PasswordInput value={f.motDePasse} onChange={set('motDePasse')} autoComplete="new-password" placeholder="8 caracteres minimum" />
        <StrengthMeter password={f.motDePasse} />
      </label>
      <button className="btn btn--primary btn--block stagger" style={{ '--i': 5 }} disabled={busy}>
        {busy ? <span className="spinner" /> : 'Creer mon compte'}
      </button>
    </form>
  )
}
