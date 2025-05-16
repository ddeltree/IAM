import { Link, Outlet, useParams } from 'react-router'
import { cn } from '../../lib/utils'
import useSWR from 'swr'

export default function TurmaLayout() {
  const secondaryHeaderHeight = '48px'
  const { id } = useParams()
  const { data } = useSWR(`turmas/${id}`, () => buscarTurma(id))
  return (
    <div
      style={
        {
          '--secondary-header-height': secondaryHeaderHeight,
          paddingTop: secondaryHeaderHeight,
        } as React.CSSProperties
      }
      className="w-full"
    >
      <div
        className={cn(
          'fixed flex w-full items-center gap-8 border-b px-16',
          'left-[calc(var(--margin)+var(--sidebar-width))]',
          'top-[calc(var(--header-height)+var(--margin))]',
          'h-[var(--secondary-header-height)]',
          'bg-background text-base shadow-xs',
        )}
      >
        {links.map((link) => (
          <Link key={link[0]} to={link[1]} className="font-semibold">
            {link[0]}
          </Link>
        ))}
      </div>

      <div className="pt-6">
        <Outlet context={data} />
      </div>
    </div>
  )
}

const links = [
  ['Mural', ''],
  ['Atividades', 'atividades'],
  ['Pessoas', 'pessoas'],
]

async function buscarTurma(id: string | undefined) {
  if (!id) return
  const response = await fetch(`http://localhost:7000/turmas/${id}`)
  const turmas = await response.json()
  return turmas as Record<string, any>[]
}
