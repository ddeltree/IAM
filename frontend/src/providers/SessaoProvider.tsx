import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import { useSWRConfig } from 'swr'
import { consultarPermissoes } from '@/lib/api'
import { lembrar } from '@/lib/conhecidos'
import type { Sessao } from '@/lib/types'

const CHAVE = 'sala:sessao'

const SessaoContext = createContext<{
  sessao: Sessao | null
  entrar: (id: string) => Promise<Sessao>
  sair: () => void
}>({
  sessao: null,
  entrar: async () => {
    throw new Error('SessaoProvider ausente')
  },
  sair: () => {},
})

function gravarCookie(id: string) {
  document.cookie = `uid=${encodeURIComponent(id)}; path=/; SameSite=Lax`
}

function apagarCookie() {
  document.cookie = 'uid=; path=/; Max-Age=0; SameSite=Lax'
}

function lerSessaoGuardada(): Sessao | null {
  try {
    const bruto = sessionStorage.getItem(CHAVE)
    return bruto ? (JSON.parse(bruto) as Sessao) : null
  } catch {
    return null
  }
}

/**
 * Descobre quem é o dono de um id.
 *
 * O backend não tem rota de login, mas `GET /permissoes` devolve o principal
 * autenticado junto com o que ele pode fazer — e responde 404 se o cookie
 * apontar para um id que não existe. Antes isto era deduzido de um 403 em
 * `/usuarios/{id}`, o que funcionava por efeito colateral das permissões;
 * agora é uma pergunta direta.
 */
export async function identificarSessao(): Promise<Sessao> {
  const { principal } = await consultarPermissoes()
  return { id: principal.id, name: principal.name, papel: principal.papel }
}

export function SessaoProvider({ children }: { children: ReactNode }) {
  // Inicialização síncrona: com useEffect, a primeira renderização sairia sem
  // sessão e as telas disparariam requisições sem cookie.
  const [sessao, setSessao] = useState<Sessao | null>(lerSessaoGuardada)
  const { mutate } = useSWRConfig()

  const limparCache = useCallback(
    () => mutate(() => true, undefined, { revalidate: false }),
    [mutate],
  )

  // O cookie e o sessionStorage têm o mesmo tempo de vida, mas o cookie some se
  // for apagado à mão. Reescrevê-lo no boot evita a sessão fantasma que só dá 401.
  useEffect(() => {
    if (sessao) gravarCookie(sessao.id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const sair = useCallback(() => {
    apagarCookie()
    sessionStorage.removeItem(CHAVE)
    setSessao(null)
    limparCache()
  }, [limparCache])

  const entrar = useCallback(
    async (id: string) => {
      gravarCookie(id)
      try {
        const nova = await identificarSessao()
        sessionStorage.setItem(CHAVE, JSON.stringify(nova))
        lembrar(nova)
        await limparCache()
        setSessao(nova)
        return nova
      } catch (erro) {
        apagarCookie() // um id inválido não pode ficar grudado no navegador
        throw erro
      }
    },
    [limparCache],
  )

  // O cliente de API emite este evento em qualquer 401.
  useEffect(() => {
    window.addEventListener('sessao-expirada', sair)
    return () => window.removeEventListener('sessao-expirada', sair)
  }, [sair])

  return (
    <SessaoContext.Provider value={{ sessao, entrar, sair }}>
      {children}
    </SessaoContext.Provider>
  )
}

export const useSessao = () => useContext(SessaoContext)
