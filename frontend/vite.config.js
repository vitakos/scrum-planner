import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      // Dev-only: forwards same-origin /api calls to the backend, so the frontend
      // (and tools like Claude's browser pane) never make a cross-origin request.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  preview: {
    host: true,
    port: 4173
  }
});
