import { ArrowRight, Check, Clock, Map as MapIcon, MapPin, Mic, Ticket, Users } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../components/Toast'
import { Empty, ErrorBox, Loader, fmtDate, fmtMoney, fmtTime, useLoad } from '../components/ui'
import { DateTile } from './Home'

export default function EventDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const isVisitor = user?.role === 'VISITEUR'

  const ev = useLoad(() => api.event(id), [id])
  const types = useLoad(() => api.ticketTypes(id), [id])
  const sessions = useLoad(() => api.sessions(id), [id])
  const mine = useLoad(() => (isVisitor ? Promise.all([api.myTickets(), api.mySessions()]) : Promise.resolve([[], []])), [isVisitor])
  const [busy, setBusy] = useState(null)

  if (ev.loading) return <Loader />
  if (ev.error) return <ErrorBox error={ev.error} />
  const e = ev.data
  const [myTickets, mySessions] = mine.data || [[], []]
  const myTicket = myTickets.find((t) => t.titre === e.titre && t.statut !== 'ANNULE')
  const joined = new Set(mySessions.map((s) => s.id))
  const finished = e.dateFin < new Date().toISOString().slice(0, 10)

  const action = async (key, fn, ok) => {
    if (!user) { navigate('/connexion', { state: { from: `/evenements/${id}` } }); return }
    setBusy(key)
    try { await fn(); toast.success(ok); types.reload(); sessions.reload(); mine.reload() }
    catch (err) { toast.error(err.message) }
    finally { setBusy(null) }
  }

  return (
    <>
      <section className="event-hero card">
        <DateTile date={e.dateDebut} />
        <div className="event-hero-text">
          <p className="eyebrow">{finished ? 'Evenement termine' : 'Evenement'}</p>
          <h1>{e.titre}</h1>
          <p className="muted icon-line"><MapPin size={15} /> {e.lieu} · du {fmtDate(e.dateDebut)} au {fmtDate(e.dateFin)}</p>
          {e.description && <p className="event-hero-desc">{e.description}</p>}
        </div>
        {(user?.role === 'EXPOSANT' || user?.role === 'ADMINISTRATEUR') && (
          <Link to={`/stands?evenement=${e.id}`} className="btn btn--ghost"><MapIcon size={16} /> Plan des stands</Link>
        )}
      </section>

      <section className="section">
        <div className="section-head"><h2>Billetterie</h2></div>
        {myTicket && (
          <div className="notice notice--success">
            <Ticket size={18} /> <span>Vous avez un billet <strong>{myTicket.typeBillet}</strong> pour cet evenement.</span>
            <Link to="/mon-espace" className="link-btn icon-line"> Voir mon billet <ArrowRight size={15} /></Link>
          </div>
        )}
        {types.loading && <Loader />}
        {types.error && <ErrorBox error={types.error} onRetry={types.reload} />}
        {types.data?.length === 0 && <Empty icon={Ticket} title="Billetterie bientot ouverte" />}
        <div className="grid grid--tickets">
          {types.data?.map((t, i) => {
            const soldOut = t.restants <= 0
            const ratio = t.quota > 0 ? (t.quota - t.restants) / t.quota : 0
            return (
              <div key={t.id} className="card ticket-type pop-in" style={{ '--i': i }}>
                <div className="ticket-type-head">
                  <h3>{t.nom}</h3>
                  <span className="price">{fmtMoney(t.prix)}</span>
                </div>
                <div className="meter"><span style={{ width: `${Math.round(ratio * 100)}%` }} /></div>
                <p className="muted">{soldOut ? 'Complet' : `${t.restants} place${t.restants > 1 ? 's' : ''} restante${t.restants > 1 ? 's' : ''}`}</p>
                {(!user || isVisitor) && (
                  <button className="btn btn--primary btn--block" disabled={soldOut || finished || !!myTicket || busy === `t${t.id}`}
                          onClick={() => action(`t${t.id}`, () => api.buyTicket(t.id), 'Billet reserve ! Retrouvez-le dans Mon espace.')}>
                    {busy === `t${t.id}` ? <span className="spinner" /> : myTicket ? 'Deja reserve' : soldOut ? 'Complet' : 'Reserver'}
                  </button>
                )}
              </div>
            )
          })}
        </div>
      </section>

      <section className="section">
        <div className="section-head"><h2>Programme</h2></div>
        {sessions.loading && <Loader />}
        {sessions.error && <ErrorBox error={sessions.error} onRetry={sessions.reload} />}
        {sessions.data?.length === 0 && <Empty icon={Mic} title="Programme a venir" />}
        <ol className="timeline">
          {sessions.data?.map((s, i) => {
            const isIn = joined.has(s.id)
            return (
              <li key={s.id} className="timeline-item pop-in" style={{ '--i': i }}>
                <div className="timeline-time">
                  <strong>{fmtTime(s.heureDebut)}</strong>
                  <small>{new Date(s.heureDebut).toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short' })}</small>
                </div>
                <div className="card timeline-card">
                  <div>
                    <h3>{s.titre}</h3>
                    {s.description && <p className="muted">{s.description}</p>}
                    <p className="muted icon-line"><Clock size={14} /> {fmtTime(s.heureDebut)} – {fmtTime(s.heureFin)} · <Users size={14} /> {s.placesRestantes}/{s.capaciteMax} places</p>
                  </div>
                  {(!user || isVisitor) && (
                    isIn ? (
                      <button className="btn btn--ghost btn--sm" disabled={busy === `s${s.id}`}
                              onClick={() => action(`s${s.id}`, () => api.leaveSession(s.id), 'Desinscription enregistree.')}>
                        <Check size={15} /> Inscrit · Annuler
                      </button>
                    ) : (
                      <button className="btn btn--primary btn--sm" disabled={s.placesRestantes <= 0 || finished || busy === `s${s.id}`}
                              title={!myTicket && isVisitor ? 'Un billet pour cet evenement est necessaire' : undefined}
                              onClick={() => action(`s${s.id}`, () => api.joinSession(s.id), 'Inscription confirmee !')}>
                        {s.placesRestantes <= 0 ? 'Complet' : "S'inscrire"}
                      </button>
                    )
                  )}
                </div>
              </li>
            )
          })}
        </ol>
      </section>
    </>
  )
}
