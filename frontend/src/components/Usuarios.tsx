import useSWR from 'swr'
import { useUser } from '../providers/UserProvider'
import TituloFrame from './TituloFrame'
import { Button } from './ui/button'
import { Link } from 'react-router'

export default function ListaUsuarios() {
  const { data, error, isLoading } = useSWR('usuarios', listUsers)
  const { setUser } = useUser()

  if (error) return <div>Erro ao listar usuários</div>
  if (isLoading) return <div>Carregando...</div>
  if (data?.length === 0) return <p>Usuários não encontrados</p>
  return (
    <TituloFrame titulo="Selecionar usuário">
      <ul>
        {data?.map((user) => (
          <li key={user.id} onClick={() => setUser(user)}>
            {user.name}
          </li>
        ))}
      </ul>
      <Link to="/usuarios/criar">
        <Button>Novo</Button>
      </Link>
    </TituloFrame>
  )
}

async function listUsers() {
  const response = await fetch('http://localhost:7000/usuarios')
  const users = await response.json()
  return users as Record<string, any>[]
}
