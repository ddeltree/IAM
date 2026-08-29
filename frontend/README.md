# Sala de Aula — frontend

Interface React do trabalho de POO. O assunto do projeto é o sistema de
autorização (IAM) do backend; esta interface existe para exercitá-lo.

## Como rodar

```bash
# backend (porta 7000)
cd backend
mvn compile dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp target/classes:$(cat cp.txt) poo.Main

# frontend (porta 5173)
cd frontend
pnpm install
pnpm dev
```

## Autenticação

O backend identifica quem está chamando pelo cookie `uid`, cujo valor é o id do
usuário — não há rota de login, senha nem token. A tela `/login` grava esse
cookie e descobre o papel de quem entrou usando o próprio sistema de permissões:

- `GET /usuarios/{id}` responde 200 para aluno e professor (cada um só enxerga o
  próprio perfil), 403 para o ADMIN — que é o único papel sem `VER_PERFIL` — e
  404 se o id não existir.

O app fala com `/api/*` na própria origem e o Vite faz o proxy para o `:7000`
(`vite.config.ts`). Isso mantém tudo same-origin e é o que faz o cookie chegar
ao backend: o CORS dele é `anyHost()`, que o navegador recusa junto com
credenciais. Por isso o app funciona em `pnpm dev` e `pnpm preview`; servir o
`dist/` por outro servidor exigiria repassar `/api` lá também.

## Primeiros passos num backend recém-iniciado

O estado fica em memória e, ao subir, só existe o **ADMIN (id 1)**:

1. entre como `1` e crie um **professor** — anote o id devolvido;
2. entre com o id do professor e crie uma **turma**;
3. na aba **Pessoas** da turma, use "criar aluno e matricular";
4. entre com o id do aluno para ver a turma do outro lado.

A ordem não é acidental: só o admin tem `CRIAR_PROFESSOR` e só professores têm
`CRIAR_ALUNO`.

## Permissões na interface

`src/lib/permissoes.ts` espelha as regras de `poo.iam.SystemPermission` para
mostrar ou esconder botões. É só dica visual — quem decide continua sendo o
backend, e toda chamada trata o 403 que vier (`src/components/ErroApi.tsx`).

Vale notar a assimetria que o modelo impõe: **editar é do autor, excluir é da
moderação**. O professor responsável apaga qualquer post da turma, mas só edita
os seus. E o ADMIN não cria conteúdo nenhum — ele lista, edita e exclui.

Como só o ADMIN pode chamar `GET /usuarios`, um professor não tem como enumerar
alunos para matricular. `src/lib/conhecidos.ts` guarda no navegador os ids já
vistos, para que a tela ofereça uma lista em vez de um campo numérico cru.
