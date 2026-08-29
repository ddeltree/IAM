import { Link } from 'react-router'
import { Button } from '@/components/ui/button'

export default function NaoEncontrado() {
  return (
    <div className="space-y-4 py-16 text-center">
      <h2 className="text-2xl font-semibold">Página não encontrada</h2>
      <p className="text-muted-foreground">
        O endereço que você abriu não existe neste sistema.
      </p>
      <Link to="/">
        <Button>Voltar para as turmas</Button>
      </Link>
    </div>
  )
}
