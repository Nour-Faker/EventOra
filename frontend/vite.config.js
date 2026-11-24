import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Le backend n'autorise (CORS) que http://localhost:5173 : on fixe ce port.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
})
