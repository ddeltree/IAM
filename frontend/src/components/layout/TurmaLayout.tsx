import {
  NavLink,
  Outlet,
  useNavigate,
  useOutletContext,
  useParams,
} from 'react-router'
import useSWR from 'swr'
import { useState } from 'react'
import { cn } from '@/lib/utils'
import { excluirTurma, renomearTurma, verTurma } from '@/lib/api'
import { usePermissoes } from '@/hooks/usePermissoes'
import { useSessao } from '@/providers/SessaoProvider'
import type { Turma } from '@/lib/types'
import ErroApi from '@/components/ErroApi'
import { Skeleton } from '@/components/ui/skeleton'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'

export type TurmaContexto = {
  turma: Turma
  recarregarTurma: () => void
}

/** Acesso tipado ao que o TurmaLayout passa para as páginas filhas. */
export const useTurma = () => useOutletContext<TurmaContexto>()

const abas = [
  { rotulo: 'Mural', para: '.', fim: true },
  { rotulo: 'Atividades', para: 'atividades', fim: false },
  { rotulo: 'Pessoas', para: 'pessoas', fim: false },
]

export default function TurmaLayout() {
  const { turmaId } = useParams()
  const { sessao } = useSessao()
  const { pode } = usePermissoes(turmaId ? [`TURMA/${turmaId}`] : [])
  const navigate = useNavigate()
  const [renomeando, setRenomeando] = useState(false)
  const [novoNome, setNovoNome] = useState('')

  const {
    data: turma,
    error,
    isLoading,
    mutate,
  } = useSWR(sessao && turmaId ? [sessao.id, 'turma', turmaId] : null, () =>
    verTurma(turmaId!),
  )

  const secondaryHeaderHeight = '48px'
  const estilo = {
    '--secondary-header-height': secondaryHeaderHeight,
    paddingTop: secondaryHeaderHeight,
  } as React.CSSProperties

  return (
    <div style={estilo} className="w-full">
      <div
        className={cn(
          'fixed flex items-center gap-6 border-b px-8',
          'left-[calc(var(--margin)+var(--sidebar-width))]',
          'right-[var(--margin)]',
          'top-[calc(var(--header-height)+var(--margin))]',
          'h-[var(--secondary-header-height)]',
          'bg-background text-base shadow-xs',
        )}
      >
        {abas.map((aba) => (
          <NavLink
            key={aba.rotulo}
            to={aba.para}
            end={aba.fim}
            className={({ isActive }) =>
              cn(
                'text-sm font-semibold',
                isActive
                  ? 'underline underline-offset-8'
                  : 'text-muted-foreground',
              )
            }
          >
            {aba.rotulo}
          </NavLink>
        ))}

        {turma && sessao && (
          <div className="ml-auto flex items-center gap-2">
            {pode('EDITAR_TURMA', `TURMA/${turma.id}`) &&
              (renomeando ? (
                <>
                  <Input
                    className="h-8 w-48"
                    value={novoNome}
                    onChange={(e) => setNovoNome(e.target.value)}
                  />
                  <Button
                    size="sm"
                    onClick={async () => {
                      if (!novoNome.trim()) return
                      await renomearTurma(turma.id, novoNome.trim())
                      setRenomeando(false)
                      mutate()
                    }}
                  >
                    Salvar
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setRenomeando(false)}
                  >
                    Cancelar
                  </Button>
                </>
              ) : (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    setNovoNome(turma.nome)
                    setRenomeando(true)
                  }}
                >
                  Renomear
                </Button>
              ))}

            {pode('EXCLUIR_TURMA', `TURMA/${turma.id}`) && !renomeando && (
              <AlertDialog>
                <AlertDialogTrigger asChild>
                  <Button size="sm" variant="destructive">
                    Excluir turma
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Excluir a turma?</AlertDialogTitle>
                    <AlertDialogDescription>
                      Os posts e as atividades da turma serão excluídos junto.
                      Isso não pode ser desfeito.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancelar</AlertDialogCancel>
                    <AlertDialogAction
                      onClick={async () => {
                        await excluirTurma(turma.id)
                        navigate('/', { replace: true })
                      }}
                    >
                      Excluir
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            )}
          </div>
        )}
      </div>

      <div className="pt-6">
        {error != null && <ErroApi erro={error} />}
        {isLoading && <Skeleton className="h-48 w-full rounded-lg" />}
        {turma && (
          <Outlet
            context={
              { turma, recarregarTurma: () => mutate() } satisfies TurmaContexto
            }
          />
        )}
      </div>
    </div>
  )
}
