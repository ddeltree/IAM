import { Link, Outlet } from 'react-router'
import { cn } from '../../lib/utils'

export default function TurmaLayout() {
  const secondaryHeaderHeight = '48px'

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
        )}
      >
        {links.map((link) => (
          <Link key={link[0]} to={link[1]}>
            {link[0]}
          </Link>
        ))}
      </div>

      <div className="pt-4">
        <Outlet />
      </div>
    </div>
  )
}

const links = [
  ['Mural', ''],
  ['Atividades', 'atividades'],
  ['Pessoas', 'pessoas'],
]
