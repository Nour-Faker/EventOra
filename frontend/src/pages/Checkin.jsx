import { Check, ScanLine, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { api } from '../api'
import { PageHeader, fmtDateTime } from '../components/ui'

// Controle des entrees (agent d'accueil) : BF-33.
// Un lecteur QR USB "tape" le code puis Entree : le champ reste toujours actif.
export default function Checkin() {
  const [code, setCode] = useState('')
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [history, setHistory] = useState([])
  const input = useRef(null)

  useEffect(() => { input.current?.focus() }, [result])

  const scan = async (e) => {
    e.preventDefault()
    const value = code.trim()
    if (!value) return
    setBusy(true)
    let r
    try {
      const info = await api.checkin(value)
      r = { ok: true, code: value, info }
    } catch (err) {
      r = { ok: false, code: value, message: err.message }
    }
    setResult({ ...r, at: Date.now() })
    setHistory((h) => [{ ...r, at: new Date() }, ...h].slice(0, 8))
    setCode(''); setBusy(false)
  }

  return (
    <>
      <PageHeader eyebrow="Agent d'accueil" title="Controle des entrees" />
      <div className="checkin">
        <form className="card checkin-form" onSubmit={scan}>
          <div className="scan-visual" aria-hidden="true"><span className="scan-line" /></div>
          <label className="field">
            <span>Code du billet</span>
            <input ref={input} className="input-xl" value={code} onChange={(e) => setCode(e.target.value.toUpperCase())}
                   placeholder="Scannez ou saisissez le code" autoComplete="off" spellCheck={false} />
          </label>
          <button className="btn btn--primary btn--block btn--lg" disabled={busy || !code.trim()}>
            {busy ? <span className="spinner" /> : 'Valider le billet'}
          </button>
        </form>

        <div className="checkin-result">
          {!result && (
            <div className="card result result--idle">
              <div className="result-icon"><ScanLine size={40} strokeWidth={1.6} /></div>
              <h2>En attente d'un billet</h2>
              <p className="muted">Le resultat du scan s'affichera ici.</p>
            </div>
          )}
          {result && (
            <div key={result.at} className={`card result ${result.ok ? 'result--ok' : 'result--ko'}`}>
              <div className="result-icon">{result.ok ? <Check size={46} strokeWidth={3} /> : <X size={46} strokeWidth={3} />}</div>
              <h2>{result.ok ? 'Acces autorise' : 'Acces refuse'}</h2>
              {result.ok ? (
                <dl className="details">
                  <dt>Visiteur</dt><dd>{result.info.visiteur}</dd>
                  <dt>Evenement</dt><dd>{result.info.titre}</dd>
                  <dt>Billet</dt><dd>{result.info.typeBillet}</dd>
                </dl>
              ) : <p>{result.message}</p>}
              <p className="muted result-code">{result.code}</p>
            </div>
          )}
        </div>
      </div>

      {history.length > 0 && (
        <section className="section">
          <div className="section-head"><h2>Derniers scans</h2></div>
          <div className="list">
            {history.map((h, i) => (
              <div key={i} className="card list-row list-row--compact">
                <span className={`dot ${h.ok ? 'dot--ok' : 'dot--ko'}`} />
                <strong>{h.code}</strong>
                <span className="muted">{h.ok ? h.info.visiteur : h.message}</span>
                <span className="muted">{fmtDateTime(h.at)}</span>
              </div>
            ))}
          </div>
        </section>
      )}
    </>
  )
}
