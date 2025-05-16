import { useState } from 'react'
import { Link, useOutletContext } from 'react-router'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Input } from '@/components/ui/input'
import { useUser } from '@/providers/UserProvider'
import useSWR from 'swr'
import { Edit } from 'lucide-react'
import { cn } from '@/lib/utils'

export default function Mural() {
  const turma = useOutletContext<any>()
  const { user } = useUser()
  const [titulo, setTitulo] = useState('')
  const [corpo, setCorpo] = useState('')
  const [novotitulo, setNovoTitulo] = useState('')
  const [novocorpo, setNovoCorpo] = useState('')
  const [editando, setEditando] = useState(false)
  const [editandoId, setEditandoId] = useState('')
  const { data, mutate } = useSWR('listar-posts', listar)

  return (
    <div>
      <div className="bg-primary flex h-48 items-end rounded-lg px-8 pb-6">
        <p className="text-primary-foreground text-3xl font-semibold">
          {turma?.nome}
        </p>
      </div>
      <div className="mt-6 flex gap-4">
        <aside className="flex h-min w-44 flex-col justify-between space-y-3 rounded-lg border text-sm">
          <div className="space-y-3 px-4 pt-4">
            <p className="font-semibold">Próximas atividades</p>
            <p>...</p>
          </div>
          <Link className="flex justify-end px-2 pb-4" to="atividades">
            <Button variant="ghost">Ver tudo</Button>
          </Link>
        </aside>

        <div className="flex w-full flex-col gap-4">
          <div className="w-full rounded-lg border p-4 shadow-md">
            <div className="flex w-full items-start gap-2">
              <Avatar className="h-12 w-12">
                <AvatarImage src="https://github.com/shadcn.png" />
                <AvatarFallback>SC</AvatarFallback>
              </Avatar>
              <div className="w-full space-y-0.5">
                <Input
                  type="text"
                  onChange={(e: any) => setTitulo(e.target.value)}
                  placeholder="Titulo"
                />
                <Textarea
                  placeholder="Escreva um aviso para a sua turma"
                  onChange={(e) => setCorpo(e.target.value)}
                />
                <Button
                  variant="outline"
                  size="sm"
                  className="float-right"
                  onClick={() => {
                    if (!titulo.trim() || !corpo.trim() || !turma || !user)
                      return
                    console.log(postar(titulo, corpo, turma.id, user.id))
                  }}
                >
                  Enviar
                </Button>
              </div>
            </div>
          </div>

          {data?.map((post) => (
            <div
              key={post.id}
              className="relative flex w-full flex-col gap-2 rounded-lg border p-4 shadow-xs"
            >
              <div className="flex items-center justify-start">
                <Avatar className="h-12 w-12">
                  <AvatarImage src="https://github.com/shadcn.png" />
                  <AvatarFallback>EU</AvatarFallback>
                </Avatar>
                <div className="flex w-full flex-col justify-center pl-4">
                  {editando && editandoId === post.id ? (
                    <Input
                      defaultValue={post.titulo}
                      onChange={(e) => setNovoTitulo(e.target.value)}
                    />
                  ) : (
                    <p className="text-xl font-semibold">{post.titulo}</p>
                  )}
                  <p className="text-xs">{post.autor.name}</p>
                </div>
              </div>
              <div className="w-full">
                {editando && editandoId === post.id ? (
                  <Textarea
                    defaultValue={post.corpo}
                    onChange={(e) => setNovoCorpo(e.target.value)}
                  />
                ) : (
                  <p>{post.corpo}</p>
                )}
              </div>
              {editando && editandoId === post.id && (
                <div className="flex justify-end">
                  <Button
                    variant={'secondary'}
                    onClick={() => setEditando(false)}
                  >
                    Cancelar
                  </Button>
                  <Button
                    onClick={async () => {
                      setEditando(false)
                      await atualizar(post.id, novotitulo, novocorpo)
                      mutate()
                    }}
                  >
                    Salvar
                  </Button>
                </div>
              )}
              <Edit
                className={cn(
                  'text-muted-foreground absolute top-4 right-4',
                  editando && editandoId === post.id && 'hidden',
                )}
                size={18}
                onClick={() => {
                  setEditando(!editando)
                  setEditandoId(post.id)
                  setNovoTitulo(post.titulo)
                  setNovoCorpo(post.corpo)
                }}
              />
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

async function atualizar(id: string, titulo: string, corpo: string) {
  const response = await fetch(`http://localhost:7000/posts/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ titulo, corpo }),
  })
  const post = await response.json()
  return post as Record<string, any>[]
}

async function postar(
  titulo: string,
  corpo: string,
  turmaId: number,
  autorId: number,
) {
  const response = await fetch('http://localhost:7000/posts', {
    method: 'POST',
    body: JSON.stringify({
      turmaId,
      corpo,
      titulo,
      autorId,
    }),
  })
  const post = await response.json()
  return post as Record<string, any>[]
}

async function listar() {
  const response = await fetch('http://localhost:7000/posts')
  const posts = await response.json()
  return posts as Record<string, any>[]
}
