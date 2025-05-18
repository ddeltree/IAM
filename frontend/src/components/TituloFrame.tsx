import { Separator } from './ui/separator'

export default function TituloFrame({
  children,
  titulo,
}: {
  children: React.ReactNode
  titulo: string
}) {
  return (
    <>
      <h3 className="text-xl">{titulo}</h3>
      <Separator className="my-3" />
      <div className="pl-8">{children}</div>
    </>
  )
}
