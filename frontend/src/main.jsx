import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './auth'
import { ToastProvider } from './components/Toast'
import Cursor from './components/Cursor'
import { ThemeProvider } from './theme'
import './styles.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <ThemeProvider>
      <ToastProvider>
        <AuthProvider>
          <App />
          <Cursor />
        </AuthProvider>
      </ToastProvider>
      </ThemeProvider>
    </BrowserRouter>
  </StrictMode>,
)
