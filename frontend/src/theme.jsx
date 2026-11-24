import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { flushSync } from 'react-dom'
import { Moon, Sun } from 'lucide-react'

// Theme jour / nuit. Par defaut : nuit. Le choix est memorise dans le navigateur.
const KEY = 'eventora.theme'
const ThemeContext = createContext(null)

function initialTheme() {
  try { return localStorage.getItem(KEY) === 'light' ? 'light' : 'dark' } catch { return 'dark' }
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(initialTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'light' ? '#f6f4ff' : '#0b0a1f')
    try { localStorage.setItem(KEY, theme) } catch { /* stockage indisponible */ }
  }, [theme])

  // Bascule avec une revelation circulaire depuis le bouton (View Transitions), sinon un fondu
  const toggle = useCallback((event) => {
    const next = theme === 'dark' ? 'light' : 'dark'
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (!document.startViewTransition || reduced) {
      document.documentElement.classList.add('theme-fade')
      setTheme(next)
      setTimeout(() => document.documentElement.classList.remove('theme-fade'), 450)
      return
    }
    const x = event?.clientX ?? window.innerWidth - 40
    const y = event?.clientY ?? 40
    const radius = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y))
    const transition = document.startViewTransition(() => {
      flushSync(() => setTheme(next))
      document.documentElement.dataset.theme = next
    })
    transition.ready.then(() => {
      document.documentElement.animate(
        { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
        { duration: 650, easing: 'cubic-bezier(0.22, 1, 0.36, 1)', pseudoElement: '::view-transition-new(root)' },
      )
    }).catch(() => { /* transition annulee */ })
  }, [theme])

  const value = useMemo(() => ({ theme, toggle }), [theme, toggle])
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() { return useContext(ThemeContext) }

export function ThemeToggle() {
  const { theme, toggle } = useTheme()
  const dark = theme === 'dark'
  return (
    <button className={`theme-toggle${dark ? ' is-dark' : ''}`} onClick={toggle}
            aria-label={dark ? 'Passer en mode jour' : 'Passer en mode nuit'} title={dark ? 'Mode jour' : 'Mode nuit'}>
      <span className="theme-toggle-icon theme-toggle-icon--sun"><Sun size={18} /></span>
      <span className="theme-toggle-icon theme-toggle-icon--moon"><Moon size={18} /></span>
    </button>
  )
}
