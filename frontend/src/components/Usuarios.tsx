import ListaUsuarios from './ListaUsuarios'
import ListLayout from './ListLayout'

export default function Usuarios() {
  return <ListLayout href="/usuarios/criar" children={<ListaUsuarios />} />
}
