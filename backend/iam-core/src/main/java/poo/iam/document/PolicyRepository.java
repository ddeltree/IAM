package poo.iam.document;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import poo.iam.Policy;

/**
 * De onde vêm as políticas nomeadas.
 *
 * É a costura por onde a política deixa de morar no código. A implementação
 * daqui guarda em memória — o que basta para carregar um documento na
 * inicialização — e uma aplicação que precise de política editável em
 * produção implementa esta mesma interface sobre um banco, sem que o motor
 * saiba a diferença.
 */
public interface PolicyRepository {

  Policy porNome(String nome);

  Collection<Policy> todas();

  /** Lê um documento {@code { "politicas": [...] }} para memória. */
  static PolicyRepository deDocumento(Object documento) {
    return emMemoria(PolicyDocument.lerTodas(documento));
  }

  static PolicyRepository emMemoria(List<Policy> politicas) {
    var porNome = new LinkedHashMap<String, Policy>();
    for (Policy policy : politicas) {
      var anterior = porNome.put(policy.getNome(), policy);
      if (anterior != null)
        throw new IllegalArgumentException(
            "Duas políticas com o nome " + policy.getNome() + "; a segunda apagaria a primeira");
    }
    return new PolicyRepository() {
      @Override
      public Policy porNome(String nome) {
        var policy = porNome.get(nome);
        // erro alto, e não null: anexar uma política que não existe é um erro
        // de configuração, e devolvê-lo como "sem permissões" faria a
        // aplicação subir com todo mundo barrado sem dizer por quê
        if (policy == null)
          throw new IllegalArgumentException(
              "Política desconhecida: " + nome + ". Conhecidas: " + porNome.keySet());
        return policy;
      }

      @Override
      public Collection<Policy> todas() {
        return porNome.values();
      }
    };
  }
}
