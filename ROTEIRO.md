# Roteiro de apresentação

Cinco minutos no [console](console-ui/), a partir do cenário semente. A ordem
importa: cada passo prepara o seguinte.

**Antes de começar:** suba o backend do console (`:7001`) e a interface
(`:5174`), abra <http://localhost:5174> e clique em **Reiniciar cenário**. Se
algo tiver sido mexido num ensaio, isso desfaz.

O cenário é um serviço de arquivos. Três usuárias — **Ana**, **Bruno**,
**Carla** —, dois grupos (**Leitores**, **Escritores**), quatro recursos e um
papel de **Auditor**.

---

## 1. A tese, em uma frase · 20s

> "O trabalho não é a sala de aula nem este console. É o **núcleo de
> autorização** — uma biblioteca sem dependência nenhuma, que decide quem pode o
> quê. Estes dois são aplicações dele, e este aqui não tem domínio nenhum: tudo
> que você vai ver na tela foi digitado."

Aba **Vocabulário**: as ações e os tipos. Nada disso está em código.

## 2. Uma decisão, e por que ela foi tomada · 90s

Aba **Simulador**. Identidade no topo: **Bruno**.

- Permissão `ESCREVER · BUCKET`, recurso `BUCKET/relatorios`. → **Negado**.
- Aponte o bloco do meio: há **uma cláusula** falando sobre escrever, vinda do
  grupo Escritores, e ela diz *condição não passou*.
- Aponte o bloco de baixo, o **contexto resolvido**:
  `recurso:dono = [ana]` contra `principal:id = [bruno]`.

> "Não é 'negado'. É: existe uma regra, ela alcança este recurso, e ela comparou
> estes dois valores."

- Troque a identidade para **Ana**, mesma pergunta. → **Permitido**, e a mesma
  cláusula agora aparece marcada como *decidiu*.

> "A mesma cláusula, para as duas. O que muda é o contexto — e é isso que
> permite uma regra só servir a todo mundo, em vez de uma linha por pessoa."

## 3. A condição é dado, não código · 60s

Aba **Políticas** → **editar** em `DonoMandaNoSeu`.

- A condição está montada em árvore: chave, operador, valor.
- Aponte o painel à direita:

```json
{ "Igual": { "recurso:dono": ["${principal:id}"] } }
```

> "Isto não é um `if` compilado. É um documento. Ele se guarda, se transmite, se
> lê de volta — e, principalmente, **se inspeciona**. É o que torna possível a
> próxima tela."

## 4. As duas perguntas que ninguém responde · 90s

Aba **Consultas**, identidade **Ana**.

**Quem pode isto?** `LER · BUCKET` sobre `BUCKET/relatorios`.

- Podem agora: **Ana**.
- Chegariam assumindo um papel: **Auditor → Ana, Bruno, Carla**.

> "Separados de propósito. 'Quem pode' e 'quem consegue chegar a poder' são
> perguntas diferentes, e uma auditoria que as confunde superestima o acesso."

**Sobre o que posso agir?** `ESCREVER · BUCKET`.

- A restrição: `recurso:dono = ana`
- O mesmo, em SQL: `SELECT * FROM recurso WHERE recurso.dono = 'ana'`

> "Ele não devolveu uma lista — devolveu o **filtro**. O mesmo objeto vira um
> predicado em memória ou uma cláusula WHERE. É o que permitiria a política valer
> dentro do banco sem que nada mais mudasse."

## 5. Compartilhar sem tocar na política de ninguém · 40s

Aba **Recursos**: `BUCKET/folha` tem política **própria**, compartilhando com a
Carla.

Simulador, identidade **Carla**, `LER · BUCKET` sobre `BUCKET/folha` →
**Permitido**, e a origem é `BUCKET/folha`.

Aba **Principais** → Carla → *política efetiva*: nada sobre a folha.

> "O dono do objeto concedeu, sobre o objeto. A política da Carla não mudou —
> ela nem sabe que isso aconteceu."

## 6. Assumir um papel · 50s

Identidade **Carla**. Simulador: `LER · BUCKET` sobre `BUCKET/relatorios` →
**Negado**.

Aba **Principais** → aba Papéis → **assumir como Carla**.

- A faixa no topo muda de cor.
- Mesma pergunta, agora como a sessão → **Permitido**.
- No contexto: `principal:id` é o id **da sessão**, e `sessao:origem` é `carla`.

> "Ela não virou outra pessoa: ela está **exercendo** um papel. E repare que uma
> regra do tipo 'o autor pode editar' deixa de valer aqui — porque quem está
> agindo não é mais ela. É essa distinção que separa 'o professor é moderador'
> de 'o professor pode passar a moderar quando precisar'."

Clique em **largar sessão**.

## 7. O fecho · 30s

> "Nada do que vocês viram está no núcleo. Ele não sabe o que é um bucket, um
> dono ou um auditor — ele compara nomes e avalia condições. A outra aplicação
> deste mesmo núcleo é uma sala de aula com turmas e posts, e ela roda ao lado
> sem saber que este console existe."

Se sobrar tempo: mostre `backend/iam-core/pom.xml` — a lista de dependências,
que é vazia.

---

## Se algo der errado

| sintoma | o que fazer |
|---|---|
| a tela não carrega | o backend do console está de pé na 7001? |
| um resultado estranho | **Reiniciar cenário**, no topo à direita |
| "não existe recurso X" | o seletor de recurso filtra pelo tipo da permissão escolhida |
| travou numa política | apague-a na aba Políticas; ela se desanexa de todos junto |

## Perguntas que provavelmente virão

**"Isso não é só um CRUD de permissões?"** — Um CRUD responde "pode?". As duas
consultas da aba Consultas não são respondíveis por uma tabela de permissões: a
segunda devolve um filtro derivado da política, não uma lista.

**"Por que não usar uma tabela?"** — Uma tabela resolve bem a metade RBAC. O que
ela não expressa é a condição: "pode editar **se for o autor**" não é uma linha,
é um predicado sobre os dados da linha. As saídas seriam materializar uma ACL
por instância, ou espalhar `if` pelos controllers.

**"E o desempenho?"** — Uma decisão percorre a política do principal, que é
pequena. As consultas reversas podam antes de avaliar, e a poda só pode
*encolher* o conjunto: o motor confirma cada sobrevivente, então um erro na poda
custa desempenho e nunca acesso indevido.

**"Quanto disso é o núcleo e quanto é a aplicação?"** — `backend/iam-core` tem
zero dependências de compilação; `console` e `classroom` dependem dele e não um
do outro. As sete interfaces de `poo.iam.spi` são tudo o que uma aplicação
precisa implementar.
