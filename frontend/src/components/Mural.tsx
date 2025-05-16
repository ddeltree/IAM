import { useState } from 'react'
import { Link, useOutletContext } from 'react-router'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Input } from '@/components/ui/input'
import { useUser } from '@/providers/UserProvider'
import useSWR from 'swr'

export default function Mural() {
  const turma = useOutletContext<any>()
  const { user } = useUser()
  const [titulo, setTitulo] = useState('')
  const [corpo, setCorpo] = useState('')
  const { data } = useSWR('listar-posts', listar)

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
              className="flex w-full flex-col gap-2 rounded-lg border p-4 shadow-xs"
            >
              <div className="flex items-center justify-start">
                <Avatar className="h-12 w-12">
                  <AvatarImage src="https://github.com/shadcn.png" />
                  <AvatarFallback>SC</AvatarFallback>
                </Avatar>
                <div className="flex w-full flex-col justify-center pl-4">
                  <p className="text-xl font-semibold">{post.titulo}</p>
                  <p className="text-xs">{post.autor.name}</p>
                </div>
              </div>
              <div className="w-full">
                <div>
                  <p>{post.corpo}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
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
