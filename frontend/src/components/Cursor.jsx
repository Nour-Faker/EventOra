import { useEffect, useRef } from 'react'

// Curseur anime : un point qui suit la souris et un anneau qui le rattrape en douceur.
// L'anneau grossit sur les elements cliquables, se contracte au clic et lance une onde.
// Desactive sur ecran tactile et si l'utilisateur prefere reduire les animations.
const CLICKABLE = 'a, button, [role="button"], select, label, .clickable, input[type="checkbox"], input[type="radio"]'
const TEXT = 'input:not([type="checkbox"]):not([type="radio"]):not([type="submit"]), textarea'

export default function Cursor() {
  const dot = useRef(null)
  const ring = useRef(null)
  const ripples = useRef(null)

  useEffect(() => {
    const fine = window.matchMedia('(pointer: fine)').matches
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (!fine || reduced) return undefined

    document.documentElement.classList.add('has-custom-cursor')
    let x = window.innerWidth / 2, y = window.innerHeight / 2
    let rx = x, ry = y
    let frame
    let visible = false

    const move = (e) => {
      x = e.clientX; y = e.clientY
      if (!visible) {
        visible = true; rx = x; ry = y
        dot.current.classList.add('is-visible'); ring.current.classList.add('is-visible')
      }
      const target = e.target instanceof Element ? e.target : null
      ring.current.classList.toggle('is-hover', !!target?.closest(CLICKABLE))
      const onText = !!target?.closest(TEXT)
      ring.current.classList.toggle('is-text', onText)
      dot.current.classList.toggle('is-text', onText)
    }
    const loop = () => {
      rx += (x - rx) * 0.18
      ry += (y - ry) * 0.18
      dot.current.style.transform = `translate3d(${x}px, ${y}px, 0)`
      ring.current.style.transform = `translate3d(${rx}px, ${ry}px, 0)`
      frame = requestAnimationFrame(loop)
    }
    const down = () => {
      ring.current.classList.add('is-down')
      const r = document.createElement('span')
      r.className = 'cursor-ripple'
      r.style.left = `${x}px`; r.style.top = `${y}px`
      ripples.current.appendChild(r)
      setTimeout(() => r.remove(), 650)
    }
    const up = () => ring.current.classList.remove('is-down')
    const leave = () => {
      visible = false
      dot.current.classList.remove('is-visible'); ring.current.classList.remove('is-visible')
    }

    window.addEventListener('mousemove', move)
    window.addEventListener('mousedown', down)
    window.addEventListener('mouseup', up)
    document.addEventListener('mouseleave', leave)
    frame = requestAnimationFrame(loop)
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('mousemove', move)
      window.removeEventListener('mousedown', down)
      window.removeEventListener('mouseup', up)
      document.removeEventListener('mouseleave', leave)
      document.documentElement.classList.remove('has-custom-cursor')
    }
  }, [])

  return (
    <div className="cursor-layer" aria-hidden="true">
      <div ref={ripples} />
      <div ref={ring} className="cursor-ring" />
      <div ref={dot} className="cursor-dot" />
    </div>
  )
}
