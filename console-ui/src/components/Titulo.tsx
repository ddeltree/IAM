import type { ReactNode } from 'react'

/** Cabeçalho de tela: o nome, e uma frase dizendo o que ela responde. */
export default function Titulo({
  titulo,
  explicacao,
  acao,
}: {
  titulo: string
  explicacao?: ReactNode
  acao?: ReactNode
}) {
  return (
    <div className="mb-6 flex items-start gap-4">
      <div>
        <h1 className="text-2xl font-semibold">{titulo}</h1>
        {explicacao && (
          <p className="text-muted-foreground mt-1 max-w-2xl text-sm">
            {explicacao}
          </p>
        )}
      </div>
      {acao && <div className="ml-auto">{acao}</div>}
    </div>
  )
}
