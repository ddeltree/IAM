import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import useSWR from 'swr'
import { carregarCenario } from '@/lib/api'
import type { Cenario, Principal } from '@/lib/types'

type Contexto = {
  cenario: Cenario | undefined
  carregando: boolean
  erro: unknown
  /** Recarrega tudo. Chamado depois de qualquer mudança. */
  recarregar: () => Promise<unknown>
  /** De quem são as perguntas. O console não tem login. */
  identidade: Principal | undefined
  identidadeId: string
  escolherIdentidade: (id: string) => void
  /** Todos os principais que podem responder: usuários, papéis e sessões. */
  principais: Principal[]
}

const CenarioContext = createContext<Contexto | null>(null)

const CHAVE = 'console-iam:identidade'

export function CenarioProvider({ children }: { children: ReactNode }) {
  const { data, error, isLoading, mutate } = useSWR('cenario', carregarCenario)
  const [identidadeId, setIdentidadeId] = useState(
    () => localStorage.getItem(CHAVE) ?? '',
  )

  const principais = useMemo(
    () => [
      ...(data?.usuarios ?? []),
      ...(data?.grupos ?? []),
      ...(data?.papeis ?? []),
      ...(data?.sessoes ?? []),
    ],
    [data],
  )

  // a identidade guardada pode não existir mais — o cenário foi reiniciado, o
  // usuário foi apagado, a sessão foi largada. Cair no primeiro usuário é
  // melhor do que a tela inteira responder "principal desconhecido"
  const identidade =
    principais.find((p) => p.id === identidadeId) ?? data?.usuarios?.[0]

  const escolherIdentidade = useCallback((id: string) => {
    localStorage.setItem(CHAVE, id)
    setIdentidadeId(id)
  }, [])

  const valor: Contexto = {
    cenario: data,
    carregando: isLoading,
    erro: error,
    recarregar: () => mutate(),
    identidade,
    identidadeId: identidade?.id ?? '',
    escolherIdentidade,
    principais,
  }

  return (
    <CenarioContext.Provider value={valor}>{children}</CenarioContext.Provider>
  )
}

export function useCenario() {
  const contexto = useContext(CenarioContext)
  if (!contexto) throw new Error('useCenario fora do CenarioProvider')
  return contexto
}
