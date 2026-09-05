import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { apagarRecurso, salvarRecurso } from '@/lib/api'
import { resumir } from '@/lib/condicao'
import { useCenario } from '@/providers/CenarioProvider'
import type { Recurso } from '@/lib/types'
import Titulo from './Titulo'
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

type Atributo = { chave: string; valor: string }

export default function Recursos() {
  const { cenario, recarregar } = useCenario()
  const [tipo, setTipo] = useState('')
  const [id, setId] = useState('')
  const [pai, setPai] = useState('')
  const [atributos, setAtributos] = useState<Atributo[]>([
    { chave: '', valor: '' },
  ])
  const [erro, setErro] = useState('')

  const salvar = async () => {
    setErro('')
    if (!tipo.trim() || !id.trim()) return setErro('Informe o tipo e o id')
    try {
      await salvarRecurso(tipo.trim().toUpperCase(), id.trim(), {
        atributos: Object.fromEntries(
          atributos.filter((a) => a.chave).map((a) => [a.chave, a.valor]),
        ),
        pai: pai || null,
      })
      setId('')
      setAtributos([{ chave: '', valor: '' }])
      await recarregar()
    } catch (e) {
      setErro(String(e instanceof Error ? e.message : e))
    }
  }

  return (
    <>
      <Titulo
        titulo="Recursos"
        explicacao={
          <>
            Os objetos sobre os quais se decide, com os atributos que{' '}
            <strong>você</strong> criar. É a diferença de fundo para uma
            aplicação comum: lá "os atributos de uma turma" é uma decisão
            escrita em código, e uma condição só pode falar do que alguém previu
            antes.
          </>
        }
      />

      <div className="bg-card mb-6 rounded-lg border p-4">
        <div className="mb-3 flex flex-wrap items-end gap-3">
          <div className="grid gap-1">
            <Label className="text-xs">Tipo</Label>
            <Input
              list="tipos-conhecidos"
              className="h-8 w-40"
              value={tipo}
              onChange={(e) => setTipo(e.target.value)}
              placeholder="BUCKET"
            />
          </div>
          <div className="grid gap-1">
            <Label className="text-xs">Id</Label>
            <Input
              className="h-8 w-48"
              value={id}
              onChange={(e) => setId(e.target.value)}
              placeholder="relatorios"
            />
          </div>
          <div className="grid gap-1">
            <Label className="text-xs">Dentro de (opcional)</Label>
            <Select
              value={pai || 'nenhum'}
              onValueChange={(v) => setPai(v === 'nenhum' ? '' : v)}
            >
              <SelectTrigger className="h-8 w-56">
                <SelectValue placeholder="nenhum" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="nenhum">nenhum</SelectItem>
                {(cenario?.recursos ?? []).map((r) => (
                  <SelectItem key={r.ref} value={r.ref}>
                    {r.ref}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <Label className="text-xs">Atributos</Label>
        <p className="text-muted-foreground mb-2 text-xs">
          Cada um vira uma chave de condição: <code>recurso:dono</code> e também{' '}
          <code>{(tipo || 'tipo').toLowerCase()}:dono</code>, esta última
          alcançável de dentro dos recursos que estiverem aqui.
        </p>
        <div className="space-y-2">
          {atributos.map((a, i) => (
            <div key={i} className="flex gap-2">
              <Input
                className="h-8 w-48"
                placeholder="dono"
                value={a.chave}
                onChange={(e) =>
                  setAtributos(
                    atributos.map((x, j) =>
                      j === i ? { ...x, chave: e.target.value } : x,
                    ),
                  )
                }
              />
              <Input
                className="h-8 w-64"
                placeholder="ana"
                value={a.valor}
                onChange={(e) =>
                  setAtributos(
                    atributos.map((x, j) =>
                      j === i ? { ...x, valor: e.target.value } : x,
                    ),
                  )
                }
              />
              <Button
                size="icon"
                variant="ghost"
                className="h-8 w-8"
                onClick={() =>
                  setAtributos(atributos.filter((_, j) => j !== i))
                }
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
          ))}
        </div>

        <div className="mt-3 flex gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={() =>
              setAtributos([...atributos, { chave: '', valor: '' }])
            }
          >
            <Plus className="mr-1 h-3 w-3" /> atributo
          </Button>
          <Button size="sm" className="ml-auto" onClick={salvar}>
            salvar recurso
          </Button>
        </div>
        {erro && <p className="text-destructive mt-2 text-sm">{erro}</p>}
      </div>

      <datalist id="tipos-conhecidos">
        {(cenario?.tipos ?? []).map((t) => (
          <option key={t} value={t} />
        ))}
      </datalist>

      <div className="space-y-2">
        {(cenario?.recursos ?? []).map((r) => (
          <Cartao key={r.ref} recurso={r} onApagar={recarregar} />
        ))}
      </div>
    </>
  )
}

function Cartao({
  recurso,
  onApagar,
}: {
  recurso: Recurso
  onApagar: () => Promise<unknown>
}) {
  return (
    <div className="bg-card rounded-lg border p-3">
      <div className="flex flex-wrap items-center gap-2">
        <code className="font-semibold">{recurso.ref}</code>
        {recurso.pai && (
          <span className="text-muted-foreground text-xs">
            dentro de <code>{recurso.pai}</code>
          </span>
        )}
        <Button
          size="icon"
          variant="ghost"
          className="ml-auto h-8 w-8"
          onClick={async () => {
            await apagarRecurso(recurso.ref)
            await onApagar()
          }}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>

      <div className="mt-2 flex flex-wrap gap-3 text-xs">
        {Object.entries(recurso.atributos).map(([chave, valores]) => (
          <span key={chave} className="bg-muted rounded px-2 py-0.5">
            <span className="text-muted-foreground">{chave}</span> ={' '}
            {valores.join(', ')}
          </span>
        ))}
        {Object.keys(recurso.atributos).length === 0 && (
          <span className="text-muted-foreground italic">sem atributos</span>
        )}
      </div>

      {recurso.politica && (
        <div className="mt-2 border-t pt-2">
          <p className="text-muted-foreground text-xs">
            política do próprio recurso — concede sem tocar na política de
            ninguém:
          </p>
          {recurso.politica.statements.map((s, i) => (
            <p key={i} className="mt-1 text-xs">
              <code>
                {s.effect} {s.action} sobre {s.resource}
              </code>
              {s.condition && (
                <span className="text-muted-foreground">
                  {' '}
                  se {resumir(s.condition)}
                </span>
              )}
            </p>
          ))}
        </div>
      )}
    </div>
  )
}
