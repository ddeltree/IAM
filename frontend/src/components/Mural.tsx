import { useState } from 'react'
import { Link } from 'react-router'
import useSWR from 'swr'
import { format, parseISO } from 'date-fns'
import { criarPost, listarAtividades, listarPosts } from '@/lib/api'
import { podeCriarPost } from '@/lib/permissoes'
import { useSessao } from '@/providers/SessaoProvider'
import { useTurma } from './layout/TurmaLayout'
import PostCard from './PostCard'
import ErroApi from './ErroApi'
import UsuarioAvatar from './UsuarioAvatar'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Skeleton } from '@/components/ui/skeleton'

export default function Mural() {
  const { turma, recarregarTurma } = useTurma()
  const { sessao } = useSessao()
  const [titulo, setTitulo] = useState('')
  const [corpo, setCorpo] = useState('')
  const [erro, setErro] = useState<unknown>(null)
  const [enviando, setEnviando] = useState(false)

  const {
    data: posts,
    error,
    isLoading,
    mutate,
  } = useSWR(sessao ? [sessao.id, 'posts', turma.id] : null, () =>
    listarPosts(turma.id),
  )

  if (!sessao) return null

  // A turma carrega os posts embutidos, então ela também fica velha a cada
  // publicação — revalidar as duas mantém o mural e o card da turma de acordo.
  const recarregar = () => {
    mutate()
    recarregarTurma()
  }

  return (
    <div>
      <div className="bg-primary flex h-48 items-end rounded-lg px-8 pb-6">
        <div>
          <p className="text-primary-foreground text-3xl font-semibold">
            {turma.nome}
          </p>
          <p className="text-primary-foreground/70 text-sm">
            {turma.professorResponsavel.name}
          </p>
        </div>
      </div>

      <div className="mt-6 flex gap-4">
        <ProximasAtividades />

        <div className="flex w-full flex-col gap-4">
          {podeCriarPost(sessao, turma) && (
            <div className="w-full rounded-lg border p-4 shadow-md">
              <div className="flex w-full items-start gap-2">
                <UsuarioAvatar nome={sessao.name} className="h-12 w-12" />
                <div className="w-full space-y-1">
                  <Input
                    type="text"
                    value={titulo}
                    onChange={(e) => setTitulo(e.target.value)}
                    placeholder="Título"
                  />
                  <Textarea
                    placeholder="Escreva um aviso para a sua turma"
                    value={corpo}
                    onChange={(e) => setCorpo(e.target.value)}
                  />
                  {erro != null && <ErroApi erro={erro} />}
                  <Button
                    variant="outline"
                    size="sm"
                    className="float-right"
                    disabled={enviando}
                    onClick={async () => {
                      if (!titulo.trim() || !corpo.trim()) return
                      setEnviando(true)
                      setErro(null)
                      try {
                        await criarPost(turma.id, {
                          titulo: titulo.trim(),
                          corpo: corpo.trim(),
                        })
                        setTitulo('')
                        setCorpo('')
                        recarregar()
                      } catch (e) {
                        setErro(e)
                      } finally {
                        setEnviando(false)
                      }
                    }}
                  >
                    {enviando ? 'Enviando...' : 'Enviar'}
                  </Button>
                </div>
              </div>
            </div>
          )}

          {error != null && <ErroApi erro={error} />}
          {isLoading && <Skeleton className="h-32 w-full rounded-lg" />}
          {posts?.length === 0 && (
            <p className="text-muted-foreground italic">
              Nenhuma publicação no mural.
            </p>
          )}
          {posts?.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              turma={turma}
              onMudou={recarregar}
            />
          ))}
        </div>
      </div>
    </div>
  )
}

function ProximasAtividades() {
  const { turma } = useTurma()
  const { sessao } = useSessao()
  const { data } = useSWR(
    sessao ? [sessao.id, 'atividades', turma.id] : null,
    () => listarAtividades(turma.id),
  )

  const proximas = (data ?? [])
    .filter((a) => a.dataEntrega)
    .sort((a, b) => a.dataEntrega!.localeCompare(b.dataEntrega!))
    .slice(0, 3)

  return (
    <aside className="flex h-min w-52 shrink-0 flex-col justify-between space-y-3 rounded-lg border text-sm">
      <div className="space-y-2 px-4 pt-4">
        <p className="font-semibold">Próximas atividades</p>
        {proximas.length === 0 ? (
          <p className="text-muted-foreground text-xs">Nada por enquanto.</p>
        ) : (
          <ul className="space-y-2">
            {proximas.map((a) => (
              <li key={a.id}>
                <Link
                  to={`atividades/${a.id}`}
                  className="block hover:underline"
                >
                  <span className="block truncate">{a.titulo}</span>
                  <span className="text-muted-foreground text-xs">
                    {format(parseISO(a.dataEntrega!), 'dd/MM/yyyy')}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
      <Link className="flex justify-end px-2 pb-4" to="atividades">
        <Button variant="ghost" size="sm">
          Ver tudo
        </Button>
      </Link>
    </aside>
  )
}
