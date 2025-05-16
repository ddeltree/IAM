import { Link, Outlet } from 'react-router'
import { useUser } from '../../providers/UserProvider'
import { cn } from '../../lib/utils'

export default function MainLayout() {
  const { user } = useUser()
  const headerHeight = '44px' // 16 * 4 = 64px
  const sidebarWidth = '192px' // 48 * 4 = 192px
  const margin = '16px' // 4 * 4 = 16px outer margin
  return (
    <div
      className={cn('relative m-4')}
      style={
        {
          '--header-height': headerHeight,
          '--sidebar-width': sidebarWidth,
          '--margin': margin,
        } as React.CSSProperties
      }
    >
      {/* Fixed Header */}
      <div
        className={cn(
          'fixed top-0 right-[var(--margin)] left-[var(--margin)] pt-[var(--margin)]',
          'z-50 flex items-baseline justify-between border-b px-2',
          'h-[calc(var(--header-height)+var(--margin))]',
          'bg-background text-base',
        )}
      >
        <div className="flex items-baseline gap-4">
          <Link to={'/'}>
            <h1 className="font-large text-3xl">Sala de Aula</h1>
          </Link>
          <h3 className="text-lg font-semibold">Turma</h3>
        </div>
        {user ? (
          <div>
            Usuário: <b className="font-black">{user.name}</b>
          </div>
        ) : (
          <Link to="/usuarios">
            <div>Usuário não escolhido</div>
          </Link>
        )}
      </div>

      {/* Fixed Sidebar */}
      <div
        className={cn(
          'fixed top-[calc(var(--header-height)+var(--margin))]',
          'bottom-[var(--margin)] left-[var(--margin)]',
          'z-40 border-r',
          'w-[var(--sidebar-width)]',
          'p-2',
          'bg-background text-base',
        )}
      >
        a
      </div>

      <div
        className={cn(
          'ml-[calc(var(--sidebar-width)+var(--margin))]',
          'mt-[calc(var(--header-height)+var(--margin))]',
          'flex justify-center',
        )}
      >
        <div className="flex w-full max-w-5xl flex-col items-center">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
