import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useSWRConfig } from 'swr'
import { criarTurma } from '@/lib/api'
import { podeCriarTurma } from '@/lib/permissoes'
import { useSessao } from '@/providers/SessaoProvider'
import TituloFrame from './TituloFrame'
import ErroApi, { SemPermissao } from './ErroApi'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export default function CriarTurma() {
  const [nome, setNome] = useState('')
  const [erro, setErro] = useState<unknown>(null)
  const [salvando, setSalvando] = useState(false)
  const { sessao } = useSessao()
  const { mutate } = useSWRConfig()
  const navigate = useNavigate()

  if (!sessao) return null
  if (!podeCriarTurma(sessao))
    return (
      <TituloFrame titulo="Nova turma">
        <SemPermissao>
          Apenas professores podem criar turmas. O administrador modera o
          conteúdo, mas não o cria.
        </SemPermissao>
      </TituloFrame>
    )

  return (
    <TituloFrame titulo="Nova turma">
      <form
        className="flex max-w-sm flex-col gap-3"
        onSubmit={async (e) => {
          e.preventDefault()
          if (!nome.trim()) return
          setSalvando(true)
          setErro(null)
          try {
            const turma = await criarTurma(nome.trim())
            await mutate([sessao.id, 'turmas'])
            navigate(`/turmas/${turma.id}`)
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
        {erro != null && <ErroApi erro={erro} />}
        <Button type="submit" className="self-end" disabled={salvando}>
          {salvando ? 'Criando...' : 'Criar'}
        </Button>
      </form>
    </TituloFrame>
  )
}
