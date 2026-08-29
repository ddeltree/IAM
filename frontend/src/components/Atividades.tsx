import { Link } from 'react-router'
import useSWR from 'swr'
import { format, parseISO } from 'date-fns'
import { ClipboardList, Expand } from 'lucide-react'
import {
  atualizarAtividade,
  criarAtividade,
  excluirAtividade,
  listarAtividades,
} from '@/lib/api'
import { usePermissoes } from '@/hooks/usePermissoes'
import { useSessao } from '@/providers/SessaoProvider'
import { useTurma } from './layout/TurmaLayout'
import { AlertaExclusao, AtividadeDialog } from './AtividadeDialog'
import AtividadeSkeletonCard from './AtividadeSkeletonCard'
import ErroApi from './ErroApi'
import { Separator } from '@/components/ui/separator'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'

export function formatarEntrega(data: string | null) {
  if (!data) return 'sem data'
  try {
    return format(parseISO(data), 'dd/MM/yyyy')
  } catch {
    return data
  }
}

export default function Atividades() {
  const { turma } = useTurma()
  const { sessao } = useSessao()

  // A rota /atividades filtra por permissão em silêncio, então esta lista
  // nunca dá 403 — vem só o que o usuário pode ver.
  const { data, error, isLoading, mutate } = useSWR(
    sessao ? [sessao.id, 'atividades', turma.id] : null,
    () => listarAtividades(turma.id),
  )

  const { pode } = usePermissoes([
    `TURMA/${turma.id}` as const,
    ...(data ?? []).map((a) => `ATIVIDADE/${a.id}` as const),
  ])

  if (!sessao) return null
  if (error != null) return <ErroApi erro={error} />
  if (isLoading)
    return (
      <div className="flex justify-center">
        <div className="w-full max-w-2xl space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <AtividadeSkeletonCard key={i} />
          ))}
        </div>
      </div>
    )

  return (
    <div className="flex justify-center">
      <div className="w-full max-w-2xl space-y-2">
        {data?.length === 0 && (
          <p className="italic">Nenhuma atividade encontrada</p>
        )}
        {data?.map((atividade) => (
          <Collapsible key={atividade.id} className="rounded-xl border-b">
            <CollapsibleTrigger className="flex w-full items-center gap-2 px-4 py-2">
              <div className="bg-accent text-accent-foreground flex h-10 w-10 items-center justify-center rounded-full">
                <ClipboardList size={24} />
              </div>
              <h2 className="text-lg font-semibold">{atividade.titulo}</h2>
              <Link className="ml-auto" to={atividade.id}>
                <Expand className="text-muted-foreground" size={18} />
              </Link>
            </CollapsibleTrigger>

            <CollapsibleContent className="space-y-2 px-4 pb-2 pl-8 text-left">
              <p className="text-accent-foreground whitespace-pre-wrap">
                {atividade.corpo}
              </p>
              <Separator />
              <div className="text-secondary-foreground flex items-baseline justify-between text-xs">
                <p>Entrega: {formatarEntrega(atividade.dataEntrega)}</p>
                <div className="flex gap-2">
                  {pode('EDITAR_ATIVIDADE', `ATIVIDADE/${atividade.id}`) && (
                    <AtividadeDialog
                      rotulo="Editar"
                      titulo="Editar atividade"
                      descricao="Dê uma nova descrição para a atividade"
                      confirmar="Salvar mudanças"
                      valorInicial={{
                        titulo: atividade.titulo,
                        corpo: atividade.corpo,
                        dataEntrega: atividade.dataEntrega ?? '',
                      }}
                      onConfirmar={async (dados) => {
                        await atualizarAtividade(atividade.id, dados)
                        mutate()
                      }}
                    />
                  )}
                  {pode('EXCLUIR_ATIVIDADE', `ATIVIDADE/${atividade.id}`) && (
                    <AlertaExclusao
                      onExcluir={async () => {
                        await excluirAtividade(atividade.id)
                        mutate()
                      }}
                    />
                  )}
                </div>
              </div>
            </CollapsibleContent>
          </Collapsible>
        ))}

        {pode('CRIAR_ATIVIDADE', `TURMA/${turma.id}`) && (
          <div className="mt-4 flex justify-end">
            <AtividadeDialog
              rotulo="Criar atividade"
              titulo="Criar atividade"
              descricao="Descreva a atividade"
              confirmar="Criar"
              variante="default"
              onConfirmar={async (dados) => {
                await criarAtividade({ ...dados, turmaId: turma.id })
                mutate()
              }}
            />
          </div>
        )}
      </div>
    </div>
  )
}
