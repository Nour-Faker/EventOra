import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, KeyRound, LockKeyhole, RefreshCw } from 'lucide-react'
import AuthShell, { CodeInput } from '../components/AuthShell'
import { api } from '../api'
import { useToast } from '../components/Toast'
import { PasswordInput, StrengthMeter } from './AuthPage'

const DELAI = 60

// Mot de passe oublie en deux etapes qui glissent : email -> code + nouveau mot de passe
export default function ForgotPassword() {
  const location = useLocation()
  const navigate = useNavigate()
  const toast = useToast()
  const [step, setStep] = useState(0)
  const [email, setEmail] = useState(location.state?.email || '')
  const [code, setCode] = useState('')
  const [pw, setPw] = useState('')
  const [confirm, setConfirm] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(false)
  const [wait, setWait] = useState(0)

  useEffect(() => {
    if (wait <= 0) return undefined
    const t = setTimeout(() => setWait((w) => w - 1), 1000)
    return () => clearTimeout(t)
  }, [wait])

  const sendCode = async (e) => {
    e?.preventDefault()
    setBusy(true)
    try {
      await api.forgotPassword(email.trim())
      setStep(1); setWait(DELAI)
      toast.info('Si un compte existe pour cette adresse, un code vient d\'etre envoye.')
    } catch (err) { toast.error(err.message) } finally { setBusy(false) }
  }

  const reset = async (e) => {
    e.preventDefault()
    if (pw !== confirm) { toast.error('Les deux mots de passe ne correspondent pas.'); return }
    setBusy(true); setError(false)
    try {
      await api.resetPassword(email.trim(), code, pw)
      toast.success('Mot de passe modifie. Vous pouvez vous connecter.')
      navigate('/connexion', { replace: true, state: { email: email.trim() } })
    } catch (err) {
      toast.error(err.message)
      if (/code/i.test(err.message)) { setError(true); setCode(''); setTimeout(() => setError(false), 500) }
    } finally { setBusy(false) }
  }

  return (
    <AuthShell>
      <div className="auth-card auth-card--single">
        <div className="steps">
          <div className="steps-track" style={{ transform: `translateX(-${step * 50}%)` }}>
            {/* Etape 1 : adresse email */}
            <form className="auth-single step" onSubmit={sendCode} aria-hidden={step !== 0} inert={step !== 0}>
              <div className="mail-badge"><KeyRound size={32} strokeWidth={1.7} /></div>
              <h1>Mot de passe oublie</h1>
              <p className="muted">Saisissez l'adresse de votre compte : nous vous enverrons un code pour choisir un nouveau mot de passe.</p>
              <label className="field"><span>Email</span>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
              </label>
              <button className="btn btn--primary btn--block" disabled={busy || !email.trim()}>
                {busy && step === 0 ? <span className="spinner" /> : 'Recevoir un code'}
              </button>
              <Link to="/connexion" className="back-link">Retour a la connexion</Link>
            </form>

            {/* Etape 2 : code + nouveau mot de passe */}
            <form className="auth-single step" onSubmit={reset} aria-hidden={step !== 1} inert={step !== 1}>
              <div className="mail-badge"><LockKeyhole size={32} strokeWidth={1.7} /></div>
              <h1>Nouveau mot de passe</h1>
              <p className="muted">Code envoye a <strong className="text-strong">{email}</strong> (valable 15 minutes).</p>
              <CodeInput value={code} onChange={setCode} disabled={busy} error={error} />
              <label className="field"><span>Nouveau mot de passe</span>
                <PasswordInput value={pw} onChange={(e) => setPw(e.target.value)} autoComplete="new-password" placeholder="8 caracteres minimum" />
                <StrengthMeter password={pw} />
              </label>
              <label className="field"><span>Confirmer</span>
                <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" minLength={8} required />
              </label>
              <button className="btn btn--primary btn--block" disabled={busy || code.length !== 6 || pw.length < 8}>
                {busy && step === 1 ? <span className="spinner" /> : 'Changer le mot de passe'}
              </button>
              <div className="resend">
                <button type="button" className="link-btn icon-line" onClick={() => setStep(0)}><ArrowLeft size={14} /> Changer d'adresse</button>
                <button type="button" className="link-btn icon-line" disabled={wait > 0 || busy} onClick={sendCode}>
                  <RefreshCw size={14} /> {wait > 0 ? `Renvoyer dans ${wait}s` : 'Renvoyer le code'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </AuthShell>
  )
}
