import { useState } from 'react'
import useSWR from 'swr'
import { ondePosso, quemPode } from '@/lib/api'
import { useCenario } from '@/providers/CenarioProvider'
import Titulo from './Titulo'
import SeletorDePermissao from './SeletorDePermissao'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

/**
 * As duas perguntas ao contrário. É por elas que este sistema se distingue de um
 * CRUD de permissões — nenhum sistema comum responde nenhuma das duas.
 */
export default function Consultas() {
  return (
    <>
      <Titulo
        titulo="Consultas"
        explicacao={
          <>
            Um sistema de permissões comum responde "pode?". Estas duas são o
            contrário: <strong>quem</strong> pode uma coisa, e{' '}
            <strong>sobre o que</strong> alguém pode agir — esta última
            devolvendo um filtro, e não uma lista.
          </>
        }
      />
      <div className="grid gap-8 lg:grid-cols-2">
        <QuemPode />
        <OndePosso />
      </div>
    </>
  )
}

function QuemPode() {
  const { cenario } = useCenario()
  const [permissao, setPermissao] = useState<{
    acao: string
    tipo: string
  } | null>(null)
  const [recurso, setRecurso] = useState('')

  const { data, error } = useSWR(
    permissao ? ['quem-pode', permissao.acao, permissao.tipo, recurso] : null,
    () => quemPode(permissao!.acao, permissao!.tipo, recurso || undefined),
  )

  const doTipo = (cenario?.recursos ?? []).filter(
    (r) => !permissao || r.tipo === permissao.tipo,
  )

  return (
    <section className="bg-card rounded-lg border p-4">
      <h2 className="mb-1 font-semibold">Quem pode isto?</h2>
      <p className="text-muted-foreground mb-4 text-sm">
        Percorre a política de todo mundo. A poda descarta quem cláusula nenhuma
        alcança, e o motor confirma cada sobrevivente — errar a poda custa
        desempenho, nunca acesso indevido.
      </p>

      <div className="mb-4 grid gap-3">
        <div className="grid gap-1">
          <Label className="text-xs">Permissão</Label>
          <SeletorDePermissao valor={permissao} onChange={setPermissao} />
        </div>
        <div className="grid gap-1">
          <Label className="text-xs">Recurso</Label>
          <Select
            value={recurso || 'nenhum'}
            onValueChange={(v) => setRecurso(v === 'nenhum' ? '' : v)}
          >
            <SelectTrigger>
              <SelectValue placeholder="sem alvo" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="nenhum">sem alvo</SelectItem>
              {doTipo.map((r) => (
                <SelectItem key={r.ref} value={r.ref}>
                  {r.ref}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {error != null && (
        <p className="text-destructive text-sm">{String(error)}</p>
      )}

      {data && (
        <div className="space-y-4">
          <div>
            <p className="mb-1 text-sm font-medium">Podem agora</p>
            {data.principais.length === 0 ? (
              <p className="text-muted-foreground text-sm">Ninguém.</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {data.principais.map((p) => (
                  <span
                    key={p.id}
                    className="bg-secondary rounded px-2 py-0.5 text-sm"
                  >
                    {p.nome}
                  </span>
                ))}
              </div>
            )}
          </div>

          {Object.keys(data.viaPapel).length > 0 && (
            <div>
              <p className="mb-1 text-sm font-medium">
                Chegariam assumindo um papel
              </p>
              {/* separado de propósito: juntar com os de cima superestimaria o
                  acesso atual numa auditoria */}
              <p className="text-muted-foreground mb-2 text-xs">
                Não podem agora. Passariam a poder se assumissem o papel.
              </p>
              {Object.entries(data.viaPapel).map(([papel, usuarios]) => (
                <p key={papel} className="text-sm">
                  <code className="text-xs">{papel}</code>:{' '}
                  {usuarios.map((u) => u.nome).join(', ')}
                </p>
              ))}
            </div>
          )}

          <p className="text-muted-foreground border-t pt-2 text-xs">
            {data.podou
              ? `a poda considerou ${data.avaliados} de ${data.conhecidos} principais`
              : `sem poda possível: ${data.avaliados} principais avaliados`}
          </p>
        </div>
      )}
    </section>
  )
}

function OndePosso() {
  const { cenario, identidade } = useCenario()
  const [permissao, setPermissao] = useState<{
    acao: string
    tipo: string
  } | null>(null)

  const { data, error } = useSWR(
    identidade && permissao
      ? ['onde-posso', identidade.id, permissao.acao, permissao.tipo]
      : null,
    () => ondePosso(identidade!.id, permissao!.acao, permissao!.tipo),
  )

  const total = (cenario?.recursos ?? []).filter(
    (r) => r.tipo === permissao?.tipo,
  ).length

  return (
    <section className="bg-card rounded-lg border p-4">
      <h2 className="mb-1 font-semibold">Sobre o que posso agir?</h2>
      <p className="text-muted-foreground mb-4 text-sm">
        Não devolve uma lista: devolve o <strong>filtro</strong> que a política
        produz. O mesmo filtro vira um predicado em memória ou uma cláusula{' '}
        <code>WHERE</code> — e é essa simetria que permitiria a política valer
        no banco sem mudar nada.
      </p>

      <div className="mb-4 grid gap-1">
        <Label className="text-xs">
          Permissão · perguntando como{' '}
          <strong>{identidade?.nome ?? '—'}</strong>
        </Label>
        <SeletorDePermissao valor={permissao} onChange={setPermissao} />
      </div>

      {error != null && (
        <p className="text-destructive text-sm">{String(error)}</p>
      )}

      {data && (
        <div className="space-y-4">
          <div>
            <p className="mb-1 text-sm font-medium">
              A restrição derivada da política
            </p>
            <pre className="bg-muted overflow-auto rounded p-2 text-xs">
              {data.restricao}
            </pre>
          </div>

          <div>
            <p className="mb-1 text-sm font-medium">
              A mesma restrição, em SQL
            </p>
            <pre className="bg-muted overflow-auto rounded p-2 text-xs">
              SELECT * FROM recurso WHERE {data.sql}
            </pre>
          </div>

          <div>
            <p className="mb-1 text-sm font-medium">
              Os recursos que ela alcança ({data.recursos.length} de {total})
            </p>
            {data.recursos.length === 0 ? (
              <p className="text-muted-foreground text-sm">Nenhum.</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {data.recursos.map((ref) => (
                  <code
                    key={ref}
                    className="bg-secondary rounded px-2 py-0.5 text-xs"
                  >
                    {ref}
                  </code>
                ))}
              </div>
            )}
            {data.candidatos.length !== data.recursos.length && (
              <p className="text-muted-foreground mt-2 text-xs">
                O filtro deixou passar {data.candidatos.length}; o motor
                confirmou {data.recursos.length}. É a regra funcionando: o
                filtro escolhe candidatos, quem decide é o motor.
              </p>
            )}
          </div>
        </div>
      )}
    </section>
  )
}
