import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useUser } from '../providers/UserProvider'
import { Button } from './ui/button'
import { Input } from './ui/input'
import { Label } from './ui/label'
import TituloFrame from './TituloFrame'

export default function CriarTurma() {
  const [nome, setNome] = useState<string>('')
  const { user } = useUser()
  const navigate = useNavigate()
  return (
    <TituloFrame titulo="Nova turma">
      <div className="flex max-w-sm flex-col gap-2">
        <Label htmlFor="nome" className="space-x-1">
          <span>Nome:</span>
          <Input type="text" onChange={(e) => setNome(e.target.value)} />
        </Label>
        <Button
          className="self-end"
          onClick={async () => {
            if (!nome.trim() || !user) return
            await criar(nome, user.id)
            navigate('/')
          }}
        >
          Criar
        </Button>
      </div>
    </TituloFrame>
  )
}

async function criar(nome: string, professorId: string) {
  const response = await fetch('http://localhost:7000/turmas', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ nome, professorId }),
  })
  return response.json()
}
