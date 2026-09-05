package poo.iam.condition;

import java.util.List;
import java.util.Set;

import poo.iam.RequestContext;

/**
 * A condição de uma concessão, como dado em vez de código.
 *
 * Enquanto era uma lambda dava para executá-la e nada mais. Sendo uma árvore,
 * ela também pode ser lida: serializada como documento, explicada quando nega,
 * e avaliada parcialmente para responder "quem pode isto?" — que é a diferença
 * entre um motor que responde sim ou não e um que se deixa consultar.
 *
 * O formato segue o bloco {@code Condition} da AWS. {@link AlgumaDas} e
 * {@link Negacao} são extensão deliberada: lá o bloco é um E implícito e a
 * alternativa se escreve como duas cláusulas. Manter a árvore booleana
 * completa é o que torna o {@link ConditionVisitor} útil.
 */
public interface Condition {

  Condition SEMPRE = Sempre.INSTANCIA;

  /** Desliga a cláusula sem apagá-la. */
  Condition NUNCA = Nunca.INSTANCIA;

  boolean avaliar(RequestContext ctx);

  /** As chaves de contexto que esta condição lê. */
  Set<String> chaves();

  <R> R accept(ConditionVisitor<R> visitor);

  default Condition ou(Condition outra) {
    return new AlgumaDas(List.of(this, outra));
  }

  default Condition e(Condition outra) {
    return new TodasAs(List.of(this, outra));
  }

  // ---------- construtores de leitura agradável ----------

  static Condition igual(String chave, String valor) {
    return new Comparacao(Operadores.IGUAL, chave, List.of(valor));
  }

  static Condition diferente(String chave, String valor) {
    return new Comparacao(Operadores.DIFERENTE, chave, List.of(valor));
  }

  /** Basta um dos valores da chave casar — o caso de "está matriculado". */
  static Condition contem(String chave, String valor) {
    return new Comparacao(Operadores.paraAlgumValor(Operadores.IGUAL), chave, List.of(valor));
  }

  static Condition ausente(String chave) {
    return new Comparacao(Operadores.NULO, chave, List.of("true"));
  }

  static Condition todasAs(Condition... condicoes) {
    return new TodasAs(List.of(condicoes));
  }

  static Condition algumaDas(Condition... condicoes) {
    return new AlgumaDas(List.of(condicoes));
  }

  static Condition nao(Condition condicao) {
    return new Negacao(condicao);
  }

  /** O atributo é maior que este número. */
  static Condition maiorQue(String chave, String valor) {
    return new Comparacao(Operadores.MAIOR, chave, List.of(valor));
  }

  static Condition menorQue(String chave, String valor) {
    return new Comparacao(Operadores.MENOR, chave, List.of(valor));
  }

  /** Depois deste instante, data ou hora ISO-8601. */
  static Condition depoisDe(String chave, String quando) {
    return new Comparacao(Operadores.DATA_DEPOIS, chave, List.of(quando));
  }

  static Condition antesDe(String chave, String quando) {
    return new Comparacao(Operadores.DATA_ANTES, chave, List.of(quando));
  }
}
