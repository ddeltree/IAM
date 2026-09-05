# IAM — sistema de autorização com uma sala de aula por cima

Trabalho de POO. O assunto é o **sistema de autorização**: um componente
genérico, inspirado no IAM da AWS, que decide se alguém pode uma ação sobre um
recurso — e que se deixa consultar. A sala de aula virtual existe para
exercitá-lo, não o contrário.

```
backend/iam-core/   o componente. Zero dependências de compilação.
backend/classroom/  o domínio (turmas, posts, atividades) e as rotas HTTP
frontend/           React + Vite, para operar tudo pelo navegador
```

## O que o núcleo faz

Uma **cláusula** de política tem os quatro campos da AWS: efeito, padrão de
ação, padrão de recurso e uma condição.

```
ALLOW  EDITAR_POST  POST  { "Igual": { "recurso:autorId": ["${principal:id}"] } }
```

A condição mora na concessão, e não na ação — é o que permite dar a mesma
permissão com restrições diferentes a cada papel. A decisão segue a ordem da
AWS: **negação explícita vence; na ausência das duas, nega.**

A condição é **dado**, não código. Por isso o motor não responde só sim ou não:

| pergunta | rota |
|---|---|
| posso? | usada internamente por toda rota |
| por que foi negado? | `GET /permissoes?recurso=POST/1&explicar=true` |
| o que posso fazer aqui? | `GET /permissoes?recurso=TURMA/1&recurso=POST/2` |
| quem pode isto? | `GET /permissoes/quem-pode?acao=EXCLUIR_POST&recurso=POST/1` |
| sobre o que posso agir? | `GET /permissoes/onde-posso?acao=LISTAR_TURMAS` |
| como é a política? | `GET /iam/politicas` (só ADMIN) |

A última devolve um filtro que vira predicado em memória **ou** cláusula
`WHERE` — a mesma restrição, dois destinos.

Tem ainda grupos, políticas nomeadas anexáveis, políticas no próprio recurso,
papéis assumíveis e sessões. Detalhes em
[`backend/iam-core/README.md`](backend/iam-core/README.md).

## Como executar

Precisa de **JDK 17+**, **Maven** e **pnpm**.

### Backend (porta 7000)

```bash
cd backend
mvn install -DskipTests
mvn -pl classroom dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "iam-core/target/classes:classroom/target/classes:$(cat classroom/cp.txt)" poo.Main
```

O `mvn install` é necessário uma vez porque o `classroom` depende do `iam-core`
como artefato. Depois, só o `java -cp` a cada mudança (com `mvn -o compile`
antes).

### Frontend (porta 5173)

```bash
cd frontend
pnpm install
pnpm dev
```

Abra <http://localhost:5173>. O Vite faz proxy de `/api` para o `:7000` — é o
que mantém tudo na mesma origem e faz o cookie de sessão chegar ao backend.

### Testes

```bash
cd backend && mvn test     # 43 no núcleo, 104 na aplicação
```

## Primeiros passos

O estado vive em memória e some quando o servidor para. Ao subir só existe o
**ADMIN**, com id `1`. Não há senha: a autenticação é o cookie `uid` com o id
do usuário — é um trabalho de faculdade, não um sistema de login.

1. entre como `1` e crie um **professor** — anote o id devolvido;
2. entre como o professor e crie uma **turma**;
3. ainda como professor, crie um **aluno** pela tela de Pessoas da turma — ele
   já entra matriculado;
4. entre como o aluno e publique algo no mural.

A partir daí dá para ver o desenho funcionando. No post do aluno, o professor
responsável **exclui mas não edita**: corrigir texto alheio é da moderação, que
é do administrador. Editar é do autor.

## Estrutura

```
backend/
  pom.xml                    pai, dois módulos
  iam-core/                  o componente — sem dependência nenhuma
    src/main/java/poo/iam/
      spi/                   o contrato de integração: 7 interfaces
      condition/             a árvore de condições (Composite + Visitor)
      document/              política ↔ documento, nos dois sentidos
      query/                 as consultas e os dois renderizadores
  classroom/
    src/main/java/poo/classroom/   o domínio
    src/main/java/poo/classroom/iam/   a adaptação ao núcleo
    src/main/java/poo/api/         as rotas HTTP
    src/main/resources/politica-padrao.json   a política, como dado
frontend/src/
  lib/ hooks/ providers/ components/
```
