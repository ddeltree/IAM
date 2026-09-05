# iam-core

Um componente de autorização, inspirado no IAM da AWS. Decide se um principal
pode uma ação sobre um recurso — e, tão importante quanto, **se deixa
perguntar**: quem pode isto, sobre o que posso agir, por que foi negado.

Não conhece nenhum domínio. O `classroom` deste repositório é uma adaptação
possível, não a razão de ele existir.

```
$ mvn -o dependency:tree -pl iam-core
poo:iam-core:jar:1.0-SNAPSHOT
\- org.junit.jupiter:junit-jupiter:jar:5.10.0:test
```

Essa lista é a especificação mais importante do módulo. Sem framework web, sem
biblioteca de serialização, sem aplicação — e é o compilador que garante, não
disciplina de quem escreve.

## O contrato de integração

Tudo o que uma aplicação precisa implementar está em `poo.iam.spi`. São sete
interfaces, e quase nenhum sistema precisa das sete:

| interface | responde | obrigatória? |
|---|---|---|
| `AttributeProvider` | como ler os atributos de um recurso do domínio | sim, se as condições falarem sobre recursos |
| `ActionCatalog` | o que existe para ser pedido | só para responder "o que posso fazer aqui?" |
| `PrincipalDirectory` | onde estão os usuários, grupos e papéis | só para as consultas ao contrário |
| `SqlMapping` | como a mesma chave se escreve em SQL | só se o filtro for para o banco |
| `ResourcePolicyProvider` | a política anexada ao próprio recurso | só se houver compartilhamento |
| `PolicyListener` | quem é avisado quando uma política muda | não; o padrão é o silêncio |
| `PolicyRepository` | de onde vêm as políticas nomeadas | não; há uma em memória |

Um domínio simples precisa de um `AttributeProvider` por tipo de recurso e nada
mais.

## Adaptando um sistema

O exemplo completo, executável, está em
[`ExemploDeAdaptacaoTest`](src/test/java/ExemploDeAdaptacaoTest.java) — um
repositório de arquivos, do vocabulário até as cinco consultas. Ele é teste, e
não só texto, para que envelhecer deixe de ser uma opção. O esqueleto:

```java
// 1. o vocabulário é seu: Action e ResourceType são interfaces, não enums fechados
enum Acao implements Action { LER, ESCREVER, APAGAR }
enum Tipo implements ResourceType { ARQUIVO }

// 2. seus objetos viram recursos
record Arquivo(String id, String donoId) implements Resource {
  public ResourceType getType() { return Tipo.ARQUIVO; }
  public String getId()         { return id; }
  public Resource getPai()      { return pasta; }   // opcional: a corrente de contexto
}

// 3. como o núcleo lê um deles
AttributeProvider atributos = ...;   // ARQUIVO -> { "donoId": ["7"] }

// 4. monte
var iam = IamFactory.novo().atributos(atributos).catalogo(...).principais(...).construir();

iam.motor().isAllowed(ana, ESCREVER, arquivo);
```

## O modelo

Uma **cláusula** (`Statement`) tem quatro campos, os mesmos da AWS:

```
Effect      ALLOW | DENY
Action      "*" | "EDITAR_*" | "EDITAR_POST"
Resource    "*" | "TURMA" | "TURMA/3"
Condition   uma árvore de comparações
```

A condição mora na **concessão**, e não na definição da ação. É o que permite a
mesma permissão ser dada com restrições diferentes a principais diferentes: um
grupo recebe `EDITAR_POST` só para os próprios posts, o administrador recebe
`EDITAR_POST` sem restrição.

A **política** de quem pede é a dele mais a de quem ele herda — grupos, papéis
assumidos. O motor percorre esse grafo somando cláusulas, e por isso não sabe o
que é um grupo. Foi assim que papéis e sessões couberam no sistema sem que o
avaliador mudasse.

A ordem é a da AWS: **negação explícita vence qualquer concessão; na ausência
das duas, nega**.

## As condições são dado

```json
{ "AlgumaDas": [
    { "Igual":                { "turma:professorId": ["${principal:id}"] } },
    { "ParaAlgumValor:Igual": { "turma:alunoIds":    ["${principal:id}"] } } ] }
```

Isso já foi uma lambda. Enquanto era, dava para executá-la e nada mais. Sendo
uma árvore, ela também se lê: serializa, explica quando nega, e se deixa avaliar
parcialmente — que é a diferença entre um motor que responde sim ou não e um que
se deixa consultar.

As variáveis (`${principal:id}`) são o que permite uma cláusula servir a todos os
usuários. Sem elas, "o autor pode editar" não seria expressável como dado.

**Operadores**: `Igual`, `Diferente`, `Parecido` (curinga), `Booleano`, `Nulo`,
`Maior`, `MaiorOuIgual`, `Menor`, `MenorOuIgual`, `DataDepois`, `DataAntes`.
**Prefixos**, que são decoradores e se compõem: `ParaAlgumValor:`,
`ParaTodoValor:`, `SeExistir:`. Acrescentar um operador é registrá-lo, não
editar o avaliador.

**Chaves publicadas pelo núcleo**: `principal:id`, `principal:name`,
`principal:groups`, `recurso:id`, `recurso:tipo`, `recurso:*` (os atributos do
alvo), `<tipo>:*` (os de cada nível da corrente de pais), `contexto:instante`,
`contexto:data`, `contexto:hora`, e `requisicao:*` (o que o chamador passar).

## As cinco perguntas

| pergunta | como |
|---|---|
| posso? | `iam.motor().isAllowed(quem, acao, recurso)` |
| por que não? | `iam.motor().avaliar(...)` — a cláusula que decidiu e onde ela estava |
| o que posso fazer aqui? | `iam.efetivas().sobre(quem, recurso)` — em lote, para a interface |
| quem pode isto? | `iam.consultas().quemPode(acao, recurso)` |
| sobre o que posso agir? | `iam.consultas().ondePosso(quem, acao)` — vira predicado ou `WHERE` |

### A regra que torna as duas últimas seguras

> **A extração de restrição só escolhe candidatos; o motor avalia cada
> sobrevivente.**

Um erro no extrator custa desempenho, nunca acesso indevido. Uma chave sem
tradução vira `1=1`, nunca `1=0` — filtro que exclui por não entender esconderia
acesso legítimo. `ConsultabilidadeTest` confere isso sobre a matriz inteira.

É essa regra que permitiu curinga, padrão de recurso, operadores de ordem,
política no recurso e papéis entrarem sem que nenhuma das consultas piorasse.
Cada capacidade veio acompanhada da peça que a mantém consultável — e não
depois.

### Um vocabulário, dois sentidos

```java
PredicateRenderer  implements ConstraintVisitor<Predicate<Resource>>   // em memória
SqlWhereRenderer   implements ConstraintVisitor<String>                // no banco
```

```
AtributoIgual("turma:professorId", "7")  →  turma.professor_id = '7'
AtributoContem("turma:alunoIds",   "7")  →  EXISTS (SELECT 1 FROM matricula m
                                                    WHERE m.turma_id = turma.id
                                                      AND m.aluno_id = '7')
```

O `AttributeProvider` **lê** a chave de dentro de um objeto; o `SqlMapping`
**escreve** a mesma chave numa consulta. É esse par que faz a mesma política
valer nos dois lugares.

## Papéis, sessões e política no recurso

Um **papel** (`Role`) não é um grupo: a política dele só vale enquanto alguém o
exerce. Quem pode assumi-lo está na política de confiança — que é uma política
*de recurso*, com o papel como recurso, exatamente como na AWS.

Sob uma sessão, `principal:id` é o id da sessão, não o da pessoa. Uma condição
"o autor pode editar" portanto **não** vale para quem assumiu um papel, e quem
estava por trás fica em `sessao:origem`. É o que separa agir como si mesmo de
agir sob um papel.

Uma **política no recurso** (`ResourcePolicyProvider`) deixa o dono de um objeto
compartilhá-lo sem editar a política de ninguém. Quem ela alcança se diz por
condição sobre `principal:*` — não há campo `Principal` na cláusula, e não
precisa haver.

## O que ele deliberadamente não faz

- **Política de sessão.** A AWS tem, mas lá ela intersecta — só restringe. Este
  motor soma, então uma aqui ampliaria em vez de restringir. Melhor não oferecer
  do que enganar.
- **Juntar quem pode com quem chegaria a poder.** `quemPode` devolve `viaPapel`
  num mapa à parte: uma auditoria que confunde as duas superestima o acesso.
- **Persistir.** `PolicyRepository` é a costura; a implementação daqui é em
  memória.
- **Nomear a cláusula que barrou, para o usuário final.** `Decisao.getSid()` é
  ferramenta de quem administra. Num 403 é vazamento de informação.
