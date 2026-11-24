import { useRef } from 'react'
import { Link } from 'react-router-dom'
import Logo from './Logo'
import { Background } from './Layout'
import { ThemeToggle } from '../theme'

// Cadre commun des pages de connexion, inscription, verification et mot de passe oublie
export default function AuthShell({ children, footer }) {
  return (
    <div className="auth-page">
      <Background />
      <Link to="/" className="auth-logo"><Logo size={40} animated tagline /></Link>
      <div className="auth-theme"><ThemeToggle /></div>
      {children}
      {footer}
    </div>
  )
}

// Saisie d'un code a 6 chiffres : avance automatique, retour arriere, collage du code entier
export function CodeInput({ value, onChange, disabled, autoFocus, error }) {
  const refs = useRef([])
  const digits = Array.from({ length: 6 }, (_, i) => value[i] || '')

  const setAt = (i, d) => {
    const next = digits.slice()
    next[i] = d
    onChange(next.join('').slice(0, 6))
  }
  const handleChange = (i, e) => {
    const typed = e.target.value.replace(/\D/g, '')
    if (!typed) { setAt(i, ''); return }
    if (typed.length > 1) { // collage ou saisie rapide
      const merged = (digits.slice(0, i).join('') + typed).slice(0, 6)
      onChange(merged)
      refs.current[Math.min(merged.length, 5)]?.focus()
      return
    }
    setAt(i, typed)
    if (i < 5) refs.current[i + 1]?.focus()
  }
  const handleKey = (i, e) => {
    if (e.key === 'Backspace' && !digits[i] && i > 0) { refs.current[i - 1]?.focus(); setAt(i - 1, '') }
    if (e.key === 'ArrowLeft' && i > 0) refs.current[i - 1]?.focus()
    if (e.key === 'ArrowRight' && i < 5) refs.current[i + 1]?.focus()
  }
  const handlePaste = (e) => {
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    if (!pasted) return
    e.preventDefault()
    onChange(pasted)
    refs.current[Math.min(pasted.length, 5)]?.focus()
  }

  return (
    <div className={`code-input${error ? ' shake' : ''}`} onPaste={handlePaste}>
      {digits.map((d, i) => (
        <input key={i} ref={(el) => { refs.current[i] = el }} value={d} inputMode="numeric" autoComplete={i === 0 ? 'one-time-code' : 'off'}
               maxLength={6} disabled={disabled} autoFocus={autoFocus && i === 0} aria-label={`Chiffre ${i + 1}`}
               className={d ? 'is-filled' : ''} style={{ '--i': i }}
               onChange={(e) => handleChange(i, e)} onKeyDown={(e) => handleKey(i, e)} onFocus={(e) => e.target.select()} />
      ))}
    </div>
  )
}
