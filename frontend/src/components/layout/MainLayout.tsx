import { Link, Outlet } from 'react-router'
import { useUser } from '../../providers/UserProvider'
import { cn } from '../../lib/utils'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import useSWR from 'swr'
import { useEffect, useState } from 'react'
import { ShieldUser, CircleUserRound } from 'lucide-react'
import { Button } from '../ui/button'

export default function MainLayout() {
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
        <UserPopover />
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
        Sidebar
      </div>

      <div
        className={cn(
          'ml-[calc(var(--sidebar-width)+var(--margin))]',
          'mt-[calc(var(--header-height)+var(--margin))]',
          'flex justify-center',
        )}
      >
        <div className="w-full max-w-5xl py-4">
          <Outlet />
        </div>
      </div>
    </div>
  )
}

function UserPopover() {
  const [usuarioId, setUsuarioId] = useState<string>('1')
  const { data: usuarios, mutate } = useSWR('usuarios', () =>
    listUsers(usuarioId),
  )
  const { setUser } = useUser()
  useEffect(() => {
    setUser(usuarios?.find((u) => u.id === usuarioId))
    mutate()
  }, [usuarios, usuarioId])
  return (
    <Select value={usuarioId} onValueChange={setUsuarioId} defaultValue="">
      <SelectTrigger className="border-0">
        <SelectValue placeholder="Selecionar usuário" />
      </SelectTrigger>
      <SelectContent>
        {usuarios?.map((u) => (
          <SelectItem key={u.id} value={u.id}>
            {u.tipo === 1 ? (
              <>
                <ShieldUser />
                <span className="text-muted-foreground text-xs">Professor</span>
              </>
            ) : u.tipo === 0 ? (
              <>
                <CircleUserRound />
                <span className="text-muted-foreground text-xs">Aluno</span>
              </>
            ) : (
              <CircleUserRound />
            )}
            {u.name}
          </SelectItem>
        ))}
        <Link to={'/usuarios'}>
          <Button variant="outline" size="icon" className="w-full">
            Novo
          </Button>
        </Link>
      </SelectContent>
    </Select>
  )
}

export async function listUsers(uid: string, turmaId?: string | undefined) {
  const query =
    `?id=${uid}` + (turmaId != undefined ? `&turmaId=${turmaId}` : '')
  const response = await fetch('http://localhost:7000/usuarios' + query)
  const users = await response.json()
  return users as Record<string, any>[]
}
