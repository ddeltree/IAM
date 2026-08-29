import type { Papel } from './types'

/**
 * Caderninho de ids, guardado no navegador.
 *
 * Só o ADMIN pode chamar GET /usuarios, então um professor não tem como
 * enumerar alunos para matricular — ele precisaria decorar os ids. Aqui
 * anotamos todo usuário que aparece (criado, logado ou listado pelo admin)
 * para oferecer uma lista em vez de um campo numérico cru.
 *
 * Isso é conveniência de interface, não autorização: o backend continua
 * exigindo o cookie e conferindo as permissões em toda chamada.
 */

const CHAVE = 'sala:conhecidos'

export interface Conhecido {
  id: string
  name: string
  papel: Papel
}

export function listarConhecidos(): Conhecido[] {
  try {
    const bruto = localStorage.getItem(CHAVE)
    return bruto ? (JSON.parse(bruto) as Conhecido[]) : []
  } catch {
    return []
  }
}

function gravar(lista: Conhecido[]) {
  localStorage.setItem(CHAVE, JSON.stringify(lista))
}

export function lembrar(conhecido: Conhecido) {
  const lista = listarConhecidos().filter((c) => c.id !== conhecido.id)
  lista.push(conhecido)
  lista.sort((a, b) => Number(a.id) - Number(b.id))
  gravar(lista)
}

/** Chamado quando um id deixa de existir (backend reiniciado, conta excluída). */
export function esquecer(id: string) {
  gravar(listarConhecidos().filter((c) => c.id !== id))
}

export const papelDoTipo = (tipo: 0 | 1): Papel =>
  tipo === 1 ? 'PROFESSOR' : 'ALUNO'
