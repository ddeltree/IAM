import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { cn } from '@/lib/utils'
import { useSessao } from '@/providers/SessaoProvider'
import { usePermissoes } from '@/hooks/usePermissoes'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import UsuarioAvatar from '@/components/UsuarioAvatar'
import type { Sessao } from '@/lib/types'

const ROTULO_PAPEL: Record<Sessao['papel'], string> = {
  ADMIN: 'Administrador',
  PROFESSOR: 'Professor',
  ALUNO: 'Aluno',
  DESCONHECIDO: 'Sem grupo',
}

export default function MainLayout() {
  const headerHeight = '56px'
  const sidebarWidth = '192px'
  const margin = '16px'

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
      {/* Cabeçalho fixo */}
      <div
        className={cn(
          'fixed top-0 right-[var(--margin)] left-[var(--margin)] pt-[var(--margin)]',
          'z-50 flex items-center justify-between border-b px-2',
          'h-[calc(var(--header-height)+var(--margin))]',
          'bg-background text-base',
        )}
      >
        <Link to="/">
          <h1 className="text-3xl font-medium">Sala de Aula</h1>
        </Link>
        <MenuDaSessao />
      </div>

      {/* Barra lateral fixa */}
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
        <Navegacao />
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

function Navegacao() {
  const { sessao } = useSessao()
  const { pode } = usePermissoes()
  if (!sessao) return null

  const criaProfessor = pode('CRIAR_PROFESSOR')
  const itens = [
    { rotulo: 'Turmas', para: '/', mostrar: true },
    {
      rotulo: 'Nova turma',
      para: '/turmas/nova',
      mostrar: pode('CRIAR_TURMA'),
    },
    {
      rotulo: 'Usuários',
      para: '/usuarios',
      mostrar: pode('LISTAR_USUARIOS'),
    },
    {
      rotulo: criaProfessor ? 'Novo professor' : 'Novo aluno',
      para: '/usuarios/novo',
      mostrar: criaProfessor || pode('CRIAR_ALUNO'),
    },
  ].filter((i) => i.mostrar)

  return (
    <nav className="flex flex-col gap-1">
      {itens.map((item) => (
        <NavLink
          key={item.para}
          to={item.para}
          end
          className={({ isActive }) =>
            cn(
              'rounded-md px-3 py-1.5 text-sm',
              isActive
                ? 'bg-accent text-accent-foreground font-medium'
                : 'hover:bg-accent/50',
            )
          }
        >
          {item.rotulo}
        </NavLink>
      ))}
    </nav>
  )
}

function MenuDaSessao() {
  const { sessao, sair } = useSessao()
  const navigate = useNavigate()
  const { pode } = usePermissoes(sessao ? [`USUARIO/${sessao.id}`] : [])
  if (!sessao) return null

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="ghost" className="h-auto gap-2 py-1">
          <UsuarioAvatar nome={sessao.name} className="h-8 w-8" />
          <span className="flex flex-col items-start leading-tight">
            <span className="text-sm font-medium">{sessao.name}</span>
            <span className="text-muted-foreground text-xs">
              {ROTULO_PAPEL[sessao.papel]} · #{sessao.id}
            </span>
          </span>
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-56 p-1">
        {/* O ADMIN não tem VER_PERFIL: o link só levaria a um 403. */}
        {pode('VER_PERFIL', `USUARIO/${sessao.id}`) && (
          <Button
            variant="ghost"
            className="w-full justify-start"
            onClick={() => navigate(`/usuarios/${sessao.id}`)}
          >
            Meu perfil
          </Button>
        )}
        <Button
          variant="ghost"
          className="w-full justify-start"
          onClick={() => navigate('/login')}
        >
          Trocar de usuário
        </Button>
        <Separator className="my-1" />
        <Button
          variant="ghost"
          className="text-destructive w-full justify-start"
          onClick={() => {
            sair()
            navigate('/login', { replace: true })
          }}
        >
          Sair
        </Button>
      </PopoverContent>
    </Popover>
  )
}
