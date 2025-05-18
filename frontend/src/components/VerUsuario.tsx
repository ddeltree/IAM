import { useParams } from 'react-router'
import useSWR from 'swr'

export default function VerUsuario() {
  const params = useParams()
  const id = params['id']
  if (!id) return <div>Usuário não encontrado</div>
  const { data, error, isLoading } = useSWR(`usuarios/${id}`, () =>
    fetchUser(id),
  )

  if (error) return <div>Erro ao buscar usuário</div>
  if (isLoading) return <div>Carregando...</div>
  if (!data) return <div>Usuário nao encontrado</div>
  return <div>{JSON.stringify(data, null, 4)}</div>
}

async function fetchUser(id: string) {
  const response = await fetch(`http://localhost:7000/usuarios/${id}`)
  const user = await response.json()
  return user as Record<string, any>
}
