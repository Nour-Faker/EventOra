import { useId } from 'react'

// Logo EventOra : un pavillon d'exposition (toit) au-dessus de trois allees de stands
// qui dessinent aussi la lettre E.
export function LogoMark({ size = 40, animated = false }) {
  const id = useId()
  return (
    <svg className={animated ? 'logo-mark logo-mark--animated' : 'logo-mark'}
         width={size} height={size} viewBox="0 0 64 64" role="img" aria-label="EventOra">
      <defs>
        <linearGradient id={`g${id}`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#ff7a59" />
          <stop offset="0.55" stopColor="#ff4d8d" />
          <stop offset="1" stopColor="#7c5cff" />
        </linearGradient>
      </defs>
      <rect width="64" height="64" rx="16" fill={`url(#g${id})`} />
      <path className="logo-roof" d="M12 26 L32 12 L52 26" fill="none" stroke="#fff"
            strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" />
      <rect className="logo-bar logo-bar--1" x="17" y="31" width="30" height="5" rx="2.5" fill="#fff" />
      <rect className="logo-bar logo-bar--2" x="17" y="40" width="22" height="5" rx="2.5" fill="#fff" opacity=".9" />
      <rect className="logo-bar logo-bar--3" x="17" y="49" width="30" height="5" rx="2.5" fill="#fff" />
    </svg>
  )
}

export default function Logo({ size = 36, animated = false, tagline = false }) {
  return (
    <span className="logo">
      <LogoMark size={size} animated={animated} />
      <span className="logo-text">
        <span className="logo-word">Event<span className="gradient-text">Ora</span></span>
        {tagline && <span className="logo-tagline">Salons &amp; festivals</span>}
      </span>
    </span>
  )
}
