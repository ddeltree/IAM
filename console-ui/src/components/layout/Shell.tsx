import { NavLink, Outlet } from 'react-router'
import { useState } from 'react'
import { largarSessao, reiniciarCenario } from '@/lib/api'
import { useCenario } from '@/providers/CenarioProvider'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const ABAS = [
  { para: '/simulador', rotulo: 'Simulador' },
  { para: '/consultas', rotulo: 'Consultas' },
  { para: '/principais', rotulo: 'Principais' },
  { para: '/politicas', rotulo: 'Políticas' },
  { para: '/recursos', rotulo: 'Recursos' },
  { para: '/vocabulario', rotulo: 'Vocabulário' },
]

export default function Shell() {
  const {
    cenario,
    carregando,
    erro,
    recarregar,
    identidade,
    escolherIdentidade,
  } = useCenario()
  const [reiniciando, setReiniciando] = useState(false)

  const ehSessao = identidade?.tipo === 'SESSAO'

  return (
    <div className="bg-background text-foreground min-h-screen">
      <header className="bg-card sticky top-0 z-10 border-b">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-4 px-6 py-3">
          <div>
            <p className="text-lg font-semibold">Console IAM</p>
            <p className="text-muted-foreground text-xs">
              o núcleo de autorização, com o vocabulário que você criar
            </p>
          </div>

          <div className="ml-auto flex items-center gap-2">
            {/* Não há login. Isto define de quem são todas as perguntas da
                tela, e por isso fica grande e sempre visível — sem ele, "pode?"
                não é uma pergunta completa. */}
            <span className="text-muted-foreground text-sm">
              perguntando como
            </span>
            <Select
              value={identidade?.id ?? ''}
              onValueChange={escolherIdentidade}
            >
              <SelectTrigger className="w-56">
                <SelectValue placeholder="escolha um principal" />
              </SelectTrigger>
              <SelectContent>
                <Grupo rotulo="Usuários" itens={cenario?.usuarios} />
                <Grupo rotulo="Papéis" itens={cenario?.papeis} />
                <Grupo rotulo="Sessões" itens={cenario?.sessoes} />
              </SelectContent>
            </Select>

            <Button
              variant="outline"
              size="sm"
              disabled={reiniciando}
              onClick={async () => {
                setReiniciando(true)
                try {
                  await reiniciarCenario()
                  await recarregar()
                } finally {
                  setReiniciando(false)
                }
              }}
            >
              Reiniciar cenário
            </Button>
          </div>
        </div>

        <nav className="mx-auto flex max-w-6xl gap-1 px-6">
          {ABAS.map((aba) => (
            <NavLink
              key={aba.para}
              to={aba.para}
              className={({ isActive }) =>
                'border-b-2 px-3 py-2 text-sm transition-colors ' +
                (isActive
                  ? 'border-primary font-medium'
                  : 'text-muted-foreground hover:text-foreground border-transparent')
              }
            >
              {aba.rotulo}
            </NavLink>
          ))}
        </nav>
      </header>

      {ehSessao && (
        <div className="bg-accent text-accent-foreground border-b">
          <div className="mx-auto flex max-w-6xl items-center gap-3 px-6 py-2 text-sm">
            <span>
              Você está exercendo o papel <strong>{identidade.papel}</strong>{' '}
              como <strong>{identidade.origem}</strong>. As condições sobre{' '}
              <code className="text-xs">principal:id</code> passam a comparar
              com o id desta sessão, e não com o seu.
            </span>
            <Button
              size="sm"
              variant="outline"
              className="ml-auto"
              onClick={async () => {
                await largarSessao(identidade.id)
                escolherIdentidade(identidade.origem ?? '')
                await recarregar()
              }}
            >
              Largar sessão
            </Button>
          </div>
        </div>
      )}

      <main className="mx-auto max-w-6xl px-6 py-6">
        {erro != null && (
          <p className="text-destructive">
            Não consegui falar com o console. Ele está rodando na porta 7001?
          </p>
        )}
        {carregando && (
          <p className="text-muted-foreground">carregando o cenário…</p>
        )}
        {cenario && <Outlet />}
      </main>
    </div>
  )
}

function Grupo({
  rotulo,
  itens,
}: {
  rotulo: string
  itens: { id: string; nome: string }[] | undefined
}) {
  if (!itens?.length) return null
  return (
    <SelectGroup>
      <SelectLabel>{rotulo}</SelectLabel>
      {itens.map((item) => (
        <SelectItem key={item.id} value={item.id}>
          {item.nome}
        </SelectItem>
      ))}
    </SelectGroup>
  )
}
