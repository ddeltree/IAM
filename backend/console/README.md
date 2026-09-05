# console

Um console de IAM sobre o `iam-core`, na porta **7001**. A interface está em
[`console-ui/`](../../console-ui).

É a segunda aplicação do núcleo — e a que o exercita por inteiro, porque **o
domínio dele é o que você digitar**. As ações, os tipos de recurso e os atributos
de cada recurso são criados em tempo de execução, pela tela. Não há domínio a
modelar.

## Por que ele existe

O `classroom` declara tudo em enums: 26 permissões, 5 tipos, 4 condições, fixos
antes de o programa rodar. Ele nunca criou uma política em tempo de execução,
nunca anexou política a um recurso e nunca assumiu um papel — três capacidades
que existiam, estavam testadas, e não tinham uso.

Uma aplicação só também não prova que o núcleo é genérico. Duas provam mais, e
duas com formas opostas — uma de vocabulário fechado, outra de vocabulário
aberto — provam bastante.

## O que ele revelou sobre o núcleo

Escrever este módulo forçou quatro adições no `iam-core`, todas pequenas:

- `ContextResolver.registrarPadrao` — com tipos nascendo na tela, registrar um
  `AttributeProvider` por tipo criado seria manutenção de mapa a cada clique;
- `remover(Statement)` e `removerPorSid` — `revoke(Permission)` apaga *todas* as
  concessões de uma permissão e nem alcança uma cláusula com curinga;
- `Role.deixaDeConfiar(sid)` — e escrever o teste disso achou um bug: toda
  cláusula de confiança tinha o mesmo sid;
- `AuthorizationEngine.clausulasDe` — `getStatements()` devolve só as cláusulas
  próprias, e quem quer *mostrar* a política de alguém precisa da soma rotulada.

E uma capacidade nova, que é o coração da tela: `explicar(...)`, que devolve
todas as cláusulas consideradas e o contexto resolvido.

## Não há autenticação

O console é uma ferramenta para montar e inspecionar políticas. Quem responde
por elas é escolhido num seletor, não por login.

Governá-lo com o próprio núcleo seria a demonstração mais forte que existe — é
literalmente o que a AWS faz com `iam:CreateUser` — e também o jeito mais fácil
de se trancar fora dele no meio de uma apresentação. O autogoverno vira um
cenário que se monta *dentro* dele: declare `iam:CriarUsuario` como ação e
simule quem poderia.

## As rotas

```
GET    /cenario                          tudo de uma vez
POST   /cenario/reiniciar

POST   /usuarios  /grupos  /papeis       {nome}
GET    /principais/{id}                  a política separada por origem
DELETE /principais/{id}
POST   /usuarios/{id}/grupos/{grupo}     entrar · DELETE para sair
POST   /principais/{id}/politicas/{nome} anexar · DELETE para desanexar
POST   /principais/{id}/statements       cláusula inline
DELETE /principais/{id}/statements/{sid}

POST   /papeis/{id}/confianca            quem pode assumir
POST   /papeis/{id}/assumir              {principal} → uma sessão
DELETE /sessoes/{id}

GET    /politicas  ·  PUT/DELETE /politicas/{nome}
GET    /politicas/documento              o documento inteiro
POST   /politicas/validar-condicao

GET    /recursos  ·  PUT/DELETE /recursos/{tipo}/{id}

GET    /vocabulario/chaves               o que uma condição pode ler
GET    /vocabulario/operadores           operadores e prefixos, separados
POST   /vocabulario/permissoes           declara o par (ação, tipo)

POST   /simular                          ★ a decisão, as cláusulas, o contexto
GET    /efetivas                         tudo o que se pode sobre um recurso
GET    /quem-pode                        a pergunta ao contrário
GET    /onde-posso                       o dual: devolve um filtro, e o SQL dele
```

## Duas decisões que valem registrar

**Permissões são declaradas, não cruzadas.** O vocabulário guarda pares (ação,
tipo), e não o produto cartesiano de tudo. Cruzar automaticamente encheria o
simulador de perguntas sem sentido (`LER` sobre `USUARIO`), e o catálogo
deixaria de ser uma lista do que existe para virar uma lista do que é
combinável.

**O `Iam` é montado uma vez** e lê dos mapas do cenário por referência — o
catálogo, o diretório e o provedor de política de recurso são lambdas sobre
eles. Numa tela em que cada clique muda a configuração, remontar o componente a
cada mudança seria inviável.

## O cenário semente

Um serviço de arquivos, escolhido para exercitar cada capacidade de uma vez:
grupos, política nomeada anexada, cláusula inline, condição com variável de
política, corrente de recursos (um `OBJETO` dentro de um `BUCKET`, com a
política falando do dono do bucket), política no próprio recurso, e um papel com
política de confiança.

Ele é testado. Se quebrar, a demonstração quebra — melhor descobrir no
`mvn test`.
