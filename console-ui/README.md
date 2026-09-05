# console-ui

A interface do console de IAM. Vite + React na porta **5174**, falando com o
backend do console na **7001** (o proxy do `vite.config.ts` mantém tudo na mesma
origem).

Independente do `frontend/`, que é a interface do classroom e continua na 5173.

## Como rodar

```bash
# backend do console (porta 7001) — veja o README da raiz
cd backend && mvn install -DskipTests
mvn -pl console dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "iam-core/target/classes:console/target/classes:$(cat console/cp.txt)" poo.console.Main

# esta interface (porta 5174)
cd console-ui && pnpm install && pnpm dev
```

## As telas

**Simulador** — a tela central. Três blocos: a decisão; **todas** as cláusulas
que falam sobre a ação, marcando se ela mira outro recurso ou se a condição não
passou; e o **contexto resolvido**, que é o que transforma "a condição não
passou" em "ela comparou `recurso:dono=[ana]` com `principal:id=[bruno]`".

**Consultas** — as duas perguntas ao contrário. "Quem pode isto?" separa quem
pode agora de quem *chegaria* a poder assumindo um papel; juntar os dois
superestimaria o acesso numa auditoria. "Sobre o que posso agir?" devolve o
filtro derivado da política, os recursos que ele alcança, e o `WHERE`
correspondente — as três formas da mesma coisa.

**Principais** — usuários, grupos e papéis. A política de cada um aparece
separada por origem (inline, de cada grupo, de cada política anexada) antes de
ser somada: uma lista achatada esconderia o que "por que ele pode isso?" quer
saber.

**Políticas** — o editor. A condição se monta em **árvore**, com o documento
renderizado ao lado em leitura. Os dois juntos são deliberados: a árvore é o que
se opera, e o painel é o que se aponta para dizer "e isto é o que ela vira —
dado". Uma interface visual sozinha esconderia a propriedade que sustenta as
consultas reversas.

**Recursos** — os objetos, com os atributos que você criar. Cada atributo vira
uma chave de condição na hora, e um recurso pode estar *dentro* de outro — que é
como uma condição sobre o bucket alcança um objeto guardado nele.

**Vocabulário** — as ações e os tipos que existem, e a lista de chaves que uma
condição pode ler.

## O seletor de identidade

Não há login. O seletor no topo define de quem são todas as perguntas da tela —
sem ele, "pode?" não é uma pergunta completa. Quando a identidade é uma sessão
de papel, aparece um aviso: `principal:id` passou a ser o id da sessão, e uma
condição sobre o autor deixa de valer.
