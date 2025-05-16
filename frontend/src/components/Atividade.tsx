import { useParams } from 'react-router'
import useSWR from 'swr'
import AtividadeSkeletonCard from './AtividadeSkeletonCard'
import { ClipboardList } from 'lucide-react'
import { Separator } from '@/components/ui/separator'
import Comentarios from './Comentarios'
import { AlertaExclusao, atualizar, DialogDemo } from './Atividades'
export default function Atividade() {
  const params = useParams()
  const id = params['id']
  const {
    data: atividade,
    error,
    isLoading,
    mutate,
  } = useSWR(`atividades/${id}`, () => ver(id))
  if (error) return <div>Erro ao buscar atividade</div>
  if (isLoading)
    return (
      <div className="flex justify-center">
        <AtividadeSkeletonCard />
      </div>
    )
  if (!atividade) return <div>Atividade nao encontrada</div>

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
        <div className="w-full">
          <p className="text-left">{atividade.corpo}</p>
        </div>

        <div className="text-secondary-foreground flex items-baseline justify-between text-xs">
          <p>Data de entrega: {atividade.dataEntrega}</p>
          <div>
            <DialogDemo
              {...{
                confirmarDialog: 'Salvar mudanças',
                descricaoDialog: 'Dẽ uma nova descrição para a atividade',
                tituloDialog: 'Editar atividade',
                onConfirmar: async (titulo, corpo, turmaId, date) => {
                  if (!titulo.trim() || !corpo.trim() || !turmaId || !date)
                    return
                  await atualizar(atividade.id, titulo, corpo, date)
                  mutate()
                },
              }}
            />
            <AlertaExclusao idAtividade={atividade.id} onExcluir={mutate} />
          </div>
        </div>
        <Separator className="mt-1 mb-3" />

        <Comentarios postId={atividade.id} />
      </div>
    </div>
  )
}

async function ver(id: string | undefined) {
  if (!id) return
  const response = await fetch(`http://localhost:7000/atividades/${id}`)
  const atividade = await response.json()
  return atividade as Record<string, any>
}
