import { cn } from '@/lib/utils'
import { Separator } from './ui/separator'

export default function TituloFrame({
  children,
  titulo,
  className,
}: {
  children: React.ReactNode
  titulo: string
  className?: string
}) {
  return (
    <>
      <h3 className="text-xl">{titulo}</h3>
      <Separator className="my-3" />
      <div className={cn('pl-8', className)}>{children}</div>
    </>
  )
}
