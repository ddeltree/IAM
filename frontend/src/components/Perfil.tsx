import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import useSWR from 'swr'
import { excluirUsuario, renomearUsuario, verUsuario } from '@/lib/api'
import { esquecer, lembrar, papelDoTipo } from '@/lib/conhecidos'
import { usePermissoes } from '@/hooks/usePermissoes'
import { useSessao } from '@/providers/SessaoProvider'
import TituloFrame from './TituloFrame'
import ErroApi, { SemPermissao } from './ErroApi'
import UsuarioAvatar from './UsuarioAvatar'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
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

export default function Perfil() {
  const { usuarioId } = useParams()
  const { sessao, sair } = useSessao()
  const navigate = useNavigate()
  const [novoNome, setNovoNome] = useState('')
  const [editando, setEditando] = useState(false)
  const [erroAcao, setErroAcao] = useState<unknown>(null)

  const { pode } = usePermissoes(
    usuarioId ? [`USUARIO/${usuarioId}` as const] : [],
  )
  // O backend só deixa cada um ver o próprio perfil, e o ADMIN nem isso —
  // então nem chegamos a pedir quando já se sabe que a resposta é 403.
  const permitido = !!usuarioId && pode('VER_PERFIL', `USUARIO/${usuarioId}`)

  const { data, error, isLoading, mutate } = useSWR(
    permitido ? [sessao!.id, 'usuario', usuarioId] : null,
    () => verUsuario(usuarioId!),
  )

  if (!sessao || !usuarioId) return null

  if (sessao.papel === 'ADMIN')
    return (
      <TituloFrame titulo="Perfil">
        <SemPermissao>
          O ADMIN não possui perfil: ele é criado junto com o sistema e não
          recebe a permissão VER_PERFIL.
        </SemPermissao>
      </TituloFrame>
    )

  if (usuarioId !== sessao.id)
    return (
      <TituloFrame titulo="Perfil">
        <SemPermissao>Você só pode ver o seu próprio perfil.</SemPermissao>
      </TituloFrame>
    )

  if (error != null) return <ErroApi erro={error} />
  if (isLoading || !data)
    return <Skeleton className="h-32 w-full max-w-md rounded-lg" />

  const papel = papelDoTipo(data.tipo)

  return (
    <TituloFrame titulo="Meu perfil">
      <div className="max-w-md space-y-4">
        <div className="flex items-center gap-4">
          <UsuarioAvatar nome={data.name} className="h-16 w-16 text-lg" />
          <div>
            {editando ? (
              <div className="flex gap-2">
                <Input
                  value={novoNome}
                  onChange={(e) => setNovoNome(e.target.value)}
                />
                <Button
                  size="sm"
                  onClick={async () => {
                    if (!novoNome.trim()) return
                    setErroAcao(null)
                    try {
                      const atualizado = await renomearUsuario(
                        data.id,
                        novoNome.trim(),
                      )
                      lembrar({
                        id: atualizado.id,
                        name: atualizado.name,
                        papel,
                      })
                      setEditando(false)
                      mutate()
                    } catch (e) {
                      setErroAcao(e)
                    }
                  }}
                >
                  Salvar
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setEditando(false)}
                >
                  Cancelar
                </Button>
              </div>
            ) : (
              <p className="text-xl font-semibold">{data.name}</p>
            )}
            <p className="text-muted-foreground text-sm">
              {papel === 'PROFESSOR' ? 'Professor' : 'Aluno'} · id #{data.id}
            </p>
          </div>
        </div>

        {erroAcao != null && <ErroApi erro={erroAcao} />}

        <Separator />

        <div className="flex gap-2">
          {pode('EDITAR_USUARIO', `USUARIO/${data.id}`) && !editando && (
            <Button
              variant="outline"
              onClick={() => {
                setNovoNome(data.name)
                setEditando(true)
              }}
            >
              Renomear
            </Button>
          )}

          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="destructive">Excluir minha conta</Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Excluir a conta?</AlertDialogTitle>
                <AlertDialogDescription>
                  Você será desligado das turmas e a sessão será encerrada. Isso
                  não pode ser desfeito.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Cancelar</AlertDialogCancel>
                <AlertDialogAction
                  onClick={async () => {
                    try {
                      await excluirUsuario(data.id)
                      esquecer(data.id)
                      sair()
                      navigate('/login', { replace: true })
                    } catch (e) {
                      setErroAcao(e)
                    }
                  }}
                >
                  Excluir
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      </div>
    </TituloFrame>
  )
}
