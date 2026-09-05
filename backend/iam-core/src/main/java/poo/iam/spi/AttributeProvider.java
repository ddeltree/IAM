package poo.iam.spi;

import java.util.List;
import java.util.Map;

import poo.iam.Resource;
import poo.iam.ResourceType;

/**
 * Traduz um recurso da aplicação nas chaves de condição que as políticas leem.
 *
 * É por aqui que o núcleo consegue avaliar "o autor é quem está pedindo?" sem
 * nunca ter ouvido falar em post ou em turma: a aplicação registra um provedor
 * por tipo, e o núcleo só enxerga texto.
 *
 * As chaves vêm sem prefixo ({@code autorId}); quem as qualifica com o tipo
 * ({@code post:autorId}) é o {@link ContextResolver}.
 */
public interface AttributeProvider {

  ResourceType tipo();

  Map<String, List<String>> atributosDe(Resource recurso);
}
