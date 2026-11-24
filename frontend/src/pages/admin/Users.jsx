import { Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { api } from '../../api'
import { ROLE_LABEL, useAuth } from '../../auth'
import { useToast } from '../../components/Toast'
import { ErrorBox, Loader, Modal, PageHeader, useLoad } from '../../components/ui'

// Gestion des utilisateurs : BF-05 a BF-07 + creation d'un agent d'accueil
export default function Users() {
  const { user: me, emailActif } = useAuth()
  const toast = useToast()
  const { data, loading, error, reload } = useLoad(() => api.users(), [])
  const [q, setQ] = useState('')
  const [role, setRole] = useState('TOUS')
  const [creating, setCreating] = useState(false)

  const run = async (fn, ok) => {
    try { await fn(); toast.success(ok); reload() } catch (err) { toast.error(err.message) }
  }

  const list = (data || []).filter((u) =>
    (role === 'TOUS' || u.role === role) &&
    `${u.prenom} ${u.nom} ${u.email}`.toLowerCase().includes(q.toLowerCase()))

  return (
    <>
      <PageHeader eyebrow="Administration" title="Utilisateurs">
        <button className="btn btn--primary" onClick={() => setCreating(true)}><Plus size={16} /> Agent d'accueil</button>
      </PageHeader>
      <div className="toolbar">
        <input className="search" placeholder="Rechercher un nom ou un email..." value={q} onChange={(e) => setQ(e.target.value)} />
        <select className="select" value={role} onChange={(e) => setRole(e.target.value)}>
          <option value="TOUS">Tous les roles</option>
          {Object.entries(ROLE_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </div>
      {loading && <Loader />}
      {error && <ErrorBox error={error} onRetry={reload} />}
      {data && (
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Utilisateur</th><th>Email</th><th>Role</th><th>Etat</th><th /></tr></thead>
            <tbody>
              {list.map((u) => (
                <tr key={u.id} className={u.bloque ? 'is-blocked' : ''}>
                  <td><span className="avatar avatar--sm">{`${u.prenom?.[0] || ''}${u.nom?.[0] || ''}`.toUpperCase()}</span> {u.prenom} {u.nom}</td>
                  <td className="muted">{u.email}</td>
                  <td><span className={`role role--${u.role}`}>{ROLE_LABEL[u.role]}</span></td>
                  <td className="cell-badges">
                    {u.bloque ? <span className="badge badge--red">Bloque</span> : <span className="badge badge--green">Actif</span>}
                    {emailActif && !u.emailVerifie && <span className="badge badge--amber">Email non verifie</span>}
                  </td>
                  <td className="cell-actions">
                    {u.id !== me.id && (
                      <>
                        <button className="btn btn--ghost btn--sm"
                                onClick={() => run(() => api.blockUser(u.id, !u.bloque), u.bloque ? 'Compte debloque.' : 'Compte bloque.')}>
                          {u.bloque ? 'Debloquer' : 'Bloquer'}
                        </button>
                        <button className="btn btn--danger btn--sm"
                                onClick={() => window.confirm(`Supprimer definitivement ${u.prenom} ${u.nom} ?`) && run(() => api.deleteUser(u.id), 'Utilisateur supprime.')}><Trash2 size={15} /></button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {creating && <AgentForm onClose={() => setCreating(false)} onDone={() => { setCreating(false); reload() }} />}
    </>
  )
}

function AgentForm({ onClose, onDone }) {
  const toast = useToast()
  const [f, setF] = useState({ prenom: '', nom: '', email: '', telephone: '', matricule: '', motDePasse: '' })
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  const submit = async (e) => {
    e.preventDefault()
    try { await api.createAgent(f); toast.success("Agent d'accueil cree."); onDone() } catch (err) { toast.error(err.message) }
  }
  return (
    <Modal title="Nouvel agent d'accueil" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <div className="row">
          <label className="field"><span>Prenom</span><input value={f.prenom} onChange={set('prenom')} required /></label>
          <label className="field"><span>Nom</span><input value={f.nom} onChange={set('nom')} required /></label>
        </div>
        <div className="row">
          <label className="field"><span>Email</span><input type="email" value={f.email} onChange={set('email')} required /></label>
          <label className="field"><span>Matricule</span><input value={f.matricule} onChange={set('matricule')} placeholder="AG-002" required /></label>
        </div>
        <div className="row">
          <label className="field"><span>Telephone</span><input value={f.telephone} onChange={set('telephone')} /></label>
          <label className="field"><span>Mot de passe</span><input type="password" value={f.motDePasse} onChange={set('motDePasse')} minLength={8} autoComplete="new-password" required /></label>
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>Annuler</button>
          <button className="btn btn--primary">Creer l'agent</button>
        </div>
      </form>
    </Modal>
  )
}
