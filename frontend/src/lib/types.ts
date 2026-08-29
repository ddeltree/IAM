/**
 * Espelha exatamente o JSON que o backend serializa. Os nomes vêm dos getters
 * das classes em `poo.classroom` e dos DTOs em `poo.api`.
 */

export type Papel = 'ADMIN' | 'PROFESSOR' | 'ALUNO' | 'DESCONHECIDO'

/**
 * Referência a um recurso no formato `TIPO/id`, como o backend espera em
 * `GET /permissoes?recurso=`.
 */
export type RefRecurso =
  `${'TURMA' | 'POST' | 'ATIVIDADE' | 'COMENTARIO' | 'USUARIO'}/${string}`

/** Mapa ação -> pode, como o backend devolve. */
export type MapaPermissoes = Record<string, boolean>

export interface RespostaPermissoes {
  principal: { id: string; name: string; papel: Papel }
  global: MapaPermissoes
  recursos: Record<string, MapaPermissoes>
}

/** Segmento literal da rota de comentários: /turmas/{id}/{posts|atividades}/... */
export type TipoPublicacao = 'posts' | 'atividades'

/** Usuário embutido em outros recursos (autor, professorResponsavel, alunos). */
export interface Usuario {
  id: string
  name: string
  type: 'USUARIO'
}

/** O que /usuarios devolve. `tipo`: 1 professor, 0 aluno. */
export interface UsuarioDTO {
  id: string
  name: string
  tipo: 0 | 1
}

export interface Comentario {
  id: string
  conteudo: string
  autor: Usuario
  publicacaoId: string
  type: 'COMENTARIO'
}

export interface Post {
  id: string
  titulo: string
  corpo: string
  autor: Usuario
  turmaId: string
  comentarios: Comentario[]
  type: 'POST'
}

/** O backend não valida `dataEntrega`, então ela pode voltar nula. */
export interface Atividade extends Omit<Post, 'type'> {
  dataEntrega: string | null
  type: 'ATIVIDADE'
}

export interface Turma {
  id: string
  nome: string
  professorResponsavel: Usuario
  /** alunos + professor responsável */
  participantes: Usuario[]
  alunos: Usuario[]
  posts: Post[]
  type: 'TURMA'
}

/** Sem campo de papel — quem é professor se descobre pela turma. */
export interface Participante {
  turmaId: string
  userId: string
  name: string
}

export interface Sessao {
  id: string
  name: string
  papel: Papel
}
