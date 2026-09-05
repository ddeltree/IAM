/** O documento de uma condição, no formato do bloco Condition da AWS. */
export type CondicaoJson = Record<string, unknown> | null

export type Statement = {
  sid: string
  effect: 'ALLOW' | 'DENY'
  action: string
  resource: string
  condition: CondicaoJson
  /** Presente só na política efetiva: de onde a cláusula veio. */
  origem?: string
}

export type TipoDePrincipal = 'USUARIO' | 'GRUPO' | 'PAPEL' | 'SESSAO'

export type Principal = {
  id: string
  nome: string
  tipo: TipoDePrincipal
  inline: Statement[]
  /** A soma do que o motor percorre, cada cláusula com sua origem. */
  efetiva: Statement[]
  anexadas?: string[]
  grupos?: string[]
  membros?: string[]
  confianca?: Statement[]
  papel?: string
  origem?: string
}

export type Politica = {
  nome: string
  statements: Statement[]
  anexadaA?: string[]
}

export type Recurso = {
  ref: string
  tipo: string
  id: string
  atributos: Record<string, string[]>
  pai: string | null
  politica: Politica | null
}

export type Permissao = {
  acao: string
  tipo: string
  rotulo: string
}

export type Cenario = {
  usuarios: Principal[]
  grupos: Principal[]
  papeis: Principal[]
  sessoes: Principal[]
  politicas: Politica[]
  recursos: Recurso[]
  acoes: string[]
  tipos: string[]
  permissoes: Permissao[]
  semAlvo: Permissao[]
}

/** Uma cláusula como o simulador a devolve: por que ela valeu, ou não. */
export type ClausulaAvaliada = Statement & {
  origem: string
  alcancaORecurso: boolean
  condicaoPassou: boolean
  aplicaria: boolean
  decisiva: boolean
}

export type Simulacao = {
  permitido: boolean
  tipo: 'PERMITIDO' | 'NEGACAO_EXPLICITA' | 'NEGACAO_PADRAO'
  motivo: string
  origem: string | null
  decisiva: Statement | null
  clausulas: ClausulaAvaliada[]
  clausulasAlcancadas: number
  contexto: Record<string, string[]>
}

export type QuemPode = {
  principais: { id: string; nome: string }[]
  viaPapel: Record<string, { id: string; nome: string }[]>
  avaliados: number
  conhecidos: number
  podou: boolean
}

export type OndePosso = {
  restricao: string
  sql: string
  recursos: string[]
  candidatos: string[]
}

export type Operadores = {
  operadores: string[]
  prefixos: string[]
}

export type ValidacaoDeCondicao = {
  valida: boolean
  erro?: string
  chaves?: string[]
  chavesDesconhecidas?: string[]
}
