import useSWR from 'swr'
import { chavesDisponiveis, operadoresDisponiveis } from '@/lib/api'

/**
 * As chaves e os operadores que uma condição pode usar.
 *
 * Sem esta lista, escrever uma condição seria adivinhar nomes — e chave errada
 * avalia falso em silêncio, o que parece bug e é erro de digitação. As chaves
 * mudam quando um recurso ganha um atributo novo, então revalidam junto com o
 * cenário.
 */
export function useVocabulario() {
  const { data: chaves } = useSWR('chaves', chavesDisponiveis)
  const { data: operadores } = useSWR('operadores', operadoresDisponiveis)

  return {
    chaves: chaves ?? [],
    operadores: operadores?.operadores ?? ['Igual'],
    prefixos: operadores?.prefixos ?? [],
  }
}
