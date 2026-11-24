import { ArrowRight, CalendarX, Map as MapIcon, MapPin, Mic, Store, Ticket } from 'lucide-react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { HOME_BY_ROLE, useAuth } from '../auth'
import { LogoMark } from '../components/Logo'
import { Empty, ErrorBox, Loader, useLoad } from '../components/ui'

export function DateTile({ date }) {
  const d = new Date(date + 'T00:00:00')
  return (
    <div className="date-tile">
      <span className="date-tile-day">{d.getDate()}</span>
      <span className="date-tile-month">{d.toLocaleDateString('fr-FR', { month: 'short' })}</span>
    </div>
  )
}

export default function Home() {
  const { user } = useAuth()
  const { data: events, loading, error, reload } = useLoad(() => api.events(), [])
  const today = new Date().toISOString().slice(0, 10)
  const upcoming = (events || []).filter((e) => e.dateFin >= today)
  const past = (events || []).filter((e) => e.dateFin < today)

  return (
    <>
      <section className="hero">
        <div className="hero-text">
          <p className="eyebrow">Salons · Festivals · Expositions</p>
          <h1 className="hero-title">
            Vivez vos evenements <span className="gradient-text">sans file d'attente</span>
          </h1>
          <p className="hero-sub">
            Billets avec code QR, programme des conferences, plan des stands en temps reel :
            EventOra reunit visiteurs, exposants et organisateurs sur une seule plateforme.
          </p>
          <div className="hero-actions">
            {user ? (
              <Link to={HOME_BY_ROLE[user.role]} className="btn btn--primary btn--lg">Mon espace <ArrowRight size={18} /></Link>
            ) : (
              <>
                <Link to="/inscription" className="btn btn--primary btn--lg">Creer un compte</Link>
                <Link to="/connexion" className="btn btn--ghost btn--lg">Se connecter</Link>
              </>
            )}
          </div>
          <ul className="hero-features">
            <li><Ticket size={16} /> Billets PDF avec QR</li>
            <li><MapIcon size={16} /> Plan des stands</li>
            <li><Mic size={16} /> Programme et sessions</li>
          </ul>
        </div>
        <div className="hero-visual" aria-hidden="true">
          <div className="orbit orbit--1"><span><Ticket size={18} /></span></div>
          <div className="orbit orbit--2"><span><Mic size={18} /></span></div>
          <div className="orbit orbit--3"><span><Store size={18} /></span></div>
          <div className="hero-logo"><LogoMark size={150} animated /></div>
        </div>
      </section>

      <section className="section">
        <div className="section-head">
          <h2>Evenements a venir</h2>
          {events && <span className="pill">{upcoming.length}</span>}
        </div>
        {loading && <Loader />}
        {error && <ErrorBox error={error} onRetry={reload} />}
        {events && upcoming.length === 0 && (
          <Empty icon={CalendarX} title="Aucun evenement a venir">Revenez bientot : le programme se prepare.</Empty>
        )}
        <div className="grid grid--cards">
          {upcoming.map((e, i) => <EventCard key={e.id} event={e} index={i} />)}
        </div>
        {past.length > 0 && (
          <>
            <div className="section-head section-head--sub"><h3>Evenements passes</h3></div>
            <div className="grid grid--cards">
              {past.map((e, i) => <EventCard key={e.id} event={e} index={i} past />)}
            </div>
          </>
        )}
      </section>
    </>
  )
}

function EventCard({ event, index, past }) {
  return (
    <Link to={`/evenements/${event.id}`} className={`card event-card pop-in${past ? ' is-past' : ''}`}
          style={{ '--i': index }}>
      <div className="event-card-top">
        <DateTile date={event.dateDebut} />
        <div>
          <h3>{event.titre}</h3>
          <p className="muted icon-line icon-line--single"><MapPin size={14} /> <span>{event.lieu}</span></p>
        </div>
      </div>
      {event.description && <p className="event-card-desc">{event.description}</p>}
      <div className="event-card-foot">
        <span className="muted">
          Du {new Date(event.dateDebut + 'T00:00:00').toLocaleDateString('fr-FR')} au {new Date(event.dateFin + 'T00:00:00').toLocaleDateString('fr-FR')}
        </span>
        <span className="event-card-arrow"><ArrowRight size={16} /></span>
      </div>
    </Link>
  )
}
