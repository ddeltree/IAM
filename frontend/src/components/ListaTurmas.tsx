import { Link } from 'react-router'
import useSWR from 'swr'

export default function ListaTurmas() {
  const { data, error, isLoading } = useSWR('usuarios', listarTurmas)

  if (error) return <div>Erro ao listar turmas</div>
  if (isLoading) return <div>Carregando...</div>
  if (data?.length === 0) return <p>Turmas não encontradas</p>
  return (
    <ul>
      {data?.map((turma) => (
        <li key={turma.id}>
          <Link to={`/turmas/${turma.id}`}>{turma.nome}</Link>
        </li>
      ))}
    </ul>
  )
}

async function listarTurmas() {
  const response = await fetch('http://localhost:7000/turmas')
  const turmas = await response.json()
  return turmas as Record<string, any>[]
}
