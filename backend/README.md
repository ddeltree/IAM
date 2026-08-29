# Núcleo de autorização (IAM) + aplicação de exemplo

Uma biblioteca de autorização independente de domínio, nos moldes do IAM da AWS,
com uma sala de aula virtual por cima como demonstração.

```
poo.iam            núcleo genérico — não conhece turma, post nem professor
poo.classroom.iam  a adaptação: vocabulário concreto e política padrão
poo.classroom      o domínio (Turma, Post, Atividade, Comentario)
poo.api            as rotas HTTP
```

A seta de dependência aponta num sentido só: `poo.iam` não importa nada de fora.

## Como rodar

```bash
mvn compile dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp target/classes:$(cat cp.txt) poo.Main     # porta 7000
mvn test                                            # 99 testes
```

O estado vive em memória. Ao subir, só existe o **ADMIN (id 1)**; a partir dele o
admin cria professores, e cada professor cria e matricula os próprios alunos.

## O modelo

Principais (usuários e grupos) carregam uma **política**: um conjunto de
cláusulas `Statement`, cada uma com efeito (`ALLOW`/`DENY`), permissão
(ação + tipo de recurso) e uma **condição** opcional.

A decisão segue a ordem da AWS: **negação explícita > concessão > negar por
padrão**, aplicada tanto à política inline quanto à dos grupos.

A condição pertence à **concessão**, não à ação. É isso que permite dar a mesma
permissão com restrições diferentes a cada papel:

```java
admin.grantPermission(EDITAR_POST.get());                      // irrestrito
professores.grantPermission(EDITAR_POST.get(), AUTOR);         // só os seus
professores.grantPermission(EXCLUIR_POST.get(), PROFESSOR_RESPONSAVEL.ou(AUTOR));
```

Daí a assimetria central do modelo: **editar é do autor, excluir é da
moderação**. O professor responsável apaga qualquer post da turma, mas só edita
os seus. E o ADMIN não cria conteúdo nenhum — ele lista, edita e exclui.

## As condições são dado

Cada condição é uma árvore de comparações sobre **chaves de contexto**, no
formato do bloco `Condition` da AWS:

```
PROFESSOR_RESPONSAVEL   Igual                { "turma:professorId": "${principal:id}" }
ALUNO_MATRICULADO       ParaAlgumValor:Igual { "turma:alunoIds":    "${principal:id}" }
AUTOR                   Igual                { "recurso:autorId":   "${principal:id}" }
```

`AlgumaDas` e `Negacao` são extensão deliberada sobre a gramática da AWS, onde o
bloco é um E implícito.

Quem traduz um objeto em chaves é um `AttributeProvider` registrado pela
aplicação; o núcleo só enxerga texto. O `ContextResolver` sobe a corrente
`Resource.getPai()` (post → turma, comentário → publicação), então uma condição
sobre a turma é avaliável partindo de um comentário sem ninguém escrever essa
navegação.

Ser dado é o que torna o motor **consultável**, e não só executável:

| pergunta | rota |
|---|---|
| o que posso fazer aqui? | `GET /permissoes?recurso=TURMA/1&recurso=POST/2` |
| por que foi negado? | idem, com `&explicar=true` |
| como é a política? | `GET /iam/politicas` (só ADMIN) |
| quem pode isto? | `GET /permissoes/quem-pode?acao=EXCLUIR_POST&recurso=POST/1` |
| sobre o que posso agir? | `GET /permissoes/onde-posso?acao=LISTAR_TURMAS` |

`GET /permissoes` conta ao usuário também o que ele **não** pode. É deliberado:
é o que permite a interface esconder um botão em vez de oferecer uma ação que
vai virar 403. O frontend não tem cópia nenhuma das regras.

## Motor e tabelas

Este projeto guarda tudo em memória, mas o desenho já prevê o outro lado, e vale
registrar onde cada abordagem ganha.

Uma modelagem relacional resolve bem a metade RBAC — um `JOIN` responde "esse
usuário tem essa permissão?". O que ela não expressa é a condição: "pode editar
**se for o autor**" não é uma linha, é um predicado sobre os dados da linha. As
saídas seriam materializar ACL por instância (uma linha por usuário × por post)
ou espalhar `if` pelos controllers.

O esquema que a política atual mapearia:

```sql
principal(id, tipo, nome)
membership(user_id, group_id)
policy(id, nome)
principal_policy(principal_id, policy_id)
policy_statement(id, policy_id, sid, efeito, acao, tipo_recurso, id_recurso, condicao_json)
  INDEX (acao, tipo_recurso)
```

A divisão do trabalho: **o SQL filtra grosso** (quais cláusulas *poderiam* falar
sobre esta ação para este principal — indexável) e **o motor decide fino** (as
condições valem para este recurso). As condições nunca executam em SQL.

A exceção é o `SqlWhereRenderer`, e ele mostra a simetria que sustenta a ideia:

```
ClassroomAttributes  lê   "turma:professorId" de dentro de uma Turma
ClassroomSqlMapping  escreve a mesma chave como turma.professor_id = ?
```

Um vocabulário de chaves, dois sentidos. O mesmo `ResourceConstraint` derivado da
política vira um `Predicate` em memória com um visitante e uma cláusula `WHERE`
com outro — sem que a política, o motor ou o controller mudem.

**Regra de segurança:** filtro e poda só podem *encolher* o conjunto de
candidatos; o motor avalia cada sobrevivente. Assim um erro na extração custa
desempenho, nunca acesso indevido.

E o que um banco **não** melhoraria: a latência de uma checagem individual
pioraria (hoje é um `HashMap`); a correção não mudaria; a duplicação que existia
no frontend foi resolvida pelo endpoint de permissões efetivas, não por tabela
nenhuma; e `condicao_json` continua opaco ao SQL — justamente a coisa que mais
se queria consultável é a que a tabela não consulta.

## O que ficou de fora

Comparado ao IAM da AWS: ARNs completos (aqui a referência é `TIPO/id`),
*roles* assumíveis, políticas baseadas em recurso, *permission boundaries* e
SCPs, e o raciocínio automatizado do Access Analyzer. O `Object... context` do
`RequestContext` existe e é o gancho para regras como "só em horário de aula",
mas nenhuma condição atual o usa.
