import { Link } from 'react-router'
import useSWR from 'swr'
import { listarTurmas } from '@/lib/api'
import { podeCriarTurma } from '@/lib/permissoes'
import { useSessao } from '@/providers/SessaoProvider'
import type { Turma } from '@/lib/types'
import TurmaSkeletonCard from './TurmaSkeletonCard'
import ErroApi from './ErroApi'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'

export default function ListaTurmas() {
  const { sessao } = useSessao()
  const { data, error, isLoading } = useSWR(
    sessao ? [sessao.id, 'turmas'] : null,
    listarTurmas,
  )

  if (error != null) return <ErroApi erro={error} />
  if (isLoading || !sessao)
    return (
      <div className="flex w-full justify-start gap-4 p-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <TurmaSkeletonCard key={i} />
        ))}
      </div>
    )

  const vazio = {
    ADMIN: 'Nenhuma turma foi criada ainda.',
    PROFESSOR: 'Você ainda não criou nenhuma turma.',
    ALUNO: 'Você ainda não foi matriculado em nenhuma turma.',
  }[sessao.papel]

  return (
    <>
      {data?.length === 0 ? (
        <p className="text-muted-foreground italic">{vazio}</p>
      ) : (
        <ul className="flex w-full flex-wrap justify-start gap-4 p-2">
          {data?.map((turma) => <TurmaCard key={turma.id} turma={turma} />)}
        </ul>
      )}
      {podeCriarTurma(sessao) && (
        <>
          <Separator className="my-4" />
          <Link to="/turmas/nova">
            <Button>Nova turma</Button>
          </Link>
        </>
      )}
    </>
  )
}

function TurmaCard({ turma }: { turma: Turma }) {
  return (
    <li className="w-72 overflow-hidden rounded-lg border">
      <Link to={`/turmas/${turma.id}`}>
        <div className="bg-primary text-primary-foreground flex h-32 flex-col justify-between p-6">
          <p className="text-xl font-semibold underline">{turma.nome}</p>
          <p className="font-normal">{turma.professorResponsavel.name}</p>
        </div>
        <div className="text-muted-foreground flex justify-between px-6 py-3 text-xs">
          <span>
            {turma.alunos.length}{' '}
            {turma.alunos.length === 1 ? 'aluno' : 'alunos'}
          </span>
          <span>
            {turma.posts.length}{' '}
            {turma.posts.length === 1 ? 'publicação' : 'publicações'}
          </span>
        </div>
      </Link>
    </li>
  )
}
