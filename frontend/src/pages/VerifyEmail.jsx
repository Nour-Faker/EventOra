import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { MailCheck, RefreshCw } from 'lucide-react'
import AuthShell, { CodeInput } from '../components/AuthShell'
import { api } from '../api'
import { HOME_BY_ROLE, useAuth } from '../auth'
import { useToast } from '../components/Toast'

const DELAI = 60 // secondes entre deux envois (meme regle que le backend)

// Saisie du code recu par email apres l'inscription (ou a la connexion d'un compte non verifie)
export default function VerifyEmail() {
  const location = useLocation()
  const navigate = useNavigate()
  const { verifyEmail } = useAuth()
  const toast = useToast()
  const [email, setEmail] = useState(location.state?.email || '')
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(false)
  const [wait, setWait] = useState(location.state?.sent ? DELAI : 0)
  const resentOnce = useRef(false)

  // Compte a rebours avant de pouvoir redemander un code
  useEffect(() => {
    if (wait <= 0) return undefined
    const t = setTimeout(() => setWait((w) => w - 1), 1000)
    return () => clearTimeout(t)
  }, [wait])

  const resend = async () => {
    if (!email.trim()) { toast.error('Saisissez votre adresse email.'); return }
    try {
      await api.resendCode(email.trim())
      toast.success('Si un compte non verifie existe pour cette adresse, un nouveau code vient d\'etre envoye.')
      setWait(DELAI)
    } catch (err) { toast.error(err.message) }
  }

  // Arrivee depuis la connexion : on envoie un code frais automatiquement
  useEffect(() => {
    if (location.state?.resend && email && !resentOnce.current) {
      resentOnce.current = true
      api.resendCode(email).then(() => setWait(DELAI)).catch(() => {})
    }
  }, [location.state, email])

  const submit = async (value = code) => {
    if (value.length !== 6 || busy) return
    setBusy(true); setError(false)
    try {
      const user = await verifyEmail(email.trim(), value)
      toast.success(`Adresse confirmee. Bienvenue ${user.prenom} !`)
      navigate(location.state?.from || HOME_BY_ROLE[user.role] || '/', { replace: true })
    } catch (err) {
      toast.error(err.message)
      setError(true); setCode('')
      setTimeout(() => setError(false), 500)
    } finally { setBusy(false) }
  }

  // Validation automatique quand les 6 chiffres sont saisis
  const onCode = (v) => { setCode(v); if (v.length === 6) submit(v) }

  return (
    <AuthShell>
      <div className="auth-card auth-card--single">
        <form className="auth-single" onSubmit={(e) => { e.preventDefault(); submit() }}>
          <div className="mail-badge"><MailCheck size={34} strokeWidth={1.7} /></div>
          <h1>Verifiez votre email</h1>
          {location.state?.email ? (
            <p className="muted">Nous avons envoye un code a 6 chiffres a <strong className="text-strong">{email}</strong>. Il expire dans 15 minutes.</p>
          ) : (
            <>
              <p className="muted">Saisissez l'adresse de votre compte puis le code recu par email.</p>
              <label className="field"><span>Email</span>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
              </label>
            </>
          )}
          <CodeInput value={code} onChange={onCode} disabled={busy} autoFocus={!!location.state?.email} error={error} />
          <button className="btn btn--primary btn--block" disabled={busy || code.length !== 6 || !email}>
            {busy ? <span className="spinner" /> : 'Confirmer mon adresse'}
          </button>
          <div className="resend">
            <span className="muted">Pas recu ? Pensez aux spams.</span>
            <button type="button" className="link-btn icon-line" disabled={wait > 0} onClick={resend}>
              <RefreshCw size={14} /> {wait > 0 ? `Renvoyer dans ${wait}s` : 'Renvoyer le code'}
            </button>
          </div>
          <Link to="/connexion" className="back-link">Retour a la connexion</Link>
        </form>
      </div>
    </AuthShell>
  )
}
