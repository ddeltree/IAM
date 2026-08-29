import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { cn } from '@/lib/utils'

function iniciais(nome: string) {
  const partes = nome.trim().split(/\s+/).filter(Boolean)
  if (partes.length === 0) return '?'
  if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase()
  return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase()
}

export default function UsuarioAvatar({
  nome,
  className,
}: {
  nome: string
  className?: string
}) {
  return (
    <Avatar className={cn('h-10 w-10', className)}>
      <AvatarFallback className="bg-accent text-accent-foreground text-sm font-medium">
        {iniciais(nome)}
      </AvatarFallback>
    </Avatar>
  )
}
