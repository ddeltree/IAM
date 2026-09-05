import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { declararPermissao, removerPermissao } from '@/lib/api'
import { useCenario } from '@/providers/CenarioProvider'
import { useVocabulario } from '@/hooks/useVocabulario'
import Titulo from './Titulo'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

/**
 * O vocabulário: quais ações existem, sobre que tipos.
 *
 * Declarar o par, e não cruzar tudo automaticamente, é decisão: o produto
 * cartesiano encheria o simulador e as permissões efetivas de perguntas sem
 * sentido, e o catálogo deixaria de ser uma lista do que existe para virar uma
 * lista do que é combinável.
 */
export default function Vocabulario() {
  const { cenario, recarregar } = useCenario()
  const { chaves } = useVocabulario()
  const [acao, setAcao] = useState('')
  const [tipo, setTipo] = useState('')
  const [semAlvo, setSemAlvo] = useState(false)
  const [erro, setErro] = useState('')

  return (
    <>
      <Titulo
        titulo="Vocabulário"
        explicacao={
          <>
            O núcleo não conhece nenhuma ação nem nenhum tipo de recurso — ele
            compara nomes. Tudo o que estiver aqui foi digitado, e é isso que
            faz este console servir a qualquer domínio.
          </>
        }
      />

      <div className="grid gap-8 lg:grid-cols-2">
        <section>
          <h2 className="mb-3 font-semibold">Permissões declaradas</h2>

          <form
            className="bg-card mb-4 flex flex-wrap items-end gap-3 rounded-lg border p-4"
            onSubmit={async (e) => {
              e.preventDefault()
              setErro('')
              if (!acao.trim() || !tipo.trim())
                return setErro('Informe a ação e o tipo')
              try {
                await declararPermissao(
                  acao.trim(),
                  tipo.trim().toUpperCase(),
                  semAlvo,
                )
                setAcao('')
                await recarregar()
              } catch (err) {
                setErro(String(err instanceof Error ? err.message : err))
              }
            }}
          >
            <div className="grid gap-1">
              <Label className="text-xs">Ação</Label>
              <Input
                className="h-8 w-44"
                value={acao}
                onChange={(e) => setAcao(e.target.value)}
                placeholder="LER"
              />
            </div>
            <div className="grid gap-1">
              <Label className="text-xs">Sobre o tipo</Label>
              <Input
                className="h-8 w-44"
                value={tipo}
                onChange={(e) => setTipo(e.target.value)}
                placeholder="BUCKET"
              />
            </div>
            <label className="flex h-8 items-center gap-2 text-xs">
              <input
                type="checkbox"
                checked={semAlvo}
                onChange={(e) => setSemAlvo(e.target.checked)}
              />
              se pede sem alvo
            </label>
            <Button type="submit" size="sm">
              <Plus className="mr-1 h-3 w-3" /> declarar
            </Button>
          </form>
          {erro && <p className="text-destructive mb-3 text-sm">{erro}</p>}

          <div className="space-y-1">
            {(cenario?.permissoes ?? []).map((p) => (
              <div
                key={p.rotulo}
                className="bg-card flex items-center gap-2 rounded border px-3 py-1.5 text-sm"
              >
                <code className="text-xs">{p.acao}</code>
                <span className="text-muted-foreground text-xs">sobre</span>
                <code className="text-xs">{p.tipo}</code>
                {cenario?.semAlvo.some((s) => s.rotulo === p.rotulo) && (
                  <span className="bg-muted rounded px-1.5 py-0.5 text-xs">
                    sem alvo
                  </span>
                )}
                <Button
                  size="icon"
                  variant="ghost"
                  className="ml-auto h-7 w-7"
                  onClick={async () => {
                    await removerPermissao(p.acao, p.tipo)
                    await recarregar()
                  }}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
            ))}
          </div>
        </section>

        <section>
          <h2 className="mb-1 font-semibold">Chaves de contexto</h2>
          <p className="text-muted-foreground mb-3 text-sm">
            O que uma condição pode ler. As de <code>recurso:</code> e as de
            cada tipo aparecem sozinhas conforme você cria recursos com
            atributos.
          </p>
          <div className="bg-card grid gap-1 rounded-lg border p-3">
            {chaves.map((chave) => (
              <code key={chave} className="text-xs">
                {chave}
              </code>
            ))}
          </div>
        </section>
      </div>
    </>
  )
}
