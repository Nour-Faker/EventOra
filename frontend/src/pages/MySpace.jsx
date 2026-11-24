import { CalendarDays, Download, MapPin, Mic, Ticket, Users } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { useToast } from '../components/Toast'
import { Badge, Empty, ErrorBox, Loader, PageHeader, fmtDate, fmtDateTime, fmtMoney, useLoad } from '../components/ui'

// Espace visiteur : billets (PDF, annulation) et sessions auxquelles il est inscrit
export default function MySpace() {
  const toast = useToast()
  const tickets = useLoad(() => api.myTickets(), [])
  const sessions = useLoad(() => api.mySessions(), [])
  const [busy, setBusy] = useState(null)

  const download = async (t) => {
    setBusy(`pdf${t.id}`)
    try {
      const blob = await api.ticketPdf(t.id)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = `billet-${t.codeQr}.pdf`
      document.body.appendChild(a); a.click(); a.remove()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (err) { toast.error(err.message) } finally { setBusy(null) }
  }

  const cancel = async (t) => {
    if (!window.confirm(`Annuler le billet ${t.typeBillet} pour « ${t.titre} » ?`)) return
    setBusy(`c${t.id}`)
    try { await api.cancelTicket(t.id); toast.success('Billet annule.'); tickets.reload() }
    catch (err) { toast.error(err.message) } finally { setBusy(null) }
  }

  const leave = async (s) => {
    setBusy(`s${s.id}`)
    try { await api.leaveSession(s.id); toast.success('Desinscription enregistree.'); sessions.reload() }
    catch (err) { toast.error(err.message) } finally { setBusy(null) }
  }

  return (
    <>
      <PageHeader eyebrow="Visiteur" title="Mon espace">
        <Link to="/" className="btn btn--ghost">Parcourir les evenements</Link>
      </PageHeader>

      <section className="section">
        <div className="section-head"><h2>Mes billets</h2></div>
        {tickets.loading && <Loader />}
        {tickets.error && <ErrorBox error={tickets.error} onRetry={tickets.reload} />}
        {tickets.data?.length === 0 && (
          <Empty icon={Ticket} title="Aucun billet pour le moment">
            Choisissez un evenement et reservez votre place en un clic.
          </Empty>
        )}
        <div className="grid grid--tickets-wide">
          {tickets.data?.map((t, i) => (
            <article key={t.id} className={`ticket pop-in${t.statut !== 'VALIDE' ? ' is-used' : ''}`} style={{ '--i': i }}>
              <div className="ticket-main">
                <div className="ticket-top"><Badge status={t.statut} /><span className="ticket-type">{t.typeBillet}</span></div>
                <h3>{t.titre}</h3>
                <p className="muted icon-line"><span className="icon-pair"><MapPin size={14} /> {t.lieu}</span> · <span className="icon-pair"><CalendarDays size={14} /> {fmtDate(t.dateDebut)}</span></p>
                <p className="ticket-price">{fmtMoney(t.prixPaye)}</p>
              </div>
              <div className="ticket-stub">
                <span className="ticket-code">{t.codeQr}</span>
                <button className="btn btn--primary btn--sm" disabled={busy === `pdf${t.id}` || t.statut === 'ANNULE'} onClick={() => download(t)}>
                  {busy === `pdf${t.id}` ? <span className="spinner" /> : <><Download size={15} /> PDF</>}
                </button>
                {t.statut === 'VALIDE' && (
                  <button className="link-btn link-btn--danger" disabled={busy === `c${t.id}`} onClick={() => cancel(t)}>Annuler</button>
                )}
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="section-head"><h2>Mes sessions</h2></div>
        {sessions.loading && <Loader />}
        {sessions.error && <ErrorBox error={sessions.error} onRetry={sessions.reload} />}
        {sessions.data?.length === 0 && (
          <Empty icon={Mic} title="Aucune session">Inscrivez-vous aux conferences depuis la page d'un evenement.</Empty>
        )}
        <div className="list">
          {sessions.data?.map((s, i) => (
            <div key={s.id} className="card list-row pop-in" style={{ '--i': i }}>
              <div>
                <h3>{s.titre}</h3>
                <p className="muted icon-line"><span className="icon-pair"><CalendarDays size={14} /> {fmtDateTime(s.heureDebut)}</span> · <span className="icon-pair"><Users size={14} /> {s.placesRestantes}/{s.capaciteMax} places</span></p>
              </div>
              <div className="row-actions">
                <Link to={`/evenements/${s.evenementId}`} className="btn btn--ghost btn--sm">Voir l'evenement</Link>
                <button className="btn btn--ghost btn--sm" disabled={busy === `s${s.id}`} onClick={() => leave(s)}>Se desinscrire</button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </>
  )
}
