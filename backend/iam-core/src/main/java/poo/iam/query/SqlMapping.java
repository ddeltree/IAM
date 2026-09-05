package poo.iam.query;

/**
 * Traduz uma chave de condição para SQL.
 *
 * É o espelho exato do {@code AttributeProvider}: o provedor <em>lê</em> a
 * chave de dentro de um objeto; este mapeamento <em>escreve</em> a mesma chave
 * numa consulta. Um vocabulário de chaves, dois sentidos — e é essa simetria
 * que permite a mesma política valer em memória e no banco.
 */
public interface SqlMapping {

  /** Fragmento para "o atributo é igual a", ou {@code null} se não souber. */
  String igual(String chave, String valor);

  /** Fragmento para "o atributo multivalorado contém". */
  String contem(String chave, String valor);
}
