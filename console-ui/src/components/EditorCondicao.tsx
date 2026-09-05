import { Plus, Trash2 } from 'lucide-react'
import {
  comparacaoVazia,
  legivel,
  nomeDoOperador,
  paraDocumento,
  type No,
} from '@/lib/condicao'
import { useVocabulario } from '@/hooks/useVocabulario'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

/**
 * A condição, montada em árvore.
 *
 * Ao lado dela vive o {@link JsonDaCondicao}, que mostra o mesmo estado como
 * documento. Os dois juntos são de propósito: a árvore é o que se opera, e o
 * documento é a prova de que a condição é dado — a propriedade que sustenta as
 * consultas reversas e que uma interface visual, sozinha, esconderia.
 */
export default function EditorCondicao({
  valor,
  onChange,
}: {
  valor: No
  onChange: (novo: No) => void
}) {
  return (
    <div className="space-y-2">
      <NoDaArvore
        no={valor}
        onChange={onChange}
        onRemover={undefined}
        nivel={0}
      />
    </div>
  )
}

function NoDaArvore({
  no,
  onChange,
  onRemover,
  nivel,
}: {
  no: No
  onChange: (novo: No) => void
  onRemover?: () => void
  nivel: number
}) {
  const { chaves, operadores, prefixos } = useVocabulario()

  const trocarTipo = (tipo: string) => {
    if (tipo === no.tipo) return
    if (tipo === 'sempre') return onChange({ tipo: 'sempre' })
    if (tipo === 'nunca') return onChange({ tipo: 'nunca' })
    if (tipo === 'comparacao') return onChange(comparacaoVazia(chaves[0] ?? ''))
    if (tipo === 'negacao')
      return onChange({ tipo: 'negacao', filho: comparacaoVazia() })
    // virar E/OU: o que estava aqui vira o primeiro filho, para não se perder
    const filhos = no.tipo === 'sempre' ? [comparacaoVazia()] : [no]
    onChange(
      tipo === 'todas' ? { tipo: 'todas', filhos } : { tipo: 'alguma', filhos },
    )
  }

  const seletorDeTipo = (
    <Select value={no.tipo} onValueChange={trocarTipo}>
      <SelectTrigger className="h-8 w-36">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="comparacao">comparação</SelectItem>
        <SelectItem value="todas">todas (E)</SelectItem>
        <SelectItem value="alguma">alguma (OU)</SelectItem>
        <SelectItem value="negacao">não</SelectItem>
        <SelectItem value="sempre">sempre</SelectItem>
        <SelectItem value="nunca">nunca</SelectItem>
      </SelectContent>
    </Select>
  )

  const botaoRemover = onRemover && (
    <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onRemover}>
      <Trash2 className="h-4 w-4" />
    </Button>
  )

  if (no.tipo === 'sempre' || no.tipo === 'nunca') {
    return (
      <div className="flex items-center gap-2">
        {seletorDeTipo}
        <span className="text-muted-foreground text-sm">
          {no.tipo === 'sempre'
            ? 'a cláusula vale sem restrição'
            : 'a cláusula está desligada, mas continua escrita'}
        </span>
        {botaoRemover}
      </div>
    )
  }

  if (no.tipo === 'comparacao') {
    // Nulo pergunta pela ausência da chave, então o campo de valor vira um
    // sim/não — deixá-lo livre convidaria a digitar qualquer coisa ali
    const ehNulo = no.operador === 'Nulo'
    return (
      <div className="flex flex-wrap items-center gap-2">
        {seletorDeTipo}

        <Select
          value={no.chave}
          onValueChange={(chave) => onChange({ ...no, chave })}
        >
          <SelectTrigger className="h-8 w-56">
            <SelectValue placeholder="chave de contexto" />
          </SelectTrigger>
          <SelectContent>
            {chaves.map((chave) => (
              <SelectItem key={chave} value={chave}>
                {chave}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={nomeDoOperador(no)}
          onValueChange={(completo) => {
            const prefixo = prefixos.find((p) => completo.startsWith(p)) ?? ''
            onChange({
              ...no,
              prefixo,
              operador: completo.slice(prefixo.length),
            })
          }}
        >
          <SelectTrigger className="h-8 w-52">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {operadores.map((op) => (
              <SelectItem key={op} value={op}>
                {legivel(op)}
              </SelectItem>
            ))}
            {prefixos.flatMap((prefixo) =>
              operadores.map((op) => (
                <SelectItem key={prefixo + op} value={prefixo + op}>
                  {legivel(prefixo + op)}
                </SelectItem>
              )),
            )}
          </SelectContent>
        </Select>

        {ehNulo ? (
          <Select
            value={no.valores[0] ?? 'true'}
            onValueChange={(v) => onChange({ ...no, valores: [v] })}
          >
            <SelectTrigger className="h-8 w-28">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="true">sim</SelectItem>
              <SelectItem value="false">não</SelectItem>
            </SelectContent>
          </Select>
        ) : (
          <div className="flex items-center gap-1">
            <Input
              className="h-8 w-56"
              value={no.valores.join(', ')}
              placeholder="valor, ou ${principal:id}"
              onChange={(e) =>
                onChange({
                  ...no,
                  valores: e.target.value.split(',').map((v) => v.trim()),
                })
              }
            />
            {/* a variável de política é o que faz uma cláusula servir a todo
                mundo, e é o que ninguém adivinha que existe */}
            <Button
              variant="outline"
              size="sm"
              className="h-8"
              title="comparar com quem está pedindo"
              onClick={() => onChange({ ...no, valores: ['${principal:id}'] })}
            >
              quem pede
            </Button>
          </div>
        )}

        {botaoRemover}
      </div>
    )
  }

  if (no.tipo === 'negacao') {
    return (
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          {seletorDeTipo}
          {botaoRemover}
        </div>
        <div className="border-muted ml-4 border-l-2 pl-4">
          <NoDaArvore
            no={no.filho}
            onChange={(filho) => onChange({ ...no, filho })}
            nivel={nivel + 1}
          />
        </div>
      </div>
    )
  }

  // todas / alguma
  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2">
        {seletorDeTipo}
        <span className="text-muted-foreground text-sm">
          {no.tipo === 'todas' ? 'todas precisam passar' : 'basta uma passar'}
        </span>
        {botaoRemover}
      </div>

      <div className="border-muted ml-4 space-y-2 border-l-2 pl-4">
        {no.filhos.map((filho, i) => (
          <NoDaArvore
            key={i}
            no={filho}
            nivel={nivel + 1}
            onChange={(novo) =>
              onChange({
                ...no,
                filhos: no.filhos.map((f, j) => (j === i ? novo : f)),
              })
            }
            onRemover={() =>
              onChange({ ...no, filhos: no.filhos.filter((_, j) => j !== i) })
            }
          />
        ))}
        <Button
          variant="outline"
          size="sm"
          onClick={() =>
            onChange({ ...no, filhos: [...no.filhos, comparacaoVazia()] })
          }
        >
          <Plus className="mr-1 h-3 w-3" /> comparação
        </Button>
      </div>
    </div>
  )
}

/**
 * O mesmo estado como documento, em leitura.
 *
 * É o painel que se aponta na tela para dizer "e isto é o que ela vira — dado,
 * não código". Sem ele, o editor visual esconderia a propriedade que torna o
 * sistema consultável.
 */
export function JsonDaCondicao({ no }: { no: No }) {
  const documento = paraDocumento(no)
  return (
    <pre className="bg-muted text-muted-foreground max-h-72 overflow-auto rounded-md p-3 text-xs">
      {documento === null
        ? '// sem bloco de condição: a cláusula vale sempre'
        : JSON.stringify(documento, null, 2)}
    </pre>
  )
}
