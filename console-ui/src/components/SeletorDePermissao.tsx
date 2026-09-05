import { useCenario } from '@/providers/CenarioProvider'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

/** As permissões declaradas no vocabulário — o par (ação, tipo). */
export default function SeletorDePermissao({
  valor,
  onChange,
}: {
  valor: { acao: string; tipo: string } | null
  onChange: (v: { acao: string; tipo: string } | null) => void
}) {
  const { cenario } = useCenario()
  const chave = valor ? `${valor.acao}|${valor.tipo}` : ''

  return (
    <Select
      value={chave}
      onValueChange={(v) => {
        const [acao, tipo] = v.split('|')
        onChange({ acao, tipo })
      }}
    >
      <SelectTrigger>
        <SelectValue placeholder="escolha a ação e o tipo" />
      </SelectTrigger>
      <SelectContent>
        {(cenario?.permissoes ?? []).map((p) => (
          <SelectItem key={`${p.acao}|${p.tipo}`} value={`${p.acao}|${p.tipo}`}>
            {p.rotulo}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
