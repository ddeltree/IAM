/**
 * O contrato de integração: o que uma aplicação implementa para plugar o núcleo.
 *
 * Tudo mais em {@code poo.iam} o núcleo oferece pronto. O que está aqui é o
 * inverso — são as coisas que só a aplicação sabe, e que o núcleo pede a ela:
 *
 * <ul>
 *   <li>{@link poo.iam.spi.AttributeProvider} — como ler os atributos de um
 *       recurso do domínio, para que as condições possam falar sobre ele sem
 *       que o núcleo conheça o tipo;</li>
 *   <li>{@link poo.iam.spi.PrincipalDirectory} — onde estão os usuários e
 *       grupos, para as consultas ao contrário ("quem pode isto?");</li>
 *   <li>{@link poo.iam.spi.SqlMapping} — como a mesma chave de condição se
 *       escreve em SQL, o espelho exato do {@code AttributeProvider}.</li>
 * </ul>
 *
 * A regra deste pacote, fixada por teste: <b>só interfaces, e nada importado
 * de fora do vocabulário de {@code poo.iam}</b>. É o que mantém a promessa de
 * que o contrato de integração cabe numa página — quem for adaptar o núcleo a
 * outro domínio lê este pacote e sabe exatamente o que precisa escrever.
 */
package poo.iam.spi;
