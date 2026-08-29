import { useState } from 'react'
import { atualizarPost, excluirPost } from '@/lib/api'
import type { Pode } from '@/hooks/usePermissoes'
import { useSessao } from '@/providers/SessaoProvider'
import type { Post, Turma } from '@/lib/types'
import Comentarios from './Comentarios'
import ErroApi from './ErroApi'
import UsuarioAvatar from './UsuarioAvatar'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'
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
import { MessageSquare } from 'lucide-react'

export default function PostCard({
  post,
  turma,
  pode,
  onMudou,
}: {
  post: Post
  turma: Turma
  /** Vem do Mural, que consulta as permissões de todos os posts de uma vez. */
  pode: Pode
  onMudou: () => void
}) {
  const { sessao } = useSessao()
  const [editando, setEditando] = useState(false)
  const [titulo, setTitulo] = useState(post.titulo)
  const [corpo, setCorpo] = useState(post.corpo)
  const [erro, setErro] = useState<unknown>(null)

  if (!sessao) return null

  return (
    <div className="flex w-full flex-col gap-2 rounded-lg border p-4 shadow-xs">
      <div className="flex items-start gap-3">
        <UsuarioAvatar nome={post.autor.name} className="h-12 w-12" />
        <div className="flex w-full flex-col justify-center">
          {editando ? (
            <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
          ) : (
            <p className="text-xl font-semibold">{post.titulo}</p>
          )}
          <p className="text-muted-foreground text-xs">{post.autor.name}</p>
        </div>

        {!editando && (
          <div className="flex shrink-0 gap-1">
            {pode('EDITAR_POST', `POST/${post.id}`) && (
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  setTitulo(post.titulo)
                  setCorpo(post.corpo)
                  setEditando(true)
                }}
              >
                Editar
              </Button>
            )}
            {pode('EXCLUIR_POST', `POST/${post.id}`) && (
              <AlertDialog>
                <AlertDialogTrigger asChild>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="text-destructive"
                  >
                    Excluir
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Excluir a publicação?</AlertDialogTitle>
                    <AlertDialogDescription>
                      Os comentários dela serão excluídos junto. Isso não pode
                      ser desfeito.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancelar</AlertDialogCancel>
                    <AlertDialogAction
                      onClick={async () => {
                        try {
                          await excluirPost(turma.id, post.id)
                          onMudou()
                        } catch (e) {
                          setErro(e)
                        }
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

      {editando ? (
        <>
          <Textarea value={corpo} onChange={(e) => setCorpo(e.target.value)} />
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setEditando(false)}>
              Cancelar
            </Button>
            <Button
              onClick={async () => {
                if (!titulo.trim() || !corpo.trim()) return
                setErro(null)
                try {
                  await atualizarPost(turma.id, post.id, {
                    titulo: titulo.trim(),
                    corpo: corpo.trim(),
                  })
                  setEditando(false)
                  onMudou()
                } catch (e) {
                  setErro(e)
                }
              }}
            >
              Salvar
            </Button>
          </div>
        </>
      ) : (
        <p className="whitespace-pre-wrap">{post.corpo}</p>
      )}

      {erro != null && <ErroApi erro={erro} />}

      <Collapsible>
        <CollapsibleTrigger asChild>
          <Button variant="ghost" size="sm" className="w-fit gap-2">
            <MessageSquare size={16} />
            Comentários ({post.comentarios.length})
          </Button>
        </CollapsibleTrigger>
        <CollapsibleContent>
          <Comentarios turma={turma} tipo="posts" pubId={post.id} />
        </CollapsibleContent>
      </Collapsible>
    </div>
  )
}
