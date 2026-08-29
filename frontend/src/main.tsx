import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { BrowserRouter } from 'react-router'
import { SWRConfig } from 'swr'
import { SessaoProvider } from './providers/SessaoProvider.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      {/* 403 é resposta normal aqui; sem isto o SWR ficaria repetindo a
          requisição negada em backoff exponencial. */}
      <SWRConfig
        value={{ revalidateOnFocus: false, shouldRetryOnError: false }}
      >
        <SessaoProvider>
          <App />
        </SessaoProvider>
      </SWRConfig>
    </BrowserRouter>
  </StrictMode>,
)
