import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { apagarPolitica, salvarPolitica } from '@/lib/api'
import { doDocumento, paraDocumento, SEMPRE, type No } from '@/lib/condicao'
import { useCenario } from '@/providers/CenarioProvider'
import type { Politica, Statement } from '@/lib/types'
import Titulo from './Titulo'
import EditorCondicao, { JsonDaCondicao } from './EditorCondicao'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

type Clausula = {
  sid: string
  effect: 'ALLOW' | 'DENY'
  action: string
  resource: string
  condicao: No
}

const NOVA: Clausula = {
  sid: '',
  effect: 'ALLOW',
  action: '*',
  resource: '*',
  condicao: SEMPRE,
}

export default function Politicas() {
  const { cenario, recarregar } = useCenario()
  const [editando, setEditando] = useState<{
    nome: string
    clausulas: Clausula[]
  } | null>(null)

  return (
    <>
      <Titulo
        titulo="Políticas"
        explicacao={
          <>
            Uma política nomeada é anexável a vários principais — mudá-la muda o
            acesso de todos de uma vez. É a diferença entre uma regra que
            pertence a alguém e uma que existe por si.
          </>
        }
        acao={
          <Button
            size="sm"
            onClick={() => setEditando({ nome: '', clausulas: [{ ...NOVA }] })}
          >
            <Plus className="mr-1 h-3 w-3" /> nova política
          </Button>
        }
      />

      {editando && (
        <Editor
          inicial={editando}
          onCancelar={() => setEditando(null)}
          onSalvo={async () => {
            setEditando(null)
            await recarregar()
          }}
        />
      )}

      <div className="space-y-3">
        {(cenario?.politicas ?? []).map((p) => (
          <Cartao
            key={p.nome}
            politica={p}
            onEditar={() =>
              setEditando({
                nome: p.nome,
                clausulas: p.statements.map((s) => ({
                  sid: s.sid,
                  effect: s.effect,
                  action: s.action,
                  resource: s.resource,
                  condicao: doDocumento(s.condition),
                })),
              })
            }
            onApagar={async () => {
              await apagarPolitica(p.nome)
              await recarregar()
            }}
          />
        ))}
      </div>
    </>
  )
}

function Cartao({
  politica,
  onEditar,
  onApagar,
}: {
  politica: Politica
  onEditar: () => void
  onApagar: () => Promise<void>
}) {
  return (
    <div className="bg-card rounded-lg border p-4">
      <div className="flex items-center gap-2">
        <strong>{politica.nome}</strong>
        <span className="text-muted-foreground text-xs">
          {politica.statements.length} cláusulas
        </span>
        {(politica.anexadaA?.length ?? 0) > 0 && (
          <span className="text-muted-foreground text-xs">
            · anexada a {politica.anexadaA!.join(', ')}
          </span>
        )}
        <div className="ml-auto flex gap-1">
          <Button size="sm" variant="outline" onClick={onEditar}>
            editar
          </Button>
          <Button size="icon" variant="ghost" onClick={onApagar}>
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </div>
      <div className="mt-2 space-y-1">
        {politica.statements.map((s: Statement, i) => (
          <div
            key={i}
            className="bg-muted/40 flex flex-wrap items-center gap-2 rounded px-2 py-1 text-xs"
          >
            <span
              className={
                'rounded px-1 font-mono ' +
                (s.effect === 'ALLOW'
                  ? 'bg-emerald-500/15 text-emerald-700'
                  : 'bg-destructive/15 text-destructive')
              }
            >
              {s.effect}
            </span>
            <code>{s.action}</code>
            <span className="text-muted-foreground">sobre</span>
            <code>{s.resource}</code>
            <span className="text-muted-foreground ml-auto">{s.sid}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function Editor({
  inicial,
  onCancelar,
  onSalvo,
}: {
  inicial: { nome: string; clausulas: Clausula[] }
  onCancelar: () => void
  onSalvo: () => Promise<void>
}) {
  const { cenario } = useCenario()
  const [nome, setNome] = useState(inicial.nome)
  const [clausulas, setClausulas] = useState(inicial.clausulas)
  const [erro, setErro] = useState('')

  const trocar = (i: number, parcial: Partial<Clausula>) =>
    setClausulas(clausulas.map((c, j) => (j === i ? { ...c, ...parcial } : c)))

  return (
    <div className="bg-card mb-6 rounded-lg border-2 p-4">
      <div className="mb-4 grid gap-1">
        <Label>Nome da política</Label>
        <Input
          className="w-72"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          placeholder="DonoMandaNoSeu"
        />
      </div>

      <div className="space-y-6">
        {clausulas.map((c, i) => (
          <div key={i} className="rounded-lg border p-3">
            <div className="mb-3 flex flex-wrap items-end gap-3">
              <div className="grid gap-1">
                <Label className="text-xs">Efeito</Label>
                <Select
                  value={c.effect}
                  onValueChange={(v) =>
                    trocar(i, { effect: v as 'ALLOW' | 'DENY' })
                  }
                >
                  <SelectTrigger className="h-8 w-28">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALLOW">ALLOW</SelectItem>
                    <SelectItem value="DENY">DENY</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="grid gap-1">
                <Label className="text-xs">Ação — aceita curinga</Label>
                <Input
                  list="acoes-conhecidas"
                  className="h-8 w-48"
                  value={c.action}
                  onChange={(e) => trocar(i, { action: e.target.value })}
                  placeholder="* ou LER ou EDITAR_*"
                />
              </div>

              <div className="grid gap-1">
                <Label className="text-xs">Recurso — TIPO ou TIPO/id</Label>
                <Input
                  list="recursos-conhecidos"
                  className="h-8 w-56"
                  value={c.resource}
                  onChange={(e) => trocar(i, { resource: e.target.value })}
                  placeholder="* ou BUCKET ou BUCKET/folha"
                />
              </div>

              <div className="grid gap-1">
                <Label className="text-xs">Nome (sid), opcional</Label>
                <Input
                  className="h-8 w-48"
                  value={c.sid}
                  onChange={(e) => trocar(i, { sid: e.target.value })}
                  placeholder="oDonoPodeTudo"
                />
              </div>

              <Button
                size="icon"
                variant="ghost"
                className="ml-auto h-8 w-8"
                onClick={() =>
                  setClausulas(clausulas.filter((_, j) => j !== i))
                }
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>

            <Label className="text-xs">Condição</Label>
            <div className="mt-2 grid gap-4 lg:grid-cols-[1fr_20rem]">
              <EditorCondicao
                valor={c.condicao}
                onChange={(condicao) => trocar(i, { condicao })}
              />
              {/* o painel que se aponta para dizer "e isto é o que ela vira" */}
              <div>
                <p className="text-muted-foreground mb-1 text-xs">
                  a mesma condição, como dado
                </p>
                <JsonDaCondicao no={c.condicao} />
              </div>
            </div>
          </div>
        ))}
      </div>

      <datalist id="acoes-conhecidas">
        {(cenario?.acoes ?? []).map((a) => (
          <option key={a} value={a} />
        ))}
      </datalist>
      <datalist id="recursos-conhecidos">
        {(cenario?.tipos ?? []).map((t) => (
          <option key={t} value={t} />
        ))}
        {(cenario?.recursos ?? []).map((r) => (
          <option key={r.ref} value={r.ref} />
        ))}
      </datalist>

      {erro && <p className="text-destructive mt-3 text-sm">{erro}</p>}

      <div className="mt-4 flex gap-2">
        <Button
          size="sm"
          variant="outline"
          onClick={() => setClausulas([...clausulas, { ...NOVA }])}
        >
          <Plus className="mr-1 h-3 w-3" /> cláusula
        </Button>
        <div className="ml-auto flex gap-2">
          <Button size="sm" variant="ghost" onClick={onCancelar}>
            cancelar
          </Button>
          <Button
            size="sm"
            onClick={async () => {
              setErro('')
              if (!nome.trim()) return setErro('A política precisa de um nome')
              try {
                await salvarPolitica(
                  nome.trim(),
                  clausulas.map(
                    (c) =>
                      ({
                        sid: c.sid || undefined,
                        effect: c.effect,
                        action: c.action,
                        resource: c.resource,
                        condition: paraDocumento(c.condicao),
                      }) as unknown as Statement,
                  ),
                )
                await onSalvo()
              } catch (e) {
                setErro(String(e instanceof Error ? e.message : e))
              }
            }}
          >
            salvar
          </Button>
        </div>
      </div>
    </div>
  )
}
