import { DoorOpen, Hourglass, Mic, RefreshCw, Store, Ticket } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api'
import { ROLE_LABEL } from '../../auth'
import { Empty, ErrorBox, Loader, PageHeader, fmtMoney, useLoad } from '../../components/ui'

// Chiffre anime de 0 a la valeur finale
function CountUp({ value, money = false }) {
  const [n, setN] = useState(0)
  useEffect(() => {
    const target = Number(value) || 0
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reduced) { setN(target); return undefined }
    const start = performance.now()
    let frame
    const step = (t) => {
      const p = Math.min((t - start) / 900, 1)
      setN(target * (1 - Math.pow(1 - p, 3)))
      if (p < 1) frame = requestAnimationFrame(step)
    }
    frame = requestAnimationFrame(step)
    return () => cancelAnimationFrame(frame)
  }, [value])
  return <>{money ? fmtMoney(Math.round(n)) : Math.round(n).toLocaleString('fr-FR')}</>
}

// Tableau de bord administrateur : BF-34
export default function Dashboard() {
  const { data: s, loading, error, reload } = useLoad(() => api.stats(), [])
  if (loading) return <Loader />
  if (error) return <ErrorBox error={error} onRetry={reload} />

  const kpis = [
    [Ticket, 'Billets vendus', s.billets, false, 'pink'],
    [DoorOpen, 'Visiteurs presents', s.presents, false, 'violet'],
    [Store, 'Stands reserves', s.standsReserves, false, 'amber'],
    [Hourglass, 'Demandes en attente', s.demandesEnAttente, false, 'coral', '/admin/demandes'],
  ]
  const roles = Object.entries(s.utilisateursParRole || {})
  const maxRole = Math.max(1, ...roles.map(([, n]) => n))
  const ca = Number(s.chiffreAffaires) || 0
  const pctBillets = ca > 0 ? (Number(s.chiffreAffairesBillets) / ca) * 100 : 0

  return (
    <>
      <PageHeader eyebrow="Administration" title="Tableau de bord">
        <button className="btn btn--ghost" onClick={reload}><RefreshCw size={15} /> Actualiser</button>
      </PageHeader>

      <div className="grid grid--kpi">
        {kpis.map(([Icon, label, value, money, color, to], i) => {
          const body = (
            <>
              <span className="kpi-icon"><Icon size={22} /></span>
              <span className="kpi-value"><CountUp value={value} money={money} /></span>
              <span className="kpi-label">{label}</span>
            </>
          )
          return to
            ? <Link key={label} to={to} className={`card kpi kpi--${color} pop-in`} style={{ '--i': i }}>{body}</Link>
            : <div key={label} className={`card kpi kpi--${color} pop-in`} style={{ '--i': i }}>{body}</div>
        })}
      </div>

      <div className="grid grid--two">
        <div className="card panel pop-in" style={{ '--i': 4 }}>
          <h2>Chiffre d'affaires</h2>
          <p className="big-number gradient-text"><CountUp value={ca} money /></p>
          <div className="split-bar" title="Repartition billets / stands">
            <span className="split-bar-a" style={{ width: `${pctBillets}%` }} />
          </div>
          <div className="legend">
            <span><i className="legend-dot legend-dot--a" /> Billets · {fmtMoney(s.chiffreAffairesBillets)}</span>
            <span><i className="legend-dot legend-dot--b" /> Stands · {fmtMoney(s.chiffreAffairesStands)}</span>
          </div>
        </div>

        <div className="card panel pop-in" style={{ '--i': 5 }}>
          <h2>Utilisateurs <span className="muted">· {s.utilisateurs}</span></h2>
          <div className="bars">
            {roles.map(([role, n]) => (
              <div key={role} className="bar-row">
                <span>{ROLE_LABEL[role] || role}</span>
                <div className="bar"><span style={{ width: `${(n / maxRole) * 100}%` }} /></div>
                <strong>{n}</strong>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="card panel pop-in" style={{ '--i': 6 }}>
        <h2>Remplissage des sessions</h2>
        {(s.remplissageSessions || []).length === 0 && <Empty icon={Mic} title="Aucune session programmee" />}
        <div className="bars">
          {(s.remplissageSessions || []).map((r) => (
            <div key={r.id} className="bar-row bar-row--wide">
              <span><strong>{r.titre}</strong><small className="muted"> · {r.evenement}</small></span>
              <div className="bar bar--fill"><span style={{ width: `${Math.min(r.taux, 100)}%` }} /></div>
              <strong>{r.inscrits}/{r.capacite} <small className="muted">({r.taux}%)</small></strong>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
