import { ArrowRight, Check, Inbox, X } from 'lucide-react'
import { useState } from 'react'
import { api } from '../../api'
import { useToast } from '../../components/Toast'
import { Badge, Empty, ErrorBox, Loader, Modal, PageHeader, fmtDateTime, fmtMoney, useLoad } from '../../components/ui'

// Traitement des demandes de stand : BF-17 a BF-19
export default function Requests() {
  const toast = useToast()
  const { data, loading, error, reload } = useLoad(() => api.allRequests(), [])
  const [filter, setFilter] = useState('EN_ATTENTE')
  const [refusing, setRefusing] = useState(null)
  const [motif, setMotif] = useState('')
  const [busy, setBusy] = useState(null)

  const decide = async (d, decision, reason) => {
    setBusy(d.id)
    try {
      await api.decide(d.id, decision, reason)
      toast.success(decision === 'VALIDEE'
        ? `Stand ${d.stand} reserve pour ${d.exposant}. Les autres demandes sur ce stand sont refusees.`
        : 'Demande refusee.')
      setRefusing(null); setMotif(''); reload()
    } catch (err) { toast.error(err.message) } finally { setBusy(null) }
  }

  const list = (data || []).filter((d) => filter === 'TOUTES' || d.statut === filter)
  const pending = (data || []).filter((d) => d.statut === 'EN_ATTENTE').length

  return (
    <>
      <PageHeader eyebrow="Administration" title="Demandes de stand" />
      <div className="filters">
        {[['EN_ATTENTE', `En attente`, pending], ['VALIDEE', 'Validees'], ['REFUSEE', 'Refusees'], ['TOUTES', 'Toutes']].map(([k, label, n]) => (
          <button key={k} className={`chip${filter === k ? ' is-on' : ''}`} onClick={() => setFilter(k)}>
            {label}{n !== undefined && <span>{n}</span>}
          </button>
        ))}
      </div>
      {loading && <Loader />}
      {error && <ErrorBox error={error} onRetry={reload} />}
      {data && list.length === 0 && <Empty icon={Inbox} title="Aucune demande ici" />}
      <div className="list">
        {list.map((d, i) => (
          <div key={d.id} className={`card list-row request request--${d.statut} pop-in`} style={{ '--i': i }}>
            <div>
              <h3 className="icon-line">{d.exposant} <span className="muted icon-line"><ArrowRight size={15} /> stand {d.stand}</span></h3>
              <p className="muted">{d.evenement} · {fmtMoney(d.prix)} · le {fmtDateTime(d.dateDemande)}</p>
              {d.motifRefus && <p className="refusal">Motif : {d.motifRefus}</p>}
            </div>
            {d.statut === 'EN_ATTENTE' ? (
              <div className="row-actions">
                <button className="btn btn--success btn--sm" disabled={busy === d.id} onClick={() => decide(d, 'VALIDEE')}><Check size={15} /> Valider</button>
                <button className="btn btn--danger btn--sm" disabled={busy === d.id} onClick={() => setRefusing(d)}><X size={15} /> Refuser</button>
              </div>
            ) : <Badge status={d.statut} />}
          </div>
        ))}
      </div>
      {refusing && (
        <Modal title={`Refuser la demande de ${refusing.exposant}`} onClose={() => setRefusing(null)}>
          <form className="form" onSubmit={(e) => { e.preventDefault(); decide(refusing, 'REFUSEE', motif) }}>
            <label className="field"><span>Motif du refus (obligatoire)</span>
              <textarea rows={3} value={motif} onChange={(e) => setMotif(e.target.value)} required autoFocus />
            </label>
            <div className="modal-actions">
              <button type="button" className="btn btn--ghost" onClick={() => setRefusing(null)}>Annuler</button>
              <button className="btn btn--danger" disabled={busy === refusing.id || !motif.trim()}>Refuser la demande</button>
            </div>
          </form>
        </Modal>
      )}
    </>
  )
}
