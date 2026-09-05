import { Navigate, Route, Routes } from 'react-router'
import Shell from './components/layout/Shell'
import Simulador from './components/Simulador'
import Principais from './components/Principais'
import Politicas from './components/Politicas'
import Recursos from './components/Recursos'
import Consultas from './components/Consultas'
import Vocabulario from './components/Vocabulario'

export default function App() {
  return (
    <Routes>
      <Route element={<Shell />}>
        <Route index element={<Navigate to="/simulador" replace />} />
        <Route path="simulador" element={<Simulador />} />
        <Route path="principais" element={<Principais />} />
        <Route path="politicas" element={<Politicas />} />
        <Route path="recursos" element={<Recursos />} />
        <Route path="consultas" element={<Consultas />} />
        <Route path="vocabulario" element={<Vocabulario />} />
        <Route path="*" element={<Navigate to="/simulador" replace />} />
      </Route>
    </Routes>
  )
}
