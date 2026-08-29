import { useState } from 'react'
import { Link } from 'react-router'
import useSWR from 'swr'
import {
  criarUsuario,
  desmatricular,
  listarParticipantes,
  matricular,
} from '@/lib/api'
import { lembrar, listarConhecidos } from '@/lib/conhecidos'
import { podeMatricular } from '@/lib/permissoes'
import { useSessao } from '@/providers/SessaoProvider'
import { useTurma } from './layout/TurmaLayout'
import ErroApi from './ErroApi'
import UsuarioAvatar from './UsuarioAvatar'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
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

export default function Pessoas() {
  const { turma, recarregarTurma } = useTurma()
  const { sessao } = useSessao()

  const { data, error, isLoading, mutate } = useSWR(
    sessao ? [sessao.id, 'participantes', turma.id] : null,
    () => listarParticipantes(turma.id),
  )

  if (!sessao) return null
  if (error != null) return <ErroApi erro={error} />

  const recarregar = () => {
    mutate()
    recarregarTurma() // turma.alunos também mudou
  }

  // O payload de Participante não diz quem é professor; a turma diz.
  const professorId = turma.professorResponsavel.id
  const professor = data?.find((p) => p.userId === professorId)
  const alunos = data?.filter((p) => p.userId !== professorId) ?? []

  return (
    <div className="mx-auto w-full max-w-2xl space-y-6">
      <section>
        <h2 className="text-xl font-semibold">Professor</h2>
        <Separator className="my-2" />
        {isLoading ? (
          <Skeleton className="h-12 w-full" />
        ) : professor ? (
          <div className="flex items-center gap-3 py-2">
            <UsuarioAvatar nome={professor.name} className="h-9 w-9" />
            <div className="flex flex-col leading-tight">
              <span className="font-medium">{professor.name}</span>
              <span className="text-muted-foreground text-xs">
                Responsável · id #{professor.userId}
              </span>
            </div>
          </div>
        ) : (
          <p className="text-muted-foreground italic">
            {turma.professorResponsavel.name}
          </p>
        )}
      </section>

      <section>
        <h2 className="text-xl font-semibold">
          Alunos {alunos.length > 0 && `(${alunos.length})`}
        </h2>
        <Separator className="my-2" />
        {isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : alunos.length === 0 ? (
          <p className="text-muted-foreground italic">
            Nenhum aluno matriculado ainda.
          </p>
        ) : (
          <ul className="divide-y">
            {alunos.map((aluno) => (
              <li key={aluno.userId} className="flex items-center gap-3 py-2">
                <UsuarioAvatar nome={aluno.name} className="h-9 w-9" />
                <div className="flex flex-col leading-tight">
                  <span className="font-medium">{aluno.name}</span>
                  <span className="text-muted-foreground text-xs">
                    id #{aluno.userId}
                  </span>
                </div>
                {podeMatricular(sessao, turma) && (
                  <AlertDialog>
                    <AlertDialogTrigger asChild>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive ml-auto"
                      >
                        Remover
                      </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogTitle>
                        Remover {aluno.name} da turma?
                      </AlertDialogTitle>
                      <AlertDialogHeader>
                        <AlertDialogDescription>
                          Ele perde o acesso ao mural, às atividades e aos
                          comentários desta turma.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Cancelar</AlertDialogCancel>
                        <AlertDialogAction
                          onClick={async () => {
                            await desmatricular(turma.id, aluno.userId)
                            recarregar()
                          }}
                        >
                          Remover
                        </AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      {podeMatricular(sessao, turma) && (
        <Matricula
          turmaId={turma.id}
          jaMatriculados={alunos.map((a) => a.userId)}
          onMatriculou={recarregar}
        />
      )}
    </div>
  )
}

/**
 * Matricular exige saber o id do aluno: professores não têm LISTAR_USUARIOS,
 * então não há como oferecer a lista completa do sistema. Daí os três caminhos.
 */
function Matricula({
  turmaId,
  jaMatriculados,
  onMatriculou,
}: {
  turmaId: string
  jaMatriculados: string[]
  onMatriculou: () => void
}) {
  const [nome, setNome] = useState('')
  const [uid, setUid] = useState('')
  const [escolhido, setEscolhido] = useState('')
  const [erro, setErro] = useState<unknown>(null)
  const [ocupado, setOcupado] = useState(false)

  const disponiveis = listarConhecidos().filter(
    (c) => c.papel === 'ALUNO' && !jaMatriculados.includes(c.id),
  )

  async function executar(acao: () => Promise<void>) {
    setOcupado(true)
    setErro(null)
    try {
      await acao()
      onMatriculou()
    } catch (e) {
      setErro(e)
    } finally {
      setOcupado(false)
    }
  }

  return (
    <section className="space-y-4 rounded-xl border p-4">
      <h2 className="font-semibold">Matricular aluno</h2>

      <div className="space-y-2">
        <Label htmlFor="novo-aluno">Criar um aluno e matricular</Label>
        <div className="flex gap-2">
          <Input
            id="novo-aluno"
            placeholder="Nome do aluno"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
          />
          <Button
            disabled={ocupado || !nome.trim()}
            onClick={() =>
              executar(async () => {
                const aluno = await criarUsuario(nome.trim(), 0)
                lembrar({ id: aluno.id, name: aluno.name, papel: 'ALUNO' })
                await matricular(turmaId, aluno.id)
                setNome('')
              })
            }
          >
            Criar e matricular
          </Button>
        </div>
      </div>

      {disponiveis.length > 0 && (
        <div className="space-y-2">
          <Label>Aluno já usado neste navegador</Label>
          <div className="flex gap-2">
            <Select value={escolhido} onValueChange={setEscolhido}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Escolher aluno" />
              </SelectTrigger>
              <SelectContent>
                {disponiveis.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.name} · #{c.id}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button
              variant="outline"
              disabled={ocupado || !escolhido}
              onClick={() =>
                executar(async () => {
                  await matricular(turmaId, escolhido)
                  setEscolhido('')
                })
              }
            >
              Matricular
            </Button>
          </div>
        </div>
      )}

      <div className="space-y-2">
        <Label htmlFor="uid-aluno">Matricular por id</Label>
        <div className="flex gap-2">
          <Input
            id="uid-aluno"
            inputMode="numeric"
            placeholder="ex: 4"
            value={uid}
            onChange={(e) => setUid(e.target.value)}
          />
          <Button
            variant="outline"
            disabled={ocupado || !uid.trim()}
            onClick={() =>
              executar(async () => {
                await matricular(turmaId, uid.trim())
                setUid('')
              })
            }
          >
            Matricular
          </Button>
        </div>
        <p className="text-muted-foreground text-xs">
          Só o administrador pode listar os usuários do sistema, então um aluno
          criado em outro navegador precisa ser informado pelo id.{' '}
          <Link to={`/usuarios/novo?turmaId=${turmaId}`} className="underline">
            Criar aluno na tela cheia
          </Link>
        </p>
      </div>

      {erro != null && <ErroApi erro={erro} />}
    </section>
  )
}
