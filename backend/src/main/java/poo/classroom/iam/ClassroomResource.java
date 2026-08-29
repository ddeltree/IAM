package poo.classroom.iam;

import poo.iam.ResourceType;

/**
 * Os tipos de recurso desta aplicação. Usuário não aparece aqui: quem o define
 * é o núcleo, em {@code PrincipalResource}, porque é ele que gerencia
 * principais.
 */
public enum ClassroomResource implements ResourceType {
  TURMA,
  POST,
  ATIVIDADE,
  COMENTARIO,
}
