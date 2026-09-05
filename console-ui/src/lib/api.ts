import type {
  Cenario,
  Operadores,
  Permissao,
  Politica,
  Principal,
  Recurso,
  OndePosso,
  QuemPode,
  Simulacao,
  Statement,
  ValidacaoDeCondicao,
} from './types'

/** O Vite faz proxy de /api para o :7001 (vite.config.ts). */
const BASE = '/api'

/** Erro com o status e a mensagem que o backend mandou, em texto puro. */
export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'ApiError'
  }
}

type Opcoes = { method?: string; body?: unknown }

async function requisicao<T>(caminho: string, opcoes?: Opcoes): Promise<T> {
  const temCorpo = opcoes?.body !== undefined
  const res = await fetch(BASE + caminho, {
    method: opcoes?.method ?? 'GET',
    headers: temCorpo ? { 'Content-Type': 'application/json' } : undefined,
    body: temCorpo ? JSON.stringify(opcoes!.body) : undefined,
  })

  // o backend responde erro em texto puro e 204 sem corpo, então lê-se o texto
  // primeiro e só então tenta-se interpretar
  const texto = await res.text()
  if (!res.ok) throw new ApiError(res.status, texto || res.statusText)
  return (texto ? JSON.parse(texto) : undefined) as T
}

// ---------- cenário ----------

export const carregarCenario = () => requisicao<Cenario>('/cenario')
export const reiniciarCenario = () =>
  requisicao<void>('/cenario/reiniciar', { method: 'POST' })

// ---------- principais ----------

export const criarPrincipal = (
  tipo: 'usuarios' | 'grupos' | 'papeis',
  nome: string,
) => requisicao<Principal>(`/${tipo}`, { method: 'POST', body: { nome } })

export const apagarPrincipal = (id: string) =>
  requisicao<void>(`/principais/${id}`, { method: 'DELETE' })

export const verPrincipal = (id: string) =>
  requisicao<Principal>(`/principais/${id}`)

export const entrarNoGrupo = (usuario: string, grupo: string) =>
  requisicao<void>(`/usuarios/${usuario}/grupos/${grupo}`, { method: 'POST' })

export const sairDoGrupo = (usuario: string, grupo: string) =>
  requisicao<void>(`/usuarios/${usuario}/grupos/${grupo}`, { method: 'DELETE' })

export const anexarPolitica = (principal: string, politica: string) =>
  requisicao<void>(`/principais/${principal}/politicas/${politica}`, {
    method: 'POST',
  })

export const desanexarPolitica = (principal: string, politica: string) =>
  requisicao<void>(`/principais/${principal}/politicas/${politica}`, {
    method: 'DELETE',
  })

export const adicionarClausula = (principal: string, statement: Statement) =>
  requisicao<Statement>(`/principais/${principal}/statements`, {
    method: 'POST',
    body: statement,
  })

export const removerClausula = (principal: string, sid: string) =>
  requisicao<void>(
    `/principais/${principal}/statements/${encodeURIComponent(sid)}`,
    {
      method: 'DELETE',
    },
  )

// ---------- papéis ----------

export const adicionarConfianca = (papel: string, statement: Statement) =>
  requisicao<Statement>(`/papeis/${papel}/confianca`, {
    method: 'POST',
    body: statement,
  })

export const removerConfianca = (papel: string, sid: string) =>
  requisicao<void>(`/papeis/${papel}/confianca/${encodeURIComponent(sid)}`, {
    method: 'DELETE',
  })

export const assumirPapel = (papel: string, principal: string) =>
  requisicao<Principal>(`/papeis/${papel}/assumir`, {
    method: 'POST',
    body: { principal },
  })

export const largarSessao = (id: string) =>
  requisicao<void>(`/sessoes/${id}`, { method: 'DELETE' })

// ---------- políticas ----------

export const salvarPolitica = (nome: string, statements: Statement[]) =>
  requisicao<Politica>(`/politicas/${encodeURIComponent(nome)}`, {
    method: 'PUT',
    body: { statements },
  })

export const apagarPolitica = (nome: string) =>
  requisicao<void>(`/politicas/${encodeURIComponent(nome)}`, {
    method: 'DELETE',
  })

export const documentoDePoliticas = () =>
  requisicao<Record<string, unknown>>('/politicas/documento')

export const validarCondicao = (condition: unknown) =>
  requisicao<ValidacaoDeCondicao>('/politicas/validar-condicao', {
    method: 'POST',
    body: { condition },
  })

// ---------- recursos ----------

export const salvarRecurso = (
  tipo: string,
  id: string,
  corpo: {
    atributos: Record<string, string[] | string>
    pai?: string | null
    politica?: { statements: Statement[] } | null
  },
) =>
  requisicao<Recurso>(
    `/recursos/${encodeURIComponent(tipo)}/${encodeURIComponent(id)}`,
    {
      method: 'PUT',
      body: corpo,
    },
  )

export const apagarRecurso = (ref: string) =>
  requisicao<void>(`/recursos/${ref}`, { method: 'DELETE' })

// ---------- vocabulário ----------

export const chavesDisponiveis = () =>
  requisicao<string[]>('/vocabulario/chaves')
export const operadoresDisponiveis = () =>
  requisicao<Operadores>('/vocabulario/operadores')

export const declararPermissao = (
  acao: string,
  tipo: string,
  semAlvo = false,
) =>
  requisicao<Permissao>('/vocabulario/permissoes', {
    method: 'POST',
    body: { acao, tipo, semAlvo },
  })

export const removerPermissao = (acao: string, tipo: string) =>
  requisicao<void>(
    `/vocabulario/permissoes/${encodeURIComponent(acao)}/${encodeURIComponent(tipo)}`,
    { method: 'DELETE' },
  )

// ---------- consultas ----------

export const simular = (corpo: {
  principal: string
  acao: string
  tipo: string
  recurso?: string | null
  chaves?: Record<string, string>
}) => requisicao<Simulacao>('/simular', { method: 'POST', body: corpo })

export const efetivas = (principal: string, recurso?: string) =>
  requisicao<Record<string, boolean>>(
    `/efetivas?principal=${encodeURIComponent(principal)}` +
      (recurso ? `&recurso=${encodeURIComponent(recurso)}` : ''),
  )

export const quemPode = (acao: string, tipo: string, recurso?: string) =>
  requisicao<QuemPode>(
    `/quem-pode?acao=${encodeURIComponent(acao)}&tipo=${encodeURIComponent(tipo)}` +
      (recurso ? `&recurso=${encodeURIComponent(recurso)}` : ''),
  )

export const ondePosso = (principal: string, acao: string, tipo: string) =>
  requisicao<OndePosso>(
    `/onde-posso?principal=${encodeURIComponent(principal)}` +
      `&acao=${encodeURIComponent(acao)}&tipo=${encodeURIComponent(tipo)}`,
  )
