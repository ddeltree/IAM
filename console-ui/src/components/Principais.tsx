import { useState } from 'react'
import { Plus, Trash2, UserPlus } from 'lucide-react'
import {
  anexarPolitica,
  apagarPrincipal,
  assumirPapel,
  criarPrincipal,
  desanexarPolitica,
  entrarNoGrupo,
  removerClausula,
  sairDoGrupo,
} from '@/lib/api'
import { resumir } from '@/lib/condicao'
import { useCenario } from '@/providers/CenarioProvider'
import type { Principal, Statement } from '@/lib/types'
import Titulo from './Titulo'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const ABAS = [
  { chave: 'usuarios', rotulo: 'Usuários' },
  { chave: 'grupos', rotulo: 'Grupos' },
  { chave: 'papeis', rotulo: 'Papéis' },
] as const

export default function Principais() {
  const { cenario, recarregar, escolherIdentidade } = useCenario()
  const [aba, setAba] = useState<'usuarios' | 'grupos' | 'papeis'>('usuarios')
  const [novo, setNovo] = useState('')
  const [erro, setErro] = useState<string>('')

  const lista = cenario?.[aba] ?? []

  return (
    <>
      <Titulo
        titulo="Principais"
        explicacao={
          <>
            Usuários, grupos e papéis. Para o núcleo os três são a mesma coisa —
            algo com um id e uma política —, e um usuário recebe as cláusulas
            dos grupos porque <em>herda</em> deles, não porque o motor saiba o
            que é um grupo.
          </>
        }
      />

      <div className="mb-4 flex gap-1">
        {ABAS.map((a) => (
          <Button
            key={a.chave}
            variant={aba === a.chave ? 'default' : 'outline'}
            size="sm"
            onClick={() => setAba(a.chave)}
          >
            {a.rotulo}
          </Button>
        ))}
      </div>

      <form
        className="mb-6 flex gap-2"
        onSubmit={async (e) => {
          e.preventDefault()
          if (!novo.trim()) return
          setErro('')
          try {
            await criarPrincipal(aba, novo.trim())
            setNovo('')
            await recarregar()
          } catch (err) {
            setErro(String(err instanceof Error ? err.message : err))
          }
        }}
      >
        <Input
          className="w-64"
          placeholder={`nome do novo ${aba.slice(0, -1)}`}
          value={novo}
          onChange={(e) => setNovo(e.target.value)}
        />
        <Button type="submit" size="sm">
          <Plus className="mr-1 h-3 w-3" /> criar
        </Button>
        {erro && (
          <span className="text-destructive self-center text-sm">{erro}</span>
        )}
      </form>

      <div className="space-y-4">
        {lista.map((p) => (
          <Cartao
            key={p.id}
            principal={p}
            onMudou={recarregar}
            onAssumir={escolherIdentidade}
          />
        ))}
        {lista.length === 0 && (
          <p className="text-muted-foreground">Nenhum ainda.</p>
        )}
      </div>
    </>
  )
}

function Cartao({
  principal,
  onMudou,
  onAssumir,
}: {
  principal: Principal
  onMudou: () => Promise<unknown>
  onAssumir: (id: string) => void
}) {
  const { cenario, identidade } = useCenario()
  const [erro, setErro] = useState('')

  const tentar = async (acao: () => Promise<unknown>) => {
    setErro('')
    try {
      await acao()
      await onMudou()
    } catch (e) {
      setErro(String(e instanceof Error ? e.message : e))
    }
  }

  return (
    <div className="bg-card rounded-lg border p-4">
      <div className="flex flex-wrap items-center gap-2">
        <strong>{principal.nome}</strong>
        <code className="text-muted-foreground text-xs">{principal.id}</code>
        <span className="bg-muted rounded px-1.5 py-0.5 text-xs">
          {principal.tipo}
        </span>

        {principal.tipo === 'PAPEL' && identidade && (
          <Button
            size="sm"
            variant="outline"
            className="ml-auto"
            onClick={() =>
              tentar(async () => {
                const sessao = await assumirPapel(principal.id, identidade.id)
                onAssumir(sessao.id)
              })
            }
          >
            <UserPlus className="mr-1 h-3 w-3" /> assumir como {identidade.nome}
          </Button>
        )}

        <Button
          variant="ghost"
          size="icon"
          className={principal.tipo === 'PAPEL' ? '' : 'ml-auto'}
          onClick={() => tentar(() => apagarPrincipal(principal.id))}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>

      {erro && <p className="text-destructive mt-2 text-sm">{erro}</p>}

      {/* grupos, para usuários */}
      {principal.tipo === 'USUARIO' && (
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <span className="text-muted-foreground text-xs">grupos:</span>
          {(principal.grupos ?? []).map((g) => (
            <button
              key={g}
              className="bg-secondary hover:bg-destructive/15 rounded px-2 py-0.5 text-xs"
              title="sair do grupo"
              onClick={() => tentar(() => sairDoGrupo(principal.id, g))}
            >
              {g} ✕
            </button>
          ))}
          <Select
            value=""
            onValueChange={(g) => tentar(() => entrarNoGrupo(principal.id, g))}
          >
            <SelectTrigger className="h-7 w-40 text-xs">
              <SelectValue placeholder="entrar num grupo" />
            </SelectTrigger>
            <SelectContent>
              {(cenario?.grupos ?? [])
                .filter((g) => !principal.grupos?.includes(g.id))
                .map((g) => (
                  <SelectItem key={g.id} value={g.id}>
                    {g.nome}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>
      )}

      {/* políticas anexadas */}
      {principal.tipo !== 'SESSAO' && (
        <div className="mt-2 flex flex-wrap items-center gap-2">
          <span className="text-muted-foreground text-xs">
            políticas anexadas:
          </span>
          {(principal.anexadas ?? []).map((nome) => (
            <button
              key={nome}
              className="bg-secondary hover:bg-destructive/15 rounded px-2 py-0.5 text-xs"
              title="desanexar"
              onClick={() =>
                tentar(() => desanexarPolitica(principal.id, nome))
              }
            >
              {nome} ✕
            </button>
          ))}
          <Select
            value=""
            onValueChange={(nome) =>
              tentar(() => anexarPolitica(principal.id, nome))
            }
          >
            <SelectTrigger className="h-7 w-44 text-xs">
              <SelectValue placeholder="anexar política" />
            </SelectTrigger>
            <SelectContent>
              {(cenario?.politicas ?? [])
                .filter((p) => !principal.anexadas?.includes(p.nome))
                .map((p) => (
                  <SelectItem key={p.nome} value={p.nome}>
                    {p.nome}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>
      )}

      {/* a política efetiva, separada por origem — é a pergunta "por que ele
          pode isso?" respondida antes de qualquer simulação */}
      <details className="mt-3">
        <summary className="cursor-pointer text-sm font-medium">
          política efetiva ({principal.efetiva.length} cláusulas)
        </summary>
        <div className="mt-2 space-y-1">
          {principal.efetiva.map((s, i) => (
            <Linha
              key={i}
              statement={s}
              podeRemover={s.origem === 'inline'}
              onRemover={() =>
                tentar(() => removerClausula(principal.id, s.sid))
              }
            />
          ))}
          {principal.efetiva.length === 0 && (
            <p className="text-muted-foreground text-sm">
              Nenhuma cláusula — este principal não pode nada.
            </p>
          )}
        </div>
      </details>

      {/* confiança, para papéis */}
      {principal.tipo === 'PAPEL' && (principal.confianca?.length ?? 0) > 0 && (
        <details className="mt-2">
          <summary className="cursor-pointer text-sm font-medium">
            quem pode assumir ({principal.confianca!.length})
          </summary>
          <div className="mt-2 space-y-1">
            {principal.confianca!.map((s, i) => (
              <Linha key={i} statement={s} />
            ))}
          </div>
        </details>
      )}
    </div>
  )
}

function Linha({
  statement,
  podeRemover,
  onRemover,
}: {
  statement: Statement
  podeRemover?: boolean
  onRemover?: () => void
}) {
  return (
    <div className="bg-muted/40 flex flex-wrap items-center gap-2 rounded px-2 py-1 text-xs">
      <span
        className={
          'rounded px-1 font-mono ' +
          (statement.effect === 'ALLOW'
            ? 'bg-emerald-500/15 text-emerald-700'
            : 'bg-destructive/15 text-destructive')
        }
      >
        {statement.effect}
      </span>
      <code>{statement.action}</code>
      <span className="text-muted-foreground">sobre</span>
      <code>{statement.resource}</code>
      {statement.condition && (
        <span className="text-muted-foreground">
          se {resumir(statement.condition)}
        </span>
      )}
      {statement.origem && (
        <span className="text-muted-foreground ml-auto">
          {statement.origem === 'inline' ? 'inline' : `de ${statement.origem}`}
        </span>
      )}
      {podeRemover && onRemover && (
        <button
          className="hover:text-destructive"
          onClick={onRemover}
          title="remover"
        >
          ✕
        </button>
      )}
    </div>
  )
}
