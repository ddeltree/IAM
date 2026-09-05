/**
 * A política como documento — o que a AWS chama de <em>policy document</em>.
 *
 * A forma é de {@link java.util.Map}, {@link java.util.List} e texto, e não de
 * alguma árvore de biblioteca de JSON. De propósito: o núcleo descreve o
 * documento sem escolher com o que a aplicação vai serializá-lo, e quem
 * transforma isso em texto é ela, com o mapeador que já usa.
 *
 * Isto é a razão de as condições terem virado dado. Enquanto eram lambdas, a
 * política só existia como código Java e não havia o que imprimir.
 */
package poo.iam.document;
