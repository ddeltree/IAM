import useSWR from 'swr'
import { consultarPermissoes } from '@/lib/api'
import { useSessao } from '@/providers/SessaoProvider'
import type { RefRecurso } from '@/lib/types'

/**
 * Pergunta ao backend o que o usuário pode fazer, em lote.
 *
 * Antes existia um `lib/permissoes.ts` que reescrevia as regras do servidor em
 * TypeScript. Eram duas fontes de verdade que podiam divergir em silêncio;
 * agora existe uma só, e ela é o motor de permissões do backend.
 *
 * `pode` devolve `false` enquanto carrega — a tela falha fechada, escondendo a
 * ação em vez de mostrá-la desabilitada e depois habilitá-la, que pisca.
 */
/** A consulta de permissões, para ser repassada a componentes filhos. */
export type Pode = (acao: string, recurso?: RefRecurso) => boolean

export function usePermissoes(recursos: readonly RefRecurso[] = []) {
  const { sessao } = useSessao()
  // ordenar mantém a chave estável quando a mesma tela pede os mesmos recursos
  const refs = [...recursos].sort()

  const { data, error, isLoading, mutate } = useSWR(
    sessao ? [sessao.id, 'permissoes', refs.join(',')] : null,
    () => consultarPermissoes(refs),
  )

  /**
   * @param acao nome da ação, como `EDITAR_POST`
   * @param recurso a referência do recurso; omita para as ações sem alvo
   */
  const pode: Pode = (acao, recurso) => {
    if (!data) return false
    const mapa = recurso ? data.recursos[recurso] : data.global
    return mapa?.[acao] === true
  }

  return {
    pode,
    principal: data?.principal,
    error,
    carregando: isLoading,
    mutate,
  }
}
