import type { CondicaoJson } from './types'

/**
 * A condição como a tela a manipula.
 *
 * O documento é um objeto de operadores; editar isso diretamente seria mexer em
 * chaves de um mapa. Aqui ela é uma árvore com um nó por caso, que é o que
 * permite um componente recursivo e botões de "adicionar comparação".
 */
export type No =
  | { tipo: 'sempre' }
  | { tipo: 'nunca' }
  | {
      tipo: 'comparacao'
      prefixo: string
      operador: string
      chave: string
      valores: string[]
    }
  | { tipo: 'todas'; filhos: No[] }
  | { tipo: 'alguma'; filhos: No[] }
  | { tipo: 'negacao'; filho: No }

export const SEMPRE: No = { tipo: 'sempre' }

export function comparacaoVazia(chave = ''): No {
  return {
    tipo: 'comparacao',
    prefixo: '',
    operador: 'Igual',
    chave,
    valores: [''],
  }
}

/** O nome completo do operador: o prefixo é decorador, e eles se compõem. */
export function nomeDoOperador(no: Extract<No, { tipo: 'comparacao' }>) {
  return no.prefixo + no.operador
}

// ---------- árvore → documento ----------

export function paraDocumento(no: No): CondicaoJson {
  switch (no.tipo) {
    case 'sempre':
      return null
    case 'nunca':
      return { Nunca: {} }
    case 'comparacao':
      return {
        [nomeDoOperador(no)]: {
          [no.chave]: no.valores.filter((v) => v !== ''),
        },
      }
    case 'todas':
      return { TodasAs: no.filhos.map((f) => paraDocumento(f) ?? {}) }
    case 'alguma':
      return { AlgumaDas: no.filhos.map((f) => paraDocumento(f) ?? {}) }
    case 'negacao':
      return { Negacao: paraDocumento(no.filho) ?? {} }
  }
}

// ---------- documento → árvore ----------

const PREFIXOS = ['ParaAlgumValor:', 'ParaTodoValor:', 'SeExistir:']

export function doDocumento(documento: unknown): No {
  if (!documento || typeof documento !== 'object') return SEMPRE

  const entradas = Object.entries(documento as Record<string, unknown>)
  if (entradas.length === 0) return SEMPRE

  // várias entradas no mesmo bloco são um E, como na AWS
  if (entradas.length > 1) {
    return {
      tipo: 'todas',
      filhos: entradas.map(([k, v]) => doDocumento({ [k]: v })),
    }
  }

  const [nome, valor] = entradas[0]
  if (nome === 'Nunca') return { tipo: 'nunca' }
  if (nome === 'Negacao') return { tipo: 'negacao', filho: doDocumento(valor) }
  if (nome === 'TodasAs' || nome === 'AlgumaDas') {
    const filhos = Array.isArray(valor) ? valor.map(doDocumento) : []
    return nome === 'TodasAs'
      ? { tipo: 'todas', filhos }
      : { tipo: 'alguma', filhos }
  }

  // uma comparação: { operador: { chave: [valores] } }
  const porChave = Object.entries((valor ?? {}) as Record<string, unknown>)
  const [chave, valores] = porChave[0] ?? ['', []]
  const prefixo = PREFIXOS.find((p) => nome.startsWith(p)) ?? ''

  const comparacao: No = {
    tipo: 'comparacao',
    prefixo,
    operador: nome.slice(prefixo.length),
    chave,
    valores: Array.isArray(valores) ? valores.map(String) : [String(valores)],
  }

  // mais de uma chave no mesmo operador também é um E
  if (porChave.length > 1) {
    return {
      tipo: 'todas',
      filhos: porChave.map(([k, v]) => doDocumento({ [nome]: { [k]: v } })),
    }
  }
  return comparacao
}

/** Uma descrição em uma linha, para listas onde a árvore não cabe. */
export function resumir(documento: unknown): string {
  const no = doDocumento(documento)
  return descrever(no)
}

function descrever(no: No): string {
  switch (no.tipo) {
    case 'sempre':
      return 'sempre'
    case 'nunca':
      return 'nunca'
    case 'comparacao':
      return `${no.chave} ${legivel(nomeDoOperador(no))} ${no.valores.join(', ')}`
    case 'todas':
      return no.filhos.map(descrever).join(' e ')
    case 'alguma':
      return no.filhos.map(descrever).join(' ou ')
    case 'negacao':
      return `não (${descrever(no.filho)})`
  }
}

const LEGIVEIS: Record<string, string> = {
  Igual: '=',
  Diferente: '≠',
  Parecido: 'parecido com',
  Booleano: 'é',
  Nulo: 'está ausente?',
  Maior: '>',
  MaiorOuIgual: '≥',
  Menor: '<',
  MenorOuIgual: '≤',
  DataDepois: 'depois de',
  DataAntes: 'antes de',
}

export function legivel(operador: string): string {
  for (const prefixo of PREFIXOS) {
    if (operador.startsWith(prefixo)) {
      const base = legivel(operador.slice(prefixo.length))
      if (prefixo === 'ParaAlgumValor:') return `contém algum que ${base}`
      if (prefixo === 'ParaTodoValor:') return `todos ${base}`
      return `se existir, ${base}`
    }
  }
  return LEGIVEIS[operador] ?? operador
}
