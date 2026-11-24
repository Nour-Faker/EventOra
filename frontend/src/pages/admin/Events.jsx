import { ArrowRight, CalendarDays, CalendarX, MapPin, Plus } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api'
import { useToast } from '../../components/Toast'
import { Empty, ErrorBox, Loader, Modal, PageHeader, fmtDate, useLoad } from '../../components/ui'

const EMPTY = { titre: '', description: '', lieu: '', dateDebut: '', dateFin: '' }

// Gestion des evenements : BF-20 a BF-22
export default function Events() {
  const toast = useToast()
  const { data, loading, error, reload } = useLoad(() => api.events(), [])
  const [editing, setEditing] = useState(null) // null | {} (nouveau) | evenement

  const remove = async (e) => {
    if (!window.confirm(`Supprimer « ${e.titre} » ? Ses stands, sessions et types de billets seront supprimes.`)) return
    try { await api.deleteEvent(e.id); toast.success('Evenement supprime.'); reload() }
    catch (err) { toast.error(err.message) }
  }

  return (
    <>
      <PageHeader eyebrow="Administration" title="Evenements">
        <button className="btn btn--primary" onClick={() => setEditing(EMPTY)}><Plus size={16} /> Nouvel evenement</button>
      </PageHeader>
      {loading && <Loader />}
      {error && <ErrorBox error={error} onRetry={reload} />}
      {data?.length === 0 && <Empty icon={CalendarX} title="Aucun evenement">Creez le premier salon ou festival.</Empty>}
      <div className="list">
        {data?.map((e, i) => (
          <div key={e.id} className="card list-row pop-in" style={{ '--i': i }}>
            <div>
              <h3>{e.titre}</h3>
              <p className="muted icon-line"><MapPin size={14} /> {e.lieu} · <CalendarDays size={14} /> {fmtDate(e.dateDebut)} <ArrowRight size={13} /> {fmtDate(e.dateFin)}</p>
            </div>
            <div className="row-actions">
              <Link to={`/admin/evenements/${e.id}`} className="btn btn--primary btn--sm">Gerer</Link>
              <button className="btn btn--ghost btn--sm" onClick={() => setEditing(e)}>Modifier</button>
              <button className="btn btn--danger btn--sm" onClick={() => remove(e)}>Supprimer</button>
            </div>
          </div>
        ))}
      </div>
      {editing && <EventForm event={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />}
    </>
  )
}

function EventForm({ event, onClose, onSaved }) {
  const toast = useToast()
  const [f, setF] = useState({ ...EMPTY, ...event, description: event.description || '' })
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  const isNew = !event.id

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    const body = { titre: f.titre, description: f.description, lieu: f.lieu, dateDebut: f.dateDebut, dateFin: f.dateFin }
    try {
      if (isNew) await api.createEvent(body); else await api.updateEvent(event.id, body)
      toast.success(isNew ? 'Evenement cree.' : 'Evenement modifie.')
      onSaved()
    } catch (err) { toast.error(err.message) } finally { setBusy(false) }
  }

  return (
    <Modal title={isNew ? 'Nouvel evenement' : 'Modifier l\'evenement'} onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label className="field"><span>Titre</span><input value={f.titre} onChange={set('titre')} required /></label>
        <label className="field"><span>Lieu</span><input value={f.lieu} onChange={set('lieu')} required /></label>
        <div className="row">
          <label className="field"><span>Debut</span><input type="date" value={f.dateDebut} onChange={set('dateDebut')} required /></label>
          <label className="field"><span>Fin</span><input type="date" value={f.dateFin} min={f.dateDebut} onChange={set('dateFin')} required /></label>
        </div>
        <label className="field"><span>Description</span><textarea rows={3} value={f.description} onChange={set('description')} /></label>
        <div className="modal-actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>Annuler</button>
          <button className="btn btn--primary" disabled={busy}>{busy ? <span className="spinner" /> : 'Enregistrer'}</button>
        </div>
      </form>
    </Modal>
  )
}
