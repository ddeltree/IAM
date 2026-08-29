import { Navigate, Outlet, useLocation } from 'react-router'
import { useSessao } from '@/providers/SessaoProvider'

export default function RotaProtegida() {
  const { sessao } = useSessao()
  const location = useLocation()

  if (!sessao)
    return <Navigate to="/login" replace state={{ de: location.pathname }} />
  return <Outlet />
}
