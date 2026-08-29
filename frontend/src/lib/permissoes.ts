import type { Comentario, Post, Sessao, Turma } from './types'

/**
 * Espelho, no cliente, das regras de `poo.iam.SystemPermission` e das listas de
 * `poo.iam.SecurityContext`. Serve só para mostrar ou esconder ações — quem
 * decide de verdade continua sendo o backend, e toda chamada trata o 403.
 *
 * A assimetria mais importante: **editar é do autor, excluir é da moderação**.
 * O professor responsável apaga qualquer post da turma, mas só edita os seus.
 */

const ehAdmin = (s: Sessao) => s.papel === 'ADMIN'

export const ehProfessorResponsavel = (s: Sessao, t: Turma) =>
  t.professorResponsavel.id === s.id

export const ehAlunoMatriculado = (s: Sessao, t: Turma) =>
  t.alunos.some((a) => a.id === s.id)

export const ehParticipante = (s: Sessao, t: Turma) =>
  ehProfessorResponsavel(s, t) || ehAlunoMatriculado(s, t)

// --- turmas ---------------------------------------------------------------

/** O ADMIN não tem CRIAR_TURMA: ele modera, não produz conteúdo. */
export const podeCriarTurma = (s: Sessao) => s.papel === 'PROFESSOR'

export const podeEditarTurma = (s: Sessao, t: Turma) =>
  ehProfessorResponsavel(s, t)

export const podeExcluirTurma = (s: Sessao, t: Turma) =>
  ehProfessorResponsavel(s, t)

// --- posts ----------------------------------------------------------------

export const podeCriarPost = (s: Sessao, t: Turma) => ehParticipante(s, t)

export const podeEditarPost = (s: Sessao, p: Post) =>
  ehAdmin(s) || p.autor.id === s.id

export const podeExcluirPost = (s: Sessao, p: Post, t: Turma) =>
  ehAdmin(s) || ehProfessorResponsavel(s, t) || p.autor.id === s.id

// --- atividades -----------------------------------------------------------

export const podeCriarAtividade = (s: Sessao, t: Turma) =>
  ehProfessorResponsavel(s, t)

export const podeEditarAtividade = (s: Sessao, t: Turma) =>
  ehAdmin(s) || ehProfessorResponsavel(s, t)

/**
 * O autor de uma atividade é sempre o professor responsável (definido no
 * construtor de `Atividade`), então excluir cai na mesma regra de editar.
 */
export const podeExcluirAtividade = (s: Sessao, t: Turma) =>
  ehAdmin(s) || ehProfessorResponsavel(s, t)

// --- comentários ----------------------------------------------------------

export const podeCriarComentario = (s: Sessao, t: Turma) => ehParticipante(s, t)

export const podeEditarComentario = (s: Sessao, c: Comentario) =>
  ehAdmin(s) || c.autor.id === s.id

export const podeExcluirComentario = (s: Sessao, c: Comentario, t: Turma) =>
  ehAdmin(s) || ehProfessorResponsavel(s, t) || c.autor.id === s.id

// --- participantes --------------------------------------------------------

export const podeMatricular = (s: Sessao, t: Turma) =>
  ehProfessorResponsavel(s, t)

// --- usuários -------------------------------------------------------------

export const podeListarUsuarios = (s: Sessao) => ehAdmin(s)

/** Só o ADMIN tem CRIAR_PROFESSOR; só os professores têm CRIAR_ALUNO. */
export const podeCriarProfessor = (s: Sessao) => ehAdmin(s)
export const podeCriarAluno = (s: Sessao) => s.papel === 'PROFESSOR'
export const podeCriarUsuario = (s: Sessao) =>
  podeCriarProfessor(s) || podeCriarAluno(s)

/** O ADMIN não tem VER_PERFIL — nem para o próprio cadastro. */
export const podeVerPerfil = (s: Sessao, id: string) =>
  !ehAdmin(s) && s.id === id

export const podeEditarUsuario = (s: Sessao, id: string) => podeVerPerfil(s, id)

export const podeExcluirUsuario = (s: Sessao, id: string) =>
  ehAdmin(s) || s.id === id
