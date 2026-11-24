import { ArrowLeft, ArrowRight, CalendarDays, Mic, Plus, Store, Ticket, Trash2, Users } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../api'
import { useToast } from '../../components/Toast'
import { Badge, Empty, ErrorBox, Loader, Modal, PageHeader, fmtDate, fmtDateTime, fmtMoney, useLoad } from '../../components/ui'

// Gestion d'un evenement : stands (BF-11 a 13), types de billets (BF-28 a 30), sessions (BF-23 a 25)
export default function EventManage() {
  const { id } = useParams()
  const ev = useLoad(() => api.event(id), [id])
  const [tab, setTab] = useState('stands')

  if (ev.loading) return <Loader />
  if (ev.error) return <ErrorBox error={ev.error} />
  const e = ev.data
  const tabs = [['stands', Store, 'Stands'], ['billets', Ticket, 'Types de billets'], ['sessions', Mic, 'Sessions']]

  return (
    <>
      <PageHeader eyebrow={`${e.lieu} · ${fmtDate(e.dateDebut)} – ${fmtDate(e.dateFin)}`} title={e.titre}>
        <Link to="/admin/evenements" className="btn btn--ghost"><ArrowLeft size={16} /> Evenements</Link>
        <Link to={`/evenements/${e.id}`} className="btn btn--ghost">Page publique</Link>
      </PageHeader>
      <div className="tabs" role="tablist">
        {tabs.map(([k, Icon, label]) => (
          <button key={k} role="tab" aria-selected={tab === k} className={`tab${tab === k ? ' is-on' : ''}`} onClick={() => setTab(k)}><Icon size={16} /> {label}</button>
        ))}
      </div>
      <div key={tab} className="page-enter">
        {tab === 'stands' && <StandsTab event={e} />}
        {tab === 'billets' && <TicketTypesTab event={e} />}
        {tab === 'sessions' && <SessionsTab event={e} />}
      </div>
    </>
  )
}

// Petite aide : action avec message de succes / erreur
function useAction(reload) {
  const toast = useToast()
  return async (fn, ok) => {
    try { await fn(); toast.success(ok); reload(); return true } catch (err) { toast.error(err.message); return false }
  }
}

function StandsTab({ event }) {
  const stands = useLoad(() => api.stands(event.id), [event.id])
  const cats = useLoad(() => api.categories(), [])
  const run = useAction(stands.reload)
  const [editing, setEditing] = useState(null)

  return (
    <section className="section">
      <div className="section-head">
        <h2>Stands</h2>
        <button className="btn btn--primary btn--sm" disabled={!cats.data?.length} onClick={() => setEditing({})}><Plus size={15} /> Ajouter un stand</button>
      </div>
      {cats.data?.length === 0 && <div className="notice">Creez d'abord une categorie dans <Link to="/admin/categories" className="link-btn">Categories</Link>.</div>}
      {stands.loading && <Loader />}
      {stands.error && <ErrorBox error={stands.error} onRetry={stands.reload} />}
      {stands.data?.length === 0 && <Empty icon={Store} title="Aucun stand" />}
      <div className="table-wrap">
        {stands.data?.length > 0 && (
          <table className="table">
            <thead><tr><th>N°</th><th>Categorie</th><th>Emplacement</th><th>Surface</th><th>Prix</th><th>Statut</th><th>Exposant</th><th /></tr></thead>
            <tbody>
              {stands.data.map((s) => (
                <tr key={s.id}>
                  <td><strong>{s.numero}</strong></td><td>{s.categorie}</td><td>{s.emplacement || '-'}</td>
                  <td>{Number(s.superficie)} m²</td><td>{fmtMoney(s.prix)}</td><td><Badge status={s.statut} /></td>
                  <td>{s.exposant || '-'}</td>
                  <td className="cell-actions">
                    <button className="btn btn--ghost btn--sm" onClick={() => setEditing(s)}>Modifier</button>
                    <button className="btn btn--danger btn--sm" disabled={s.statut !== 'LIBRE'}
                            title={s.statut !== 'LIBRE' ? 'Seul un stand libre peut etre supprime' : undefined}
                            onClick={() => window.confirm(`Supprimer le stand ${s.numero} ?`) && run(() => api.deleteStand(s.id), 'Stand supprime.')}><Trash2 size={15} /></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      {editing && (
        <StandForm stand={editing} categories={cats.data || []} onClose={() => setEditing(null)}
                   onSubmit={async (body) => {
                     const ok = await run(() => (editing.id ? api.updateStand(editing.id, body) : api.createStand(event.id, body)),
                       editing.id ? 'Stand modifie.' : 'Stand ajoute.')
                     if (ok) setEditing(null)
                   }} />
      )}
    </section>
  )
}

function StandForm({ stand, categories, onClose, onSubmit }) {
  const [f, setF] = useState({
    numero: stand.numero || '', emplacement: stand.emplacement || '',
    superficie: stand.superficie ?? '', prix: stand.prix ?? '',
    categorieId: stand.categorieId || categories[0]?.id || '',
  })
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  return (
    <Modal title={stand.id ? `Modifier le stand ${stand.numero}` : 'Nouveau stand'} onClose={onClose}>
      <form className="form" onSubmit={(e) => { e.preventDefault(); onSubmit({ ...f, categorieId: Number(f.categorieId), superficie: Number(f.superficie), prix: Number(f.prix) }) }}>
        <div className="row">
          <label className="field"><span>Numero</span><input value={f.numero} onChange={set('numero')} placeholder="A1" required /></label>
          <label className="field"><span>Categorie</span>
            <select className="select" value={f.categorieId} onChange={set('categorieId')} required>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.nom}</option>)}
            </select>
          </label>
        </div>
        <label className="field"><span>Emplacement</span><input value={f.emplacement} onChange={set('emplacement')} placeholder="Hall A, allee 2" /></label>
        <div className="row">
          <label className="field"><span>Superficie (m²)</span><input type="number" min="0.5" step="0.5" value={f.superficie} onChange={set('superficie')} required /></label>
          <label className="field"><span>Prix (TND)</span><input type="number" min="0" step="0.01" value={f.prix} onChange={set('prix')} required /></label>
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>Annuler</button>
          <button className="btn btn--primary">Enregistrer</button>
        </div>
      </form>
    </Modal>
  )
}

function TicketTypesTab({ event }) {
  const types = useLoad(() => api.ticketTypes(event.id), [event.id])
  const run = useAction(types.reload)
  const [editing, setEditing] = useState(null)

  return (
    <section className="section">
      <div className="section-head">
        <h2>Types de billets</h2>
        <button className="btn btn--primary btn--sm" onClick={() => setEditing({})}><Plus size={15} /> Ajouter un type</button>
      </div>
      {types.loading && <Loader />}
      {types.error && <ErrorBox error={types.error} onRetry={types.reload} />}
      {types.data?.length === 0 && <Empty icon={Ticket} title="Aucun type de billet" />}
      <div className="grid grid--tickets">
        {types.data?.map((t, i) => (
          <div key={t.id} className="card ticket-type pop-in" style={{ '--i': i }}>
            <div className="ticket-type-head"><h3>{t.nom}</h3><span className="price">{fmtMoney(t.prix)}</span></div>
            <div className="meter"><span style={{ width: `${t.quota ? ((t.quota - t.restants) / t.quota) * 100 : 0}%` }} /></div>
            <p className="muted">{t.quota - t.restants} vendus / {t.quota}</p>
            <div className="row-actions">
              <button className="btn btn--ghost btn--sm" onClick={() => setEditing(t)}>Modifier</button>
              <button className="btn btn--danger btn--sm"
                      onClick={() => window.confirm(`Supprimer le type « ${t.nom} » ?`) && run(() => api.deleteTicketType(t.id), 'Type supprime.')}>Supprimer</button>
            </div>
          </div>
        ))}
      </div>
      {editing && (
        <TicketTypeForm type={editing} onClose={() => setEditing(null)}
                        onSubmit={async (body) => {
                          const ok = await run(() => (editing.id ? api.updateTicketType(editing.id, body) : api.createTicketType(event.id, body)),
                            editing.id ? 'Type modifie.' : 'Type ajoute.')
                          if (ok) setEditing(null)
                        }} />
      )}
    </section>
  )
}

function TicketTypeForm({ type, onClose, onSubmit }) {
  const [f, setF] = useState({ nom: type.nom || '', prix: type.prix ?? '', quota: type.quota ?? '' })
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  return (
    <Modal title={type.id ? `Modifier « ${type.nom} »` : 'Nouveau type de billet'} onClose={onClose}>
      <form className="form" onSubmit={(e) => { e.preventDefault(); onSubmit({ nom: f.nom, prix: Number(f.prix), quota: Number(f.quota) }) }}>
        <label className="field"><span>Nom</span><input value={f.nom} onChange={set('nom')} placeholder="Standard, VIP..." required /></label>
        <div className="row">
          <label className="field"><span>Prix (TND)</span><input type="number" min="0" step="0.01" value={f.prix} onChange={set('prix')} required /></label>
          <label className="field"><span>Quota</span><input type="number" min="1" step="1" value={f.quota} onChange={set('quota')} required /></label>
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>Annuler</button>
          <button className="btn btn--primary">Enregistrer</button>
        </div>
      </form>
    </Modal>
  )
}

function SessionsTab({ event }) {
  const sessions = useLoad(() => api.sessions(event.id), [event.id])
  const run = useAction(sessions.reload)
  const [editing, setEditing] = useState(null)

  return (
    <section className="section">
      <div className="section-head">
        <h2>Sessions du programme</h2>
        <button className="btn btn--primary btn--sm" onClick={() => setEditing({})}><Plus size={15} /> Ajouter une session</button>
      </div>
      {sessions.loading && <Loader />}
      {sessions.error && <ErrorBox error={sessions.error} onRetry={sessions.reload} />}
      {sessions.data?.length === 0 && <Empty icon={Mic} title="Aucune session" />}
      <div className="list">
        {sessions.data?.map((s, i) => (
          <div key={s.id} className="card list-row pop-in" style={{ '--i': i }}>
            <div>
              <h3>{s.titre}</h3>
              <p className="muted icon-line"><CalendarDays size={14} /> {fmtDateTime(s.heureDebut)} <ArrowRight size={13} /> {fmtDateTime(s.heureFin)} · <Users size={14} /> {s.capaciteMax - s.placesRestantes}/{s.capaciteMax} inscrits</p>
            </div>
            <div className="row-actions">
              <button className="btn btn--ghost btn--sm" onClick={() => setEditing(s)}>Modifier</button>
              <button className="btn btn--danger btn--sm"
                      onClick={() => window.confirm(`Supprimer la session « ${s.titre} » ?`) && run(() => api.deleteSession(s.id), 'Session supprimee.')}>Supprimer</button>
            </div>
          </div>
        ))}
      </div>
      {editing && (
        <SessionForm session={editing} event={event} onClose={() => setEditing(null)}
                     onSubmit={async (body) => {
                       const ok = await run(() => (editing.id ? api.updateSession(editing.id, body) : api.createSession(event.id, body)),
                         editing.id ? 'Session modifiee.' : 'Session ajoutee.')
                       if (ok) setEditing(null)
                     }} />
      )}
    </section>
  )
}

// "2027-03-10T10:00" (input datetime-local) -> "2027-03-10T10:00:00" (format attendu par le backend)
const toApi = (v) => (v && v.length === 16 ? `${v}:00` : v)
const toInput = (v) => (v ? v.slice(0, 16) : '')

function SessionForm({ session, event, onClose, onSubmit }) {
  const [f, setF] = useState({
    titre: session.titre || '', description: session.description || '',
    heureDebut: toInput(session.heureDebut) || `${event.dateDebut}T10:00`,
    heureFin: toInput(session.heureFin) || `${event.dateDebut}T11:00`,
    capaciteMax: session.capaciteMax ?? 50,
  })
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  return (
    <Modal title={session.id ? `Modifier « ${session.titre} »` : 'Nouvelle session'} onClose={onClose}>
      <form className="form" onSubmit={(e) => {
        e.preventDefault()
        onSubmit({ titre: f.titre, description: f.description, heureDebut: toApi(f.heureDebut), heureFin: toApi(f.heureFin), capaciteMax: Number(f.capaciteMax) })
      }}>
        <label className="field"><span>Titre</span><input value={f.titre} onChange={set('titre')} placeholder="Conference, atelier..." required /></label>
        <label className="field"><span>Description</span><textarea rows={2} value={f.description} onChange={set('description')} /></label>
        <div className="row">
          <label className="field"><span>Debut</span>
            <input type="datetime-local" value={f.heureDebut} min={`${event.dateDebut}T00:00`} max={`${event.dateFin}T23:59`} onChange={set('heureDebut')} required />
          </label>
          <label className="field"><span>Fin</span>
            <input type="datetime-local" value={f.heureFin} min={f.heureDebut} max={`${event.dateFin}T23:59`} onChange={set('heureFin')} required />
          </label>
        </div>
        <label className="field"><span>Capacite</span><input type="number" min="1" value={f.capaciteMax} onChange={set('capaciteMax')} required /></label>
        <div className="modal-actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>Annuler</button>
          <button className="btn btn--primary">Enregistrer</button>
        </div>
      </form>
    </Modal>
  )
}
