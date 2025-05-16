import { useUser } from '@/providers/UserProvider'
import useSWR from 'swr'
import { Textarea } from './ui/textarea'
import { Avatar, AvatarFallback, AvatarImage } from '@radix-ui/react-avatar'
import { Button } from '@/components/ui/button'
import { useState } from 'react'

export default function Comentarios({ postId }: { postId: string }) {
  const { user } = useUser()
  const { data, error, isLoading, mutate } = useSWR('comentarios', listar)
  const [conteudo, setConteudo] = useState('')

  if (error) return <div>Erro ao listar comentários</div>
  if (isLoading) return <div>Carregando...</div>
  return (
    <div className="mt-6 space-y-4 rounded-xl border p-4">
      <ul>
        {data?.length === 0 && <p>Nenhum comentário</p>}
        {data?.map((comentario: any) => (
          <li key={comentario.id}>
            <p>{comentario.conteudo}</p>
            <p>{comentario.autor.name}</p>
            {comentario.autor.id === user?.id && (
              <button onClick={() => excluir(comentario.id)}>Excluir</button>
            )}
          </li>
        ))}
      </ul>

      <div className="flex w-full items-start gap-2">
        <Avatar className="h-12 w-12">
          <AvatarImage src="https://github.com/shadcn.png" />
          <AvatarFallback>EU</AvatarFallback>
        </Avatar>
        <div className="w-full space-y-0.5">
          <Textarea
            placeholder="Escreva um comentário"
            onChange={(e) => setConteudo(e.target.value)}
          />
          <Button
            variant="outline"
            size="sm"
            className="float-right"
            onClick={async () => {
              if (!conteudo.trim() || !user) return
              await criar(conteudo, user.id, postId)
              mutate()
            }}
          >
            Enviar
          </Button>
        </div>
      </div>
    </div>
  )
}

async function listar() {
  const response = await fetch('http://localhost:7000/comentarios')
  const comments = await response.json()
  return comments as Record<string, any>[]
}

async function criar(conteudo: string, autorId: string, postId: string) {
  const response = await fetch('http://localhost:7000/comentarios', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      conteudo,
      autorId,
      postId,
    }),
  })
  return response.json()
}

async function excluir(id: string) {
  const response = await fetch(`http://localhost:7000/comentarios/${id}`, {
    method: 'DELETE',
  })
  return response.text()
}

async function ver(id: string | undefined) {
  if (!id) return
  const response = await fetch(`http://localhost:7000/comentarios/${id}`)
  const comentario = await response.json()
  return comentario as Record<string, any>
}

async function atualizar(id: string, conteudo: string) {
  const response = await fetch(`http://localhost:7000/comentarios/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ conteudo }),
  })
  const comentario = await response.json()
  return comentario as Record<string, any>
}
