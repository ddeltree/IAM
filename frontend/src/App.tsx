import { Route, Routes } from 'react-router'
import MainLayout from './components/layout/MainLayout'
import TurmaLayout from './components/layout/TurmaLayout'
import RotaProtegida from './components/RotaProtegida'
import Login from './components/Login'
import ListaTurmas from './components/ListaTurmas'
import CriarTurma from './components/CriarTurma'
import Usuarios from './components/Usuarios'
import CriarUsuario from './components/CriarUsuario'
import Perfil from './components/Perfil'
import Mural from './components/Mural'
import Atividades from './components/Atividades'
import Atividade from './components/Atividade'
import Pessoas from './components/Pessoas'
import NaoEncontrado from './components/NaoEncontrado'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<RotaProtegida />}>
        <Route element={<MainLayout />}>
          <Route index element={<ListaTurmas />} />
          <Route path="usuarios">
            <Route index element={<Usuarios />} />
            <Route path="novo" element={<CriarUsuario />} />
            <Route path=":usuarioId" element={<Perfil />} />
          </Route>
          <Route path="turmas">
            <Route path="nova" element={<CriarTurma />} />
            {/* o parâmetro precisa de nome próprio: com dois `:id` aninhados o
                roteador guarda só o último e o id da turma se perde */}
            <Route path=":turmaId" element={<TurmaLayout />}>
              <Route index element={<Mural />} />
              <Route path="atividades" element={<Atividades />} />
              <Route path="atividades/:atividadeId" element={<Atividade />} />
              <Route path="pessoas" element={<Pessoas />} />
            </Route>
          </Route>
          <Route path="*" element={<NaoEncontrado />} />
        </Route>
      </Route>
    </Routes>
  )
}
