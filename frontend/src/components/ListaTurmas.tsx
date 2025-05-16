import { Link } from 'react-router'
import useSWR from 'swr'
import { Skeleton } from '@/components/ui/skeleton'
import _ from 'lodash'

export default function ListaTurmas() {
  const { data, error, isLoading } = useSWR('listar-turmas', listarTurmas)

  if (error) return <div>Erro ao listar turmas</div>
  if (isLoading)
    return (
      <div className="flex w-full justify-start gap-4 p-2">
        {_.range(0, 3).map((i) => (
          <SkeletonCard key={i} />
        ))}
      </div>
    )
  if (data?.length === 0) return <p>Turmas não encontradas</p>
  return (
    <ul className="flex w-full justify-start gap-4 p-2">
      {data?.map((turma) => (
        <li
          key={turma.id}
          className="h-72 w-72 overflow-hidden rounded-lg border"
        >
          <Link to={`/turmas/${turma.id}`}>
            <div className="bg-primary text-primary-foreground flex h-32 flex-col justify-between p-6">
              <p className="text-xl font-semibold underline">{turma.nome}</p>
              <p className="font-normal">{turma.professorResponsavel.name}</p>
            </div>
          </Link>
        </li>
      ))}
    </ul>
  )
}

export function SkeletonCard() {
  return (
    <div className="flex h-[250px] flex-col space-y-3 rounded-xl border">
      <Skeleton className="h-[100px] w-[250px] rounded-xl" />
      <div className="m-4 space-y-2">
        <Skeleton className="h-4 w-[200px]" />
        <Skeleton className="h-4 w-[150px]" />
        <Skeleton className="h-4 w-[75px]" />
      </div>
    </div>
  )
}

async function listarTurmas() {
  const response = await fetch('http://localhost:7000/turmas')
  const turmas = await response.json()
  return turmas as Record<string, any>[]
}
