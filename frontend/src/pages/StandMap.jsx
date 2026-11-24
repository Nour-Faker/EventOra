import { CalendarX, Store } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../components/Toast'
import { Badge, Empty, ErrorBox, Loader, Modal, PageHeader, fmtMoney, useLoad } from '../components/ui'

// Plan des stands d'un evenement (exposant et administrateur) : BF-14, BF-15
export default function StandMap() {
  const { user } = useAuth()
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const events = useLoad(() => api.events(), [])
  const eventId = params.get('evenement')
  const stands = useLoad(() => (eventId ? api.stands(eventId) : Promise.resolve(null)), [eventId])
  const [selected, setSelected] = useState(null)
  const [filter, setFilter] = useState('TOUS')
  const [busy, setBusy] = useState(false)

  // Premier evenement a venir par defaut
  useEffect(() => {
    if (!eventId && events.data?.length) {
      const today = new Date().toISOString().slice(0, 10)
      const next = events.data.find((e) => e.dateFin >= today) || events.data[0]
      setParams({ evenement: String(next.id) }, { replace: true })
    }
  }, [eventId, events.data, setParams])

  const request = async () => {
    setBusy(true)
    try {
      await api.requestStand(selected.id)
      toast.success(`Demande envoyee pour le stand ${selected.numero}.`)
      setSelected(null); stands.reload()
    } catch (err) { toast.error(err.message) } finally { setBusy(false) }
  }

  const list = (stands.data || []).filter((s) => filter === 'TOUS' || s.statut === filter)
  const counts = (stands.data || []).reduce((acc, s) => ({ ...acc, [s.statut]: (acc[s.statut] || 0) + 1 }), {})

  return (
    <>
      <PageHeader eyebrow={user.role === 'EXPOSANT' ? 'Exposant' : 'Administration'} title="Plan des stands">
        <select className="select" value={eventId || ''} onChange={(e) => setParams({ evenement: e.target.value })}>
          {events.data?.map((e) => <option key={e.id} value={e.id}>{e.titre}</option>)}
        </select>
      </PageHeader>

      {events.data?.length === 0 && <Empty icon={CalendarX} title="Aucun evenement" />}
      {stands.loading && eventId && <Loader />}
      {stands.error && <ErrorBox error={stands.error} onRetry={stands.reload} />}

      {stands.data && (
        <>
          <div className="filters">
            {[['TOUS', 'Tous', stands.data.length], ['LIBRE', 'Libres', counts.LIBRE || 0],
              ['EN_ATTENTE', 'En attente', counts.EN_ATTENTE || 0], ['RESERVE', 'Reserves', counts.RESERVE || 0]].map(([k, label, n]) => (
              <button key={k} className={`chip chip--${k}${filter === k ? ' is-on' : ''}`} onClick={() => setFilter(k)}>
                {label} <span>{n}</span>
              </button>
            ))}
          </div>
          {stands.data.length === 0 && <Empty icon={Store} title="Aucun stand pour cet evenement" />}
          <div className="stand-grid">
            {list.map((s, i) => (
              <button key={s.id} className={`stand stand--${s.statut} pop-in`} style={{ '--i': i }} onClick={() => setSelected(s)}>
                <span className="stand-num">{s.numero}</span>
                <span className="stand-cat">{s.categorie}</span>
                <span className="stand-meta">{Number(s.superficie)} m² · {fmtMoney(s.prix)}</span>
                {s.exposant && <span className="stand-owner">{s.exposant}</span>}
              </button>
            ))}
          </div>
        </>
      )}

      {selected && (
        <Modal title={`Stand ${selected.numero}`} onClose={() => setSelected(null)}>
          <dl className="details">
            <dt>Statut</dt><dd><Badge status={selected.statut} /></dd>
            <dt>Evenement</dt><dd>{selected.evenement}</dd>
            <dt>Categorie</dt><dd>{selected.categorie}</dd>
            <dt>Emplacement</dt><dd>{selected.emplacement || '-'}</dd>
            <dt>Superficie</dt><dd>{Number(selected.superficie)} m²</dd>
            <dt>Prix</dt><dd className="price">{fmtMoney(selected.prix)}</dd>
            {selected.exposant && (<><dt>Exposant</dt><dd>{selected.exposant}</dd></>)}
          </dl>
          {user.role === 'EXPOSANT' && (
            <div className="modal-actions">
              <button className="btn btn--ghost" onClick={() => setSelected(null)}>Fermer</button>
              <button className="btn btn--primary" disabled={selected.statut === 'RESERVE' || busy} onClick={request}>
                {busy ? <span className="spinner" /> : selected.statut === 'RESERVE' ? 'Deja reserve' : 'Demander ce stand'}
              </button>
            </div>
          )}
        </Modal>
      )}
    </>
  )
}
