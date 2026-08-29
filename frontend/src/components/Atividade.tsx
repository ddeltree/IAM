import { useNavigate, useParams } from 'react-router'
import useSWR, { useSWRConfig } from 'swr'
import { ClipboardList } from 'lucide-react'
import { atualizarAtividade, excluirAtividade, verAtividade } from '@/lib/api'
import { podeEditarAtividade, podeExcluirAtividade } from '@/lib/permissoes'
import { useSessao } from '@/providers/SessaoProvider'
import { useTurma } from './layout/TurmaLayout'
import { AlertaExclusao, AtividadeDialog } from './AtividadeDialog'
import { formatarEntrega } from './Atividades'
import AtividadeSkeletonCard from './AtividadeSkeletonCard'
import Comentarios from './Comentarios'
import ErroApi from './ErroApi'
import { Separator } from '@/components/ui/separator'

export default function Atividade() {
  const { atividadeId } = useParams()
  const { turma } = useTurma()
  const { sessao } = useSessao()
  const { mutate: mutateGlobal } = useSWRConfig()
  const navigate = useNavigate()

  const {
    data: atividade,
    error,
    isLoading,
    mutate,
  } = useSWR(
    sessao && atividadeId ? [sessao.id, 'atividade', atividadeId] : null,
    () => verAtividade(atividadeId!),
  )

  if (!sessao) return null
  if (error != null) return <ErroApi erro={error} />
  if (isLoading)
    return (
      <div className="flex justify-center">
        <AtividadeSkeletonCard />
      </div>
    )
  if (!atividade) return <p className="italic">Atividade não encontrada</p>

  return (
    <div className="flex w-full flex-col items-center">
      <div className="w-full max-w-2xl">
        <div className="flex items-center gap-4 px-4 py-2">
          <div className="bg-accent text-accent-foreground flex h-10 w-10 items-center justify-center rounded-full">
            <ClipboardList size={32} />
          </div>
          <h2 className="text-2xl font-semibold">{atividade.titulo}</h2>
        </div>
        <Separator className="mt-1 mb-3" />
        <p className="text-left whitespace-pre-wrap">{atividade.corpo}</p>

        <div className="text-secondary-foreground mt-3 flex items-baseline justify-between text-xs">
          <p>Entrega: {formatarEntrega(atividade.dataEntrega)}</p>
          <div className="flex gap-2">
            {podeEditarAtividade(sessao, turma) && (
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
            {podeExcluirAtividade(sessao, turma) && (
              <AlertaExclusao
                onExcluir={async () => {
                  await excluirAtividade(atividade.id)
                  await mutateGlobal([sessao.id, 'atividades', turma.id])
                  navigate(`/turmas/${turma.id}/atividades`, { replace: true })
                }}
              />
            )}
          </div>
        </div>
        <Separator className="mt-1 mb-3" />

        <Comentarios turma={turma} tipo="atividades" pubId={atividade.id} />
      </div>
    </div>
  )
}
