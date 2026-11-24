import { useState } from 'react'
import { api } from '../api'
import { ROLE_LABEL, useAuth } from '../auth'
import { useToast } from '../components/Toast'
import { PageHeader } from '../components/ui'

// Profil de l'utilisateur connecte : BF-03, BF-04
export default function Profile() {
  const { user, refresh } = useAuth()
  const toast = useToast()
  const [nom, setNom] = useState(user.nom)
  const [prenom, setPrenom] = useState(user.prenom)
  const [pw, setPw] = useState({ old: '', next: '', confirm: '' })
  const [busy, setBusy] = useState(null)

  const save = async (e) => {
    e.preventDefault()
    setBusy('p')
    try { const u = await api.updateMe(nom, prenom); refresh(u); toast.success('Profil mis a jour.') }
    catch (err) { toast.error(err.message) } finally { setBusy(null) }
  }

  const changePw = async (e) => {
    e.preventDefault()
    if (pw.next !== pw.confirm) { toast.error('Les deux nouveaux mots de passe ne correspondent pas.'); return }
    setBusy('pw')
    try {
      await api.changePassword(pw.old, pw.next)
      setPw({ old: '', next: '', confirm: '' })
      toast.success('Mot de passe modifie.')
    } catch (err) { toast.error(err.message) } finally { setBusy(null) }
  }

  return (
    <>
      <PageHeader eyebrow={ROLE_LABEL[user.role]} title="Mon profil" />
      <div className="grid grid--two">
        <form className="card form pop-in" onSubmit={save}>
          <div className="profile-head">
            <span className="avatar avatar--lg">{`${user.prenom?.[0] || ''}${user.nom?.[0] || ''}`.toUpperCase()}</span>
            <div><h2>{user.prenom} {user.nom}</h2><p className="muted">{user.email}</p></div>
          </div>
          <div className="row">
            <label className="field"><span>Prenom</span><input value={prenom} onChange={(e) => setPrenom(e.target.value)} required /></label>
            <label className="field"><span>Nom</span><input value={nom} onChange={(e) => setNom(e.target.value)} required /></label>
          </div>
          <button className="btn btn--primary" disabled={busy === 'p'}>{busy === 'p' ? <span className="spinner" /> : 'Enregistrer'}</button>
        </form>

        <form className="card form pop-in" style={{ '--i': 1 }} onSubmit={changePw}>
          <h2>Changer le mot de passe</h2>
          <label className="field"><span>Mot de passe actuel</span>
            <input type="password" value={pw.old} onChange={(e) => setPw({ ...pw, old: e.target.value })} autoComplete="current-password" required />
          </label>
          <label className="field"><span>Nouveau mot de passe</span>
            <input type="password" value={pw.next} onChange={(e) => setPw({ ...pw, next: e.target.value })} autoComplete="new-password" minLength={8} required />
          </label>
          <label className="field"><span>Confirmer</span>
            <input type="password" value={pw.confirm} onChange={(e) => setPw({ ...pw, confirm: e.target.value })} autoComplete="new-password" minLength={8} required />
          </label>
          <button className="btn btn--primary" disabled={busy === 'pw'}>{busy === 'pw' ? <span className="spinner" /> : 'Modifier'}</button>
        </form>
      </div>
    </>
  )
}
