import { Route, Routes } from 'react-router'
import MainLayout from './components/layout/MainLayout'
import Home from './components/Home'
import Usuarios from './components/Usuarios'
import CriarTurma from './components/CriarTurma'
import CriarUsuario from './components/CriarUsuario'
import VerUsuario from './components/VerUsuario'
import TurmaLayout from './components/layout/TurmaLayout'
import ListaTurmas from './components/ListaTurmas'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<Home />} />
        <Route path="/usuarios" element={<Usuarios />}>
          <Route path="criar" element={<CriarUsuario />} />
          <Route path=":id" element={<VerUsuario />} />
        </Route>
        <Route path="/turmas">
          <Route index element={<ListaTurmas />} />
          <Route path="criar" element={<CriarTurma />} />
          <Route path=":id" element={<TurmaLayout />}>
            <Route index element={<p>mural</p>} />
            <Route path="atividades" element={<p>atividades</p>} />
            <Route path="pessoas" element={<p>pessoas</p>} />
          </Route>
        </Route>
      </Route>
    </Routes>
  )
}
