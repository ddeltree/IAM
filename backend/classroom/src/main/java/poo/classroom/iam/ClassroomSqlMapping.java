package poo.classroom.iam;

import poo.iam.query.SqlMapping;

/**
 * A outra metade do vocabulário de chaves: onde cada uma mora no banco.
 *
 * Não existe banco neste projeto — isto está aqui para mostrar que o filtro
 * derivado da política vira SQL sem que a política, o motor ou o controller
 * mudem. O {@code ClassroomAttributes} lê a chave de dentro do objeto; este
 * mapeamento escreve a mesma chave numa consulta.
 */
public final class ClassroomSqlMapping implements SqlMapping {

  @Override
  public String igual(String chave, String valor) {
    return switch (chave) {
      case "turma:professorId" -> "turma.professor_id = " + literal(valor);
      case "turma:id", "recurso:id" -> "turma.id = " + literal(valor);
      case "recurso:autorId", "post:autorId", "atividade:autorId", "comentario:autorId" ->
        "publicacao.autor_id = " + literal(valor);
      default -> null; // sem tradução: não filtra, o motor confere depois
    };
  }

  @Override
  public String contem(String chave, String valor) {
    return switch (chave) {
      case "turma:alunoIds" -> "EXISTS (SELECT 1 FROM matricula m"
          + " WHERE m.turma_id = turma.id AND m.aluno_id = " + literal(valor) + ")";
      case "principal:groups" -> null;
      default -> null;
    };
  }

  private static String literal(String valor) {
    return "'" + valor.replace("'", "''") + "'";
  }
}
