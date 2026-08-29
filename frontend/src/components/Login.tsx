import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { ApiError } from '@/lib/api'
import { esquecer, listarConhecidos } from '@/lib/conhecidos'
import { useSessao } from '@/providers/SessaoProvider'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { CircleUserRound, ShieldUser } from 'lucide-react'
import ErroApi from './ErroApi'

export default function Login() {
  const { sessao, entrar } = useSessao()
  const navigate = useNavigate()
  const location = useLocation()
  const [id, setId] = useState('')
  const [erro, setErro] = useState<unknown>(null)
  const [entrando, setEntrando] = useState(false)
  const conhecidos = listarConhecidos()

  const destino = (location.state as { de?: string } | null)?.de ?? '/'
  if (sessao) return <Navigate to={destino} replace />

  async function autenticar(uid: string) {
    const limpo = uid.trim()
    if (!limpo) return
    setEntrando(true)
    setErro(null)
    try {
      await entrar(limpo)
      navigate(destino, { replace: true })
    } catch (e) {
      // O backend guarda tudo em memória: se ele reiniciou, os ids antigos
      // deixaram de existir e não faz sentido continuar oferecendo este.
      if (e instanceof ApiError && e.status === 404) esquecer(limpo)
      setErro(e)
    } finally {
      setEntrando(false)
    }
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col justify-center gap-6 p-6">
      <div>
        <h1 className="text-3xl font-semibold">Sala de Aula</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          A identificação é feita pelo id do usuário — é ele que vai no cookie
          que o backend lê em cada requisição.
        </p>
      </div>

      <form
        className="space-y-3"
        onSubmit={(e) => {
          e.preventDefault()
          autenticar(id)
        }}
      >
        <div className="grid gap-1">
          <Label htmlFor="uid">Seu id</Label>
          <Input
            id="uid"
            inputMode="numeric"
            placeholder="ex: 2"
            value={id}
            onChange={(e) => setId(e.target.value)}
          />
        </div>
        <Button type="submit" className="w-full" disabled={entrando}>
          {entrando ? 'Entrando...' : 'Entrar'}
        </Button>
      </form>

      {erro != null && <ErroApi erro={erro} />}

      {conhecidos.length > 0 && (
        <div className="space-y-2">
          <Separator />
          <p className="text-muted-foreground text-xs">
            Usados neste navegador
          </p>
          <ul className="grid gap-1">
            {conhecidos.map((c) => (
              <li key={c.id}>
                <Button
                  variant="outline"
                  className="w-full justify-start gap-2"
                  onClick={() => autenticar(c.id)}
                  disabled={entrando}
                >
                  {c.papel === 'ALUNO' ? <CircleUserRound /> : <ShieldUser />}
                  <span>{c.name}</span>
                  <span className="text-muted-foreground ml-auto text-xs">
                    #{c.id}
                  </span>
                </Button>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="text-muted-foreground space-y-2 text-xs">
        <Separator />
        <p>
          Num backend recém-iniciado só existe o <strong>ADMIN (id 1)</strong>.
          A partir dele: o admin cria professores, e cada professor cria e
          matricula os próprios alunos.
        </p>
        <Button
          variant="ghost"
          size="sm"
          className="px-0"
          onClick={() => autenticar('1')}
          disabled={entrando}
        >
          Entrar como ADMIN (id 1)
        </Button>
      </div>
    </div>
  )
}
