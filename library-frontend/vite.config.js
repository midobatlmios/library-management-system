import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // The backend's CORS allows exactly this origin, so keep the port fixed.
    port: 5173,
    strictPort: true,
  },
  build: {
    rolldownOptions: {
      output: {
        // Big libraries in their own files: they change less often than our code,
        // so after an app update the browser can keep them from its cache.
        codeSplitting: {
          groups: [
            { name: 'react', test: /node_modules[\\/](react|react-dom|react-router|react-router-dom|scheduler|react-redux|@reduxjs|redux|immer|reselect)[\\/]/ },
            { name: 'mui', test: /node_modules[\\/](@mui|@emotion|@popperjs|react-transition-group|stylis)[\\/]/ },
          ],
        },
      },
    },
  },
})
