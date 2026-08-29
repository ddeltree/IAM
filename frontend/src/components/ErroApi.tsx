import { Link } from 'react-router'
import { ApiError } from '@/lib/api'
import { cn } from '@/lib/utils'

/**
 * As mensagens do backend já vêm em português e em texto puro — quando existem,
 * mostramos a dele em vez de inventar outra.
 */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ApiError) {
    if (erro.status === 401) return 'Sua sessão expirou. Entre novamente.'
    if (erro.status === 403)
      return erro.message || 'Você não tem permissão para acessar isto.'
    return erro.message || 'Não foi possível concluir a operação.'
  }
  if (erro instanceof Error) return erro.message
  return 'Erro inesperado.'
}

export default function ErroApi({
  erro,
  className,
}: {
  erro: unknown
  className?: string
}) {
  const expirou = erro instanceof ApiError && erro.status === 401
  return (
    <div
      className={cn(
        'text-destructive rounded-lg border border-current/30 px-4 py-3 text-sm',
        className,
      )}
    >
      <p>{mensagemDeErro(erro)}</p>
      {expirou && (
        <Link to="/login" className="mt-1 inline-block underline">
          Ir para o login
        </Link>
      )}
    </div>
  )
}

/** Aviso para os casos que dá para barrar antes mesmo de chamar o backend. */
export function SemPermissao({ children }: { children?: React.ReactNode }) {
  return (
    <p className="text-muted-foreground italic">
      {children ?? 'Você não tem permissão para acessar esta página.'}
    </p>
  )
}
