import { Sparkles, X } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'

// Charge des donnees a l'affichage et permet de recharger apres une action
export function useLoad(loader, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null })
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(loader, deps)
  const reload = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: null }))
    return run().then(
      (data) => setState({ data, loading: false, error: null }),
      (error) => setState({ data: null, loading: false, error }),
    )
  }, [run])
  useEffect(() => { reload() }, [reload])
  return { ...state, reload }
}

export function Loader({ label = 'Chargement' }) {
  return <div className="loader" role="status"><span className="loader-ring" /><span>{label}...</span></div>
}

export function ErrorBox({ error, onRetry }) {
  return (
    <div className="error-box">
      <p>{error?.message || 'Une erreur est survenue.'}</p>
      {onRetry && <button className="btn btn--ghost" onClick={onRetry}>Reessayer</button>}
    </div>
  )
}

export function Empty({ icon: Icon = Sparkles, title, children }) {
  return (
    <div className="empty">
      <div className="empty-icon"><Icon size={34} strokeWidth={1.6} /></div>
      <h3>{title}</h3>
      {children && <p>{children}</p>}
    </div>
  )
}

const BADGES = {
  LIBRE: ['Libre', 'green'], EN_ATTENTE: ['En attente', 'amber'], RESERVE: ['Reserve', 'pink'],
  VALIDEE: ['Validee', 'green'], REFUSEE: ['Refusee', 'red'],
  VALIDE: ['Valide', 'green'], UTILISE: ['Utilise', 'violet'], ANNULE: ['Annule', 'red'],
}
export function Badge({ status, children }) {
  const [label, color] = BADGES[status] || [children || status, 'violet']
  return <span className={`badge badge--${color}`}>{children || label}</span>
}

export function Modal({ title, onClose, children, wide = false }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal${wide ? ' modal--wide' : ''}`} role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-head">
          <h2>{title}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Fermer"><X size={18} /></button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function PageHeader({ eyebrow, title, children }) {
  return (
    <header className="page-header">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
      </div>
      {children && <div className="page-actions">{children}</div>}
    </header>
  )
}

// Formats d'affichage
export const fmtDate = (d) => d ? new Date(d + (d.length === 10 ? 'T00:00:00' : '')).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }) : '-'
export const fmtDateTime = (d) => d ? new Date(d).toLocaleString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '-'
export const fmtTime = (d) => d ? new Date(d).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) : '-'
export const fmtMoney = (n) => `${Number(n ?? 0).toLocaleString('fr-FR', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} TND`
