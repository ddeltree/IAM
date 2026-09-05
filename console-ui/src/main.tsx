import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { SWRConfig } from 'swr'
import './index.css'
import App from './App'
import { CenarioProvider } from './providers/CenarioProvider'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SWRConfig value={{ revalidateOnFocus: false, shouldRetryOnError: false }}>
      <BrowserRouter>
        <CenarioProvider>
          <App />
        </CenarioProvider>
      </BrowserRouter>
    </SWRConfig>
  </StrictMode>,
)
