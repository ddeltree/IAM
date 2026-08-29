import { Link } from 'react-router'
import useSWR from 'swr'
import { excluirUsuario, listarUsuarios } from '@/lib/api'
import { esquecer, lembrar, papelDoTipo } from '@/lib/conhecidos'
import { podeCriarUsuario, podeListarUsuarios } from '@/lib/permissoes'
import { useSessao } from '@/providers/SessaoProvider'
import TituloFrame from './TituloFrame'
import ErroApi, { SemPermissao } from './ErroApi'
import UsuarioAvatar from './UsuarioAvatar'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
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
import { useEffect } from 'react'

export default function Usuarios() {
  const { sessao } = useSessao()
  const podeListar = !!sessao && podeListarUsuarios(sessao)

  const { data, error, isLoading, mutate } = useSWR(
    podeListar ? [sessao!.id, 'usuarios'] : null,
    listarUsuarios,
  )

  // Aproveita a única listagem completa do sistema para alimentar o caderninho
  // de ids que os professores usam na hora de matricular.
  useEffect(() => {
    data?.forEach((u) =>
      lembrar({ id: u.id, name: u.name, papel: papelDoTipo(u.tipo) }),
    )
  }, [data])

  if (!sessao) return null

  if (!podeListar)
    return (
      <TituloFrame titulo="Usuários">
        <SemPermissao>
          Só o administrador pode listar os usuários do sistema.
        </SemPermissao>
        {podeCriarUsuario(sessao) && (
          <Link to="/usuarios/novo" className="mt-4 inline-block">
            <Button>Novo aluno</Button>
          </Link>
        )}
      </TituloFrame>
    )

  if (error != null) return <ErroApi erro={error} />

  return (
    <TituloFrame titulo="Usuários">
      {isLoading ? (
        <Skeleton className="h-24 w-full max-w-lg rounded-lg" />
      ) : data?.length === 0 ? (
        <p className="text-muted-foreground italic">
          Nenhum usuário cadastrado além do administrador.
        </p>
      ) : (
        <ul className="max-w-lg divide-y">
          {data?.map((u) => (
            <li key={u.id} className="flex items-center gap-3 py-2">
              <UsuarioAvatar nome={u.name} className="h-9 w-9" />
              <div className="flex flex-col leading-tight">
                <span className="font-medium">{u.name}</span>
                <span className="text-muted-foreground text-xs">
                  {u.tipo === 1 ? 'Professor' : 'Aluno'} · id #{u.id}
                </span>
              </div>
              <AlertDialog>
                <AlertDialogTrigger asChild>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-destructive ml-auto"
                  >
                    Excluir
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Excluir {u.name}?</AlertDialogTitle>
                    <AlertDialogDescription>
                      O usuário será removido dos grupos de permissão. Isso não
                      pode ser desfeito.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancelar</AlertDialogCancel>
                    <AlertDialogAction
                      onClick={async () => {
                        await excluirUsuario(u.id)
                        esquecer(u.id)
                        mutate()
                      }}
                    >
                      Excluir
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            </li>
          ))}
        </ul>
      )}

      <Separator className="my-4 max-w-lg" />
      <Link to="/usuarios/novo">
        <Button>Novo professor</Button>
      </Link>
    </TituloFrame>
  )
}
