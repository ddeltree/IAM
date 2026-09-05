# Núcleo de autorização (IAM) + aplicação de exemplo

Dois módulos Maven:

```
iam-core/    o componente de autorização — não conhece turma, post nem professor
classroom/   uma sala de aula virtual que o adapta, e as rotas HTTP
```

O `iam-core` **não tem dependência de compilação nenhuma**, e é o compilador que
garante isso desde que os módulos se separaram. A documentação dele está em
[`iam-core/README.md`](iam-core/README.md) — é lá que se lê o modelo, o contrato
de integração e as cinco consultas. Este arquivo trata da aplicação.

## Como rodar

```bash
mvn test                                        # 43 no núcleo, 104 no classroom

mvn -o compile
CP=$(find ~/.m2/repository -name '*.jar' | tr '\n' ':')
java -cp "iam-core/target/classes:classroom/target/classes:$CP" poo.Main   # porta 7000
```

O estado vive em memória. Ao subir, só existe o **ADMIN (id 1)**; a partir dele o
admin cria professores, e cada professor cria e matricula os próprios alunos. A
autenticação é um cookie `uid` — é um trabalho de faculdade, não um sistema de
login.

## A política desta aplicação

Ela não está em código. Está em
[`classroom/src/main/resources/politica-padrao.json`](classroom/src/main/resources/politica-padrao.json),
e é de lá que o `SecurityContext` a carrega na inicialização. São três políticas
nomeadas — `Administracao`, `Professor`, `Aluno` — anexadas ao usuário ADMIN e
aos dois grupos.

Três papéis fixos é decisão *desta* aplicação, não do núcleo. Para ele, ADMIN é
um usuário e Professores é um grupo, ambos com uma política anexada, e nada os
distingue de qualquer outro principal.

### A assimetria central

**Editar é do autor; excluir é da moderação.** O professor responsável apaga
qualquer post da turma dele, mas só edita os seus — corrigir texto alheio é do
administrador. Isso não é uma regra escrita em lugar nenhum: é o que resulta de
`EDITAR_POST` ser concedido com a condição `AUTOR` e `EXCLUIR_POST` com
`PROFESSOR_RESPONSAVEL ou AUTOR`.

### O administrador, por extenso

```
ALLOW  *                  *          modera tudo
DENY   CRIAR_*            TURMA      não cria conteúdo
DENY   CRIAR_ALUNO        *          nem aluno — quem cria é o professor
DENY   *MATRICULAR_ALUNO  *          nem monta turma
```

Quatro cláusulas no lugar de quinze concessões enumeradas. O curinga diz a
intenção, e o preço é que o efeito dele se espalha por ações que ainda não
existem — uma ação nova passa a ser permitida ao administrador sem que ninguém
escreva nada. `PoliticaDoAdminTest` fixa a lista por extenso justamente para
esse fato aparecer como uma linha que muda, e não em produção.

## As chaves de condição deste domínio

```
PROFESSOR_RESPONSAVEL   Igual                { "turma:professorId": "${principal:id}" }
ALUNO_MATRICULADO       ParaAlgumValor:Igual { "turma:alunoIds":    "${principal:id}" }
AUTOR                   Igual                { "recurso:autorId":   "${principal:id}" }
PROPRIO_USUARIO         Igual                { "recurso:id":        "${principal:id}" }
```

Quem traduz um objeto nessas chaves é o `ClassroomAttributes`; o núcleo só
enxerga texto. O `ContextResolver` sobe a corrente `Resource.getPai()`
(comentário → publicação → turma), então uma condição sobre a turma é avaliável
partindo de um comentário sem ninguém escrever essa navegação.

O `Utils` publica ainda `requisicao:ip`, `requisicao:metodo` e
`requisicao:caminho` — o que só a camada HTTP sabe. Nenhuma política atual as
usa; estão lá porque o custo é uma linha, e a alternativa é descobrir, quando
precisar de "só da rede da escola", que o dado nunca chegou até a decisão.

## As rotas de consulta

| pergunta | rota |
|---|---|
| o que posso fazer aqui? | `GET /permissoes?recurso=TURMA/1&recurso=POST/2` |
| por que foi negado? | idem, com `&explicar=true` |
| como é a política? | `GET /iam/politicas` (só ADMIN) |
| quem pode isto? | `GET /permissoes/quem-pode?acao=EXCLUIR_POST&recurso=POST/1` |
| sobre o que posso agir? | `GET /permissoes/onde-posso?acao=LISTAR_TURMAS` |

`GET /permissoes` conta ao usuário também o que ele **não** pode. É deliberado:
é o que permite a interface esconder um botão em vez de oferecer uma ação que
vai virar 403. O frontend não tem cópia nenhuma das regras — antes tinha
dezenove, reescritas em TypeScript.

## Motor e tabelas

Este projeto guarda tudo em memória, mas vale registrar onde cada abordagem
ganharia.

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
policy_statement(id, policy_id, sid, efeito, acao, recurso, condicao_json)
  INDEX (acao, recurso)
```

A divisão do trabalho: **o SQL filtra grosso** (quais cláusulas *poderiam* falar
sobre esta ação — indexável) e **o motor decide fino**. As condições nunca
executam em SQL.

A exceção é o `SqlWhereRenderer`, e ele mostra a simetria que sustenta a ideia:

```
ClassroomAttributes  lê      "turma:professorId" de dentro de uma Turma
ClassroomSqlMapping  escreve a mesma chave como turma.professor_id = ?
```

Um vocabulário, dois sentidos.

E o que um banco **não** melhoraria: a latência de uma checagem individual
pioraria (hoje é um `HashMap`); a correção não mudaria; a duplicação que existia
no frontend foi resolvida pelo endpoint de permissões efetivas, não por tabela
nenhuma; e `condicao_json` continua opaco ao SQL — justamente a coisa que mais
se queria consultável é a que a tabela não consulta.

## O que ficou de fora

Comparado ao IAM da AWS: ARNs completos (aqui a referência é `TIPO/id`, sem
partição, serviço nem região), *permission boundaries*, SCPs, federação, e o
raciocínio automatizado do Access Analyzer.

Do lado do classroom: as políticas no recurso e os papéis assumíveis existem no
núcleo e estão exercitados por teste, mas nenhuma tela os usa — a capacidade
precisa de exercício, não de interface.
