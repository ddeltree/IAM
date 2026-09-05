import { useState } from 'react'
import useSWR from 'swr'
import { Check, X } from 'lucide-react'
import { simular } from '@/lib/api'
import { resumir } from '@/lib/condicao'
import { useCenario } from '@/providers/CenarioProvider'
import type { ClausulaAvaliada } from '@/lib/types'
import Titulo from './Titulo'
import SeletorDePermissao from './SeletorDePermissao'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

/**
 * O simulador — o `SimulatePrincipalPolicy` da AWS.
 *
 * A resposta tem três blocos, e o terceiro é o que faz o desenho ficar visível:
 * sem as chaves resolvidas, "a condição não passou" não diz qual valor foi
 * comparado com qual.
 */
export default function Simulador() {
  const { cenario, identidade } = useCenario()
  const [permissao, setPermissao] = useState<{
    acao: string
    tipo: string
  } | null>(null)
  const [recurso, setRecurso] = useState<string>('')
  const [chave, setChave] = useState('')
  const [valor, setValor] = useState('')

  const doTipo = (cenario?.recursos ?? []).filter(
    (r) => !permissao || r.tipo === permissao.tipo,
  )

  const pergunta =
    identidade && permissao
      ? {
          principal: identidade.id,
          acao: permissao.acao,
          tipo: permissao.tipo,
          recurso: recurso || null,
          chaves: chave ? { [chave]: valor } : undefined,
        }
      : null

  const { data: resposta, error } = useSWR(
    pergunta ? ['simular', JSON.stringify(pergunta)] : null,
    () => simular(pergunta!),
  )

  return (
    <>
      <Titulo
        titulo="Simulador"
        explicacao={
          <>
            Pode? E, principalmente, <strong>por que não</strong>. A decisão
            sozinha diz sim ou não; aqui aparecem todas as cláusulas que falam
            sobre a ação, o que houve com cada uma, e o contexto inteiro sobre o
            qual as condições decidiram.
          </>
        }
      />

      <div className="bg-card mb-6 grid gap-4 rounded-lg border p-4 md:grid-cols-4">
        <div className="grid gap-1">
          <Label>Principal</Label>
          <p className="flex h-9 items-center text-sm">
            {identidade ? identidade.nome : '—'}
            <span className="text-muted-foreground ml-2 text-xs">
              (troque no topo da tela)
            </span>
          </p>
        </div>

        <div className="grid gap-1 md:col-span-2">
          <Label>Permissão</Label>
          <SeletorDePermissao valor={permissao} onChange={setPermissao} />
        </div>

        <div className="grid gap-1">
          <Label>Recurso</Label>
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

        <div className="grid gap-1 md:col-span-4">
          <Label className="text-muted-foreground text-xs">
            Chave de requisição (opcional) — o que só quem chama sabe: origem,
            horário, cabeçalho
          </Label>
          <div className="flex gap-2">
            <Input
              className="w-64"
              placeholder="ip"
              value={chave}
              onChange={(e) => setChave(e.target.value)}
            />
            <Input
              className="w-64"
              placeholder="10.0.0.1"
              value={valor}
              onChange={(e) => setValor(e.target.value)}
            />
          </div>
        </div>
      </div>

      {!permissao && (
        <p className="text-muted-foreground">
          Escolha uma permissão para simular.
        </p>
      )}
      {error != null && <p className="text-destructive">{String(error)}</p>}

      {resposta && (
        <div className="space-y-6">
          {/* 1. a decisão */}
          <section
            className={
              'rounded-lg border-2 p-4 ' +
              (resposta.permitido
                ? 'border-emerald-500/50 bg-emerald-500/5'
                : 'border-destructive/40 bg-destructive/5')
            }
          >
            <div className="flex items-center gap-2">
              {resposta.permitido ? (
                <Check className="h-5 w-5 text-emerald-600" />
              ) : (
                <X className="text-destructive h-5 w-5" />
              )}
              <strong className="text-lg">
                {resposta.permitido ? 'Permitido' : 'Negado'}
              </strong>
              <span className="bg-muted rounded px-2 py-0.5 font-mono text-xs">
                {resposta.tipo}
              </span>
            </div>
            <p className="text-muted-foreground mt-2 text-sm">
              {resposta.motivo}
            </p>
            {resposta.tipo === 'NEGACAO_PADRAO' && (
              <p className="text-muted-foreground mt-1 text-xs">
                Ninguém negou explicitamente — simplesmente nada concedeu. O
                padrão do modelo é negar.
              </p>
            )}
          </section>

          {/* 2. as cláusulas consideradas */}
          <section>
            <h2 className="mb-1 font-semibold">Cláusulas consideradas</h2>
            <p className="text-muted-foreground mb-3 text-sm">
              {resposta.clausulas.length} de {resposta.clausulasAlcancadas}{' '}
              cláusulas alcançadas falam sobre esta ação.
              {resposta.clausulas.length === 0 &&
                ' Nenhuma menciona esta ação — o problema não está em condição nenhuma.'}
            </p>
            <div className="space-y-2">
              {resposta.clausulas.map((c, i) => (
                <Clausula key={i} clausula={c} />
              ))}
            </div>
          </section>

          {/* 3. o contexto — o bloco que faz o desenho ficar visível */}
          <section>
            <h2 className="mb-1 font-semibold">Contexto resolvido</h2>
            <p className="text-muted-foreground mb-3 text-sm">
              As chaves sobre as quais as condições decidiram. É aqui que se vê
              qual valor foi comparado com qual.
            </p>
            <div className="bg-card overflow-hidden rounded-lg border">
              <table className="w-full text-sm">
                <tbody>
                  {Object.entries(resposta.contexto).map(([chave, valores]) => (
                    <tr key={chave} className="border-b last:border-0">
                      <td className="text-muted-foreground w-72 px-4 py-1.5 font-mono text-xs">
                        {chave}
                      </td>
                      <td className="px-4 py-1.5 font-mono text-xs">
                        {valores.length === 0 ? (
                          <span className="text-muted-foreground italic">
                            vazia
                          </span>
                        ) : (
                          valores.join(', ')
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}
    </>
  )
}

function Clausula({ clausula }: { clausula: ClausulaAvaliada }) {
  const estado = !clausula.alcancaORecurso
    ? { texto: 'mira outro recurso', cor: 'text-muted-foreground' }
    : clausula.condicaoPassou
      ? { texto: 'condição passou', cor: 'text-emerald-600' }
      : { texto: 'condição não passou', cor: 'text-amber-600' }

  return (
    <div
      className={
        'bg-card rounded-lg border p-3 ' +
        (clausula.decisiva ? 'border-primary' : '')
      }
    >
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span
          className={
            'rounded px-1.5 py-0.5 font-mono text-xs ' +
            (clausula.effect === 'ALLOW'
              ? 'bg-emerald-500/15 text-emerald-700'
              : 'bg-destructive/15 text-destructive')
          }
        >
          {clausula.effect}
        </span>
        <code className="text-xs">{clausula.action}</code>
        <span className="text-muted-foreground text-xs">sobre</span>
        <code className="text-xs">{clausula.resource}</code>
        <span className="text-muted-foreground ml-auto text-xs">
          de <strong>{clausula.origem}</strong>
        </span>
        {clausula.decisiva && (
          <span className="bg-primary text-primary-foreground rounded px-1.5 py-0.5 text-xs">
            decidiu
          </span>
        )}
      </div>
      <p className="text-muted-foreground mt-1.5 text-xs">
        <span className={estado.cor}>{estado.texto}</span>
        {' · condição: '}
        <code>{resumir(clausula.condition)}</code>
      </p>
    </div>
  )
}
