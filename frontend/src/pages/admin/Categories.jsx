import { Plus, Tag } from 'lucide-react'
import { useState } from 'react'
import { api } from '../../api'
import { useToast } from '../../components/Toast'
import { Empty, ErrorBox, Loader, Modal, PageHeader, useLoad } from '../../components/ui'

// Categories de stands : BF-08 a BF-10
export default function Categories() {
  const toast = useToast()
  const { data, loading, error, reload } = useLoad(() => api.categories(), [])
  const [editing, setEditing] = useState(null)
  const [f, setF] = useState({ nom: '', description: '' })

  const open = (c) => { setEditing(c); setF({ nom: c.nom || '', description: c.description || '' }) }
  const save = async (e) => {
    e.preventDefault()
    try {
      if (editing.id) await api.updateCategory(editing.id, f); else await api.createCategory(f)
      toast.success(editing.id ? 'Categorie modifiee.' : 'Categorie creee.')
      setEditing(null); reload()
    } catch (err) { toast.error(err.message) }
  }
  const remove = async (c) => {
    if (!window.confirm(`Supprimer la categorie « ${c.nom} » ?`)) return
    try { await api.deleteCategory(c.id); toast.success('Categorie supprimee.'); reload() } catch (err) { toast.error(err.message) }
  }

  return (
    <>
      <PageHeader eyebrow="Administration" title="Categories de stands">
        <button className="btn btn--primary" onClick={() => open({})}><Plus size={16} /> Nouvelle categorie</button>
      </PageHeader>
      {loading && <Loader />}
      {error && <ErrorBox error={error} onRetry={reload} />}
      {data?.length === 0 && <Empty icon={Tag} title="Aucune categorie">Technologie, alimentation, artisanat...</Empty>}
      <div className="grid grid--cards">
        {data?.map((c, i) => (
          <div key={c.id} className="card category pop-in" style={{ '--i': i }}>
            <h3 className="icon-line"><Tag size={17} /> {c.nom}</h3>
            <p className="muted">{c.description || 'Pas de description'}</p>
            <div className="row-actions">
              <button className="btn btn--ghost btn--sm" onClick={() => open(c)}>Modifier</button>
              <button className="btn btn--danger btn--sm" onClick={() => remove(c)}>Supprimer</button>
            </div>
          </div>
        ))}
      </div>
      {editing && (
        <Modal title={editing.id ? 'Modifier la categorie' : 'Nouvelle categorie'} onClose={() => setEditing(null)}>
          <form className="form" onSubmit={save}>
            <label className="field"><span>Nom</span><input value={f.nom} onChange={(e) => setF({ ...f, nom: e.target.value })} required /></label>
            <label className="field"><span>Description</span><textarea rows={3} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></label>
            <div className="modal-actions">
              <button type="button" className="btn btn--ghost" onClick={() => setEditing(null)}>Annuler</button>
              <button className="btn btn--primary">Enregistrer</button>
            </div>
          </form>
        </Modal>
      )}
    </>
  )
}
