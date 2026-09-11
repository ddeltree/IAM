# Notas para quem trabalha neste repositório

## Teste que varre um conjunto de tipos varre o conjunto inteiro

No `backend/iam-core`, quando um teste sustenta uma invariante percorrendo as **espécies**
de alguma coisa — os operadores de `condition/Operadores`, os nós de `ResourceConstraint`
ou de `Condition`, as formas de `ResourcePattern`, os efeitos de `Effect` —, a lista de
espécies tem de vir da fonte da verdade, e não de uma amostra escrita à mão:

```java
for (String nome : OperatorRegistry.padrao().nomes()) { ... }   // sim
for (String nome : List.of("Igual", "Diferente")) { ... }       // não
```

e o teste termina conferindo que cobriu todas elas, para que **espécie nova sem caso
quebre o teste em vez de passar por ele**. `ConsultabilidadeTest` é o modelo:
`aPodaEAVarreduraConcordamEmTodoOperador` é dirigido por `OperatorRegistry.nomes()` e
falha se algum operador ficar de fora.

**Por quê.** A garantia que este núcleo anuncia — *"a extração de restrição só escolhe
candidatos; o motor avalia cada sobrevivente; um erro no extrator custa desempenho, nunca
acesso indevido"* — era conferida por um teste que dizia varrer "a matriz inteira" e
varria **um** operador, o `Igual`. Foi exatamente ali que o `PrincipalConstraintExtractor`
pôde devolver o conjunto **invertido** para o `Diferente`: perguntado quem podia ler um
arquivo, o motor dizia "Bruno e Carla" e a consulta respondia "ninguém". A poda decidindo,
que é o que o módulo promete que nunca acontece — e escondendo acesso numa resposta de
auditoria, que é o pior sentido para se errar.

Uma matriz com um caso por espécie não é rigor extra: é a diferença entre um teste que
sustenta a invariante e um que a ilustra. E o custo é baixo — as 44 combinações daquele
teste cabem numa classe que roda inteira em 136 ms.

**Consequência de projeto.** Se uma espécie não se deixa enumerar, o enumerador faz parte
da mudança. Foi por isso que `OperatorRegistry.nomes()` passou a existir junto com a
correção: sem ele, a lista voltaria a ser escrita à mão.

## Onde o trabalho acontece

O assunto do repositório é o `backend/iam-core` — o componente de autorização, sem
dependência de compilação nenhuma. `classroom`, `console`, `frontend` e `console-ui` são
aplicações dele, e existem para exercitá-lo. Antes de acrescentar capacidade ao núcleo,
`backend/iam-core/README.md` diz o que ele já faz e o que ele deliberadamente não faz.
