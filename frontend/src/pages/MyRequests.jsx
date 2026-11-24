import { Plus, Store } from 'lucide-react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { Badge, Empty, ErrorBox, Loader, PageHeader, fmtDateTime, fmtMoney, useLoad } from '../components/ui'

// Suivi des demandes de stand de l'exposant : BF-16
export default function MyRequests() {
  const { data, loading, error, reload } = useLoad(() => api.myRequests(), [])
  return (
    <>
      <PageHeader eyebrow="Exposant" title="Mes demandes de stand">
        <Link to="/stands" className="btn btn--primary"><Plus size={16} /> Nouvelle demande</Link>
      </PageHeader>
      {loading && <Loader />}
      {error && <ErrorBox error={error} onRetry={reload} />}
      {data?.length === 0 && (
        <Empty icon={Store} title="Aucune demande">Choisissez un stand libre sur le plan pour envoyer votre premiere demande.</Empty>
      )}
      <div className="list">
        {data?.map((d, i) => (
          <div key={d.id} className={`card list-row request request--${d.statut} pop-in`} style={{ '--i': i }}>
            <div>
              <h3>Stand {d.stand} · {d.evenement}</h3>
              <p className="muted">Envoyee le {fmtDateTime(d.dateDemande)} · {fmtMoney(d.prix)}</p>
              {d.statut === 'REFUSEE' && d.motifRefus && <p className="refusal">Motif : {d.motifRefus}</p>}
            </div>
            <Badge status={d.statut} />
          </div>
        ))}
      </div>
    </>
  )
}
