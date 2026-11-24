import { Check, CircleAlert, Info } from 'lucide-react'
import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const nextId = useRef(1)

  const remove = useCallback((id) => {
    setToasts((t) => t.map((x) => (x.id === id ? { ...x, leaving: true } : x)))
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 300)
  }, [])

  const push = useCallback((type, message) => {
    const id = nextId.current++
    setToasts((t) => [...t, { id, type, message }])
    setTimeout(() => remove(id), type === 'error' ? 5500 : 3500)
  }, [remove])

  const api = useMemo(() => ({
    success: (m) => push('success', m),
    error: (m) => push('error', m),
    info: (m) => push('info', m),
  }), [push])

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {toasts.map((t) => (
          <button key={t.id} className={`toast toast--${t.type}${t.leaving ? ' toast--leaving' : ''}`}
                  onClick={() => remove(t.id)}>
            <span className="toast-icon">{t.type === 'success' ? <Check size={15} strokeWidth={3} /> : t.type === 'error' ? <CircleAlert size={16} strokeWidth={2.5} /> : <Info size={16} strokeWidth={2.5} />}</span>
            <span>{t.message}</span>
          </button>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() { return useContext(ToastContext) }
