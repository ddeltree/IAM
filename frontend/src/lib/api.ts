import type {
  Atividade,
  RefRecurso,
  RespostaPermissoes,
  Comentario,
  Participante,
  Post,
  TipoPublicacao,
  Turma,
  UsuarioDTO,
} from './types'

/**
 * Em desenvolvimento o Vite faz proxy de /api para o backend (vite.config.ts),
 * o que mantém tudo na mesma origem e faz o cookie `uid` viajar junto.
 */
const BASE = import.meta.env.VITE_API_BASE ?? '/api'

/** Erro com o status HTTP e a mensagem que o backend mandou (texto puro). */
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

  // O backend responde erro em texto puro e 204 sem corpo nenhum, então ler
  // como texto primeiro evita estourar no JSON.parse.
  const texto = await res.text()
  if (!res.ok) {
    if (res.status === 401) window.dispatchEvent(new Event('sessao-expirada'))
    throw new ApiError(res.status, texto.trim() || res.statusText)
  }
  return (texto ? JSON.parse(texto) : undefined) as T
}

// --- permissões -----------------------------------------------------------

/**
 * Pergunta ao backend o que o usuário pode fazer — as ações sem alvo e, para
 * cada referência passada, as ações sobre aquele recurso.
 *
 * É o que substitui a antiga cópia das regras de autorização no cliente: agora
 * existe uma fonte de verdade só, e ela é o motor de permissões do servidor.
 */
export const consultarPermissoes = (refs: readonly RefRecurso[] = []) =>
  requisicao<RespostaPermissoes>(
    '/permissoes' +
      (refs.length
        ? '?' + refs.map((r) => `recurso=${encodeURIComponent(r)}`).join('&')
        : ''),
  )

// --- turmas ---------------------------------------------------------------

export const listarTurmas = () => requisicao<Turma[]>('/turmas')

export const verTurma = (turmaId: string) =>
  requisicao<Turma>(`/turmas/${turmaId}`)

export const criarTurma = (nome: string) =>
  requisicao<Turma>('/turmas', { method: 'POST', body: { nome } })

export const renomearTurma = (turmaId: string, nome: string) =>
  requisicao<Turma>(`/turmas/${turmaId}`, { method: 'PUT', body: { nome } })

export const excluirTurma = (turmaId: string) =>
  requisicao<void>(`/turmas/${turmaId}`, { method: 'DELETE' })

// --- posts (aninhados na turma) -------------------------------------------

export const listarPosts = (turmaId: string) =>
  requisicao<Post[]>(`/turmas/${turmaId}/posts`)

export const criarPost = (
  turmaId: string,
  dados: { titulo: string; corpo: string },
) =>
  requisicao<Post>(`/turmas/${turmaId}/posts`, { method: 'POST', body: dados })

export const atualizarPost = (
  turmaId: string,
  postId: string,
  dados: { titulo: string; corpo: string },
) =>
  requisicao<Post>(`/turmas/${turmaId}/posts/${postId}`, {
    method: 'PUT',
    body: dados,
  })

export const excluirPost = (turmaId: string, postId: string) =>
  requisicao<void>(`/turmas/${turmaId}/posts/${postId}`, { method: 'DELETE' })

// --- atividades (rota plana, filtro por query) ----------------------------

export const listarAtividades = (turmaId?: string) =>
  requisicao<Atividade[]>(
    '/atividades' + (turmaId ? `?turmaId=${encodeURIComponent(turmaId)}` : ''),
  )

export const verAtividade = (id: string) =>
  requisicao<Atividade>(`/atividades/${id}`)

export const criarAtividade = (dados: {
  titulo: string
  corpo: string
  dataEntrega: string
  turmaId: string
}) => requisicao<Atividade>('/atividades', { method: 'POST', body: dados })

/** O PUT ignora `turmaId`, então nem mandamos — o Jackson recusa campo a mais. */
export const atualizarAtividade = (
  id: string,
  dados: { titulo: string; corpo: string; dataEntrega: string },
) => requisicao<Atividade>(`/atividades/${id}`, { method: 'PUT', body: dados })

export const excluirAtividade = (id: string) =>
  requisicao<void>(`/atividades/${id}`, { method: 'DELETE' })

// --- comentários ----------------------------------------------------------

const rotaComentarios = (
  turmaId: string,
  tipo: TipoPublicacao,
  pubId: string,
) => `/turmas/${turmaId}/${tipo}/${pubId}/comentarios`

export const listarComentarios = (
  turmaId: string,
  tipo: TipoPublicacao,
  pubId: string,
) => requisicao<Comentario[]>(rotaComentarios(turmaId, tipo, pubId))

export const criarComentario = (
  turmaId: string,
  tipo: TipoPublicacao,
  pubId: string,
  conteudo: string,
) =>
  requisicao<Comentario>(rotaComentarios(turmaId, tipo, pubId), {
    method: 'POST',
    body: { conteudo },
  })

export const atualizarComentario = (
  turmaId: string,
  tipo: TipoPublicacao,
  pubId: string,
  id: string,
  conteudo: string,
) =>
  requisicao<Comentario>(`${rotaComentarios(turmaId, tipo, pubId)}/${id}`, {
    method: 'PUT',
    body: { conteudo },
  })

export const excluirComentario = (
  turmaId: string,
  tipo: TipoPublicacao,
  pubId: string,
  id: string,
) =>
  requisicao<void>(`${rotaComentarios(turmaId, tipo, pubId)}/${id}`, {
    method: 'DELETE',
  })

// --- usuários -------------------------------------------------------------

/** Só o ADMIN tem LISTAR_USUARIOS; para os outros isso responde 403. */
export const listarUsuarios = () => requisicao<UsuarioDTO[]>('/usuarios')

/** Só o próprio perfil. O ADMIN não tem VER_PERFIL, então recebe 403 até de si. */
export const verUsuario = (id: string) =>
  requisicao<UsuarioDTO>(`/usuarios/${id}`)

export const criarUsuario = (name: string, tipo: 0 | 1) =>
  requisicao<UsuarioDTO>('/usuarios', { method: 'POST', body: { name, tipo } })

export const renomearUsuario = (id: string, name: string) =>
  requisicao<UsuarioDTO>(`/usuarios/${id}`, { method: 'PUT', body: { name } })

export const excluirUsuario = (id: string) =>
  requisicao<void>(`/usuarios/${id}`, { method: 'DELETE' })

// --- participantes --------------------------------------------------------

export const listarParticipantes = (turmaId: string) =>
  requisicao<Participante[]>(`/turmas/${turmaId}/participantes`)

/**
 * Devolve void de propósito: o backend responde 201 com o professor que
 * matriculou, e não com o aluno — ler esse corpo só levaria a engano.
 */
export const matricular = (turmaId: string, uid: string) =>
  requisicao<void>(`/turmas/${turmaId}/participantes`, {
    method: 'POST',
    body: { uid },
  })

export const desmatricular = (turmaId: string, uid: string) =>
  requisicao<void>(`/turmas/${turmaId}/participantes/${uid}`, {
    method: 'DELETE',
  })
