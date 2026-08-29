import { useState } from 'react'
import useSWR from 'swr'
import {
  atualizarComentario,
  criarComentario,
  excluirComentario,
  listarComentarios,
} from '@/lib/api'
import { usePermissoes, type Pode } from '@/hooks/usePermissoes'
import { useSessao } from '@/providers/SessaoProvider'
import type { Comentario, TipoPublicacao, Turma } from '@/lib/types'
import ErroApi from './ErroApi'
import UsuarioAvatar from './UsuarioAvatar'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Skeleton } from '@/components/ui/skeleton'

/**
 * Recebe a turma inteira (e não só o id) porque a regra de exclusão precisa
 * saber quem é o professor responsável: ele apaga qualquer comentário da turma.
 */
export default function Comentarios({
  turma,
  tipo,
  pubId,
}: {
  turma: Turma
  tipo: TipoPublicacao
  pubId: string
}) {
  const { sessao } = useSessao()
  const [conteudo, setConteudo] = useState('')
  const [erroAcao, setErroAcao] = useState<unknown>(null)

  const { data, error, isLoading, mutate } = useSWR(
    sessao ? [sessao.id, 'comentarios', turma.id, tipo, pubId] : null,
    () => listarComentarios(turma.id, tipo, pubId),
  )

  // a turma responde por criar; cada comentário, por editar e excluir
  const { pode } = usePermissoes([
    `TURMA/${turma.id}` as const,
    ...(data ?? []).map((c) => `COMENTARIO/${c.id}` as const),
  ])

  if (!sessao) return null
  if (error != null) return <ErroApi erro={error} className="mt-4" />

  return (
    <div className="mt-6 space-y-4 rounded-xl border p-4">
      {isLoading ? (
        <Skeleton className="h-12 w-full" />
      ) : data?.length === 0 ? (
        <p className="text-muted-foreground text-sm italic">
          Nenhum comentário ainda.
        </p>
      ) : (
        <ul className="space-y-3">
          {data?.map((comentario) => (
            <ItemComentario
              key={comentario.id}
              comentario={comentario}
              turma={turma}
              tipo={tipo}
              pubId={pubId}
              pode={pode}
              onMudou={() => mutate()}
            />
          ))}
        </ul>
      )}

      {erroAcao != null && <ErroApi erro={erroAcao} />}

      {pode('CRIAR_COMENTARIO', `TURMA/${turma.id}`) && (
        <div className="flex w-full items-start gap-2">
          <UsuarioAvatar nome={sessao.name} />
          <div className="w-full space-y-1">
            <Textarea
              placeholder="Escreva um comentário"
              value={conteudo}
              onChange={(e) => setConteudo(e.target.value)}
            />
            <Button
              variant="outline"
              size="sm"
              className="float-right"
              onClick={async () => {
                if (!conteudo.trim()) return
                setErroAcao(null)
                try {
                  await criarComentario(turma.id, tipo, pubId, conteudo.trim())
                  setConteudo('')
                  mutate()
                } catch (e) {
                  setErroAcao(e)
                }
              }}
            >
              Enviar
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}

function ItemComentario({
  comentario,
  turma,
  tipo,
  pubId,
  pode,
  onMudou,
}: {
  comentario: Comentario
  turma: Turma
  tipo: TipoPublicacao
  pubId: string
  pode: Pode
  onMudou: () => void
}) {
  const { sessao } = useSessao()
  const [editando, setEditando] = useState(false)
  const [texto, setTexto] = useState(comentario.conteudo)
  if (!sessao) return null

  return (
    <li className="flex items-start gap-2">
      <UsuarioAvatar nome={comentario.autor.name} className="h-8 w-8" />
      <div className="w-full">
        <p className="text-xs font-medium">{comentario.autor.name}</p>
        {editando ? (
          <div className="space-y-1">
            <Textarea
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
            />
            <div className="flex justify-end gap-1">
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  setTexto(comentario.conteudo)
                  setEditando(false)
                }}
              >
                Cancelar
              </Button>
              <Button
                size="sm"
                onClick={async () => {
                  if (!texto.trim()) return
                  await atualizarComentario(
                    turma.id,
                    tipo,
                    pubId,
                    comentario.id,
                    texto.trim(),
                  )
                  setEditando(false)
                  onMudou()
                }}
              >
                Salvar
              </Button>
            </div>
          </div>
        ) : (
          <p className="text-sm">{comentario.conteudo}</p>
        )}
      </div>

      {!editando && (
        <div className="ml-auto flex shrink-0 gap-1">
          {/* Editar é do autor; excluir também vale para a moderação da turma. */}
          {pode('EDITAR_COMENTARIO', `COMENTARIO/${comentario.id}`) && (
            <Button size="sm" variant="ghost" onClick={() => setEditando(true)}>
              Editar
            </Button>
          )}
          {pode('EXCLUIR_COMENTARIO', `COMENTARIO/${comentario.id}`) && (
            <Button
              size="sm"
              variant="ghost"
              className="text-destructive"
              onClick={async () => {
                await excluirComentario(turma.id, tipo, pubId, comentario.id)
                onMudou()
              }}
            >
              Excluir
            </Button>
          )}
        </div>
      )}
    </li>
  )
}
