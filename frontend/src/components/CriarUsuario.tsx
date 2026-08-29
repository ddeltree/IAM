import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useSWRConfig } from 'swr'
import { criarUsuario, matricular } from '@/lib/api'
import { lembrar } from '@/lib/conhecidos'
import { usePermissoes } from '@/hooks/usePermissoes'
import { useSessao } from '@/providers/SessaoProvider'
import type { UsuarioDTO } from '@/lib/types'
import TituloFrame from './TituloFrame'
import ErroApi, { SemPermissao } from './ErroApi'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Separator } from '@/components/ui/separator'

export default function CriarUsuario() {
  const { sessao } = useSessao()
  const { pode, carregando } = usePermissoes()
  const { mutate } = useSWRConfig()
  const [parametros] = useSearchParams()
  const turmaId = parametros.get('turmaId')

  const [nome, setNome] = useState('')
  const [tipo, setTipo] = useState<'0' | '1'>('0')
  const [criado, setCriado] = useState<UsuarioDTO | null>(null)
  const [erro, setErro] = useState<unknown>(null)
  const [salvando, setSalvando] = useState(false)

  if (!sessao || carregando) return null

  const podeProfessor = pode('CRIAR_PROFESSOR')
  const podeAluno = pode('CRIAR_ALUNO')

  if (!podeProfessor && !podeAluno)
    return (
      <TituloFrame titulo="Novo usuário">
        <SemPermissao>
          Alunos não podem criar contas. Professores criam alunos; o
          administrador cria professores.
        </SemPermissao>
      </TituloFrame>
    )

  // Cada papel só cria um tipo: CRIAR_PROFESSOR é exclusivo do admin e
  // CRIAR_ALUNO, dos professores. Mostrar as duas opções só geraria 403.
  const tipoFixo: '0' | '1' | null = podeProfessor
    ? '1'
    : podeAluno
      ? '0'
      : null
  const tipoEscolhido = tipoFixo ?? tipo

  if (criado)
    return (
      <TituloFrame titulo="Usuário criado">
        <div className="max-w-md space-y-4">
          <p>
            <strong>{criado.name}</strong> foi criado como{' '}
            {criado.tipo === 1 ? 'professor' : 'aluno'}.
          </p>
          <div className="bg-accent text-accent-foreground rounded-lg px-4 py-3">
            <p className="text-sm">
              id de acesso: <strong className="text-lg">#{criado.id}</strong>
            </p>
            <p className="mt-1 text-xs">
              É com esse id que ele entra no sistema — e é ele que você informa
              para matricular numa turma.
            </p>
          </div>
          <Separator />
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => {
                setCriado(null)
                setNome('')
              }}
            >
              Criar outro
            </Button>
            {turmaId && (
              <Link to={`/turmas/${turmaId}/pessoas`}>
                <Button>Voltar para a turma</Button>
              </Link>
            )}
          </div>
        </div>
      </TituloFrame>
    )

  return (
    <TituloFrame titulo={podeProfessor ? 'Novo professor' : 'Novo aluno'}>
      <form
        className="grid max-w-sm grid-cols-1 gap-3"
        onSubmit={async (e) => {
          e.preventDefault()
          if (!nome.trim()) return
          setSalvando(true)
          setErro(null)
          try {
            const usuario = await criarUsuario(
              nome.trim(),
              tipoEscolhido === '1' ? 1 : 0,
            )
            lembrar({
              id: usuario.id,
              name: usuario.name,
              papel: usuario.tipo === 1 ? 'PROFESSOR' : 'ALUNO',
            })
            // Veio da tela de Pessoas: já matricula e volta pronto.
            if (turmaId && usuario.tipo === 0) {
              await matricular(turmaId, usuario.id)
              await mutate([sessao.id, 'participantes', turmaId])
              await mutate([sessao.id, 'turma', turmaId])
            }
            await mutate([sessao.id, 'usuarios'])
            setCriado(usuario)
          } catch (err) {
            setErro(err)
          } finally {
            setSalvando(false)
          }
        }}
      >
        <div className="grid gap-1">
          <Label htmlFor="nome">Nome</Label>
          <Input
            id="nome"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
          />
        </div>

        <div className="flex flex-col">
          <Label>Tipo</Label>
          {tipoFixo ? (
            <p className="text-muted-foreground py-2 text-sm">
              {tipoFixo === '1' ? 'Professor' : 'Aluno'} — é o único tipo que
              seu papel pode criar.
            </p>
          ) : (
            <RadioGroup
              value={tipo}
              onValueChange={(v) => setTipo(v as '0' | '1')}
              className="grid gap-1 rounded-lg p-2"
            >
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="0" id="tipo-aluno" />
                <Label htmlFor="tipo-aluno">Aluno</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="1" id="tipo-professor" />
                <Label htmlFor="tipo-professor">Professor</Label>
              </div>
            </RadioGroup>
          )}
        </div>

        {turmaId && tipoEscolhido === '0' && (
          <p className="text-muted-foreground text-xs">
            O aluno será matriculado na turma assim que for criado.
          </p>
        )}

        {erro != null && <ErroApi erro={erro} />}

        <Button type="submit" className="justify-self-end" disabled={salvando}>
          {salvando ? 'Criando...' : 'Criar'}
        </Button>
      </form>
    </TituloFrame>
  )
}
