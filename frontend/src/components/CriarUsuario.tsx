import { useState } from 'react'
import { useNavigate } from 'react-router'
import { Button } from './ui/button'
import { Input } from './ui/input'
import { Label } from './ui/label'
import { RadioGroup, RadioGroupItem } from './ui/radio-group'
import { useUser } from '@/providers/UserProvider'
import TituloFrame from './TituloFrame'

export default function CriarUsuarios() {
  const [nome, setNome] = useState<string>('')
  const [tipo, setTipo] = useState<string>('option-aluno')
  const navigate = useNavigate()
  const { user, setUser } = useUser()

  return (
    <TituloFrame titulo="Novo usuário">
      <div className="grid max-w-sm grid-cols-1 gap-2">
        <div className="grid gap-1">
          <Label htmlFor="nome" className="text-sm font-medium">
            Nome
          </Label>
          <Input
            id="nome"
            type="text"
            className="rounded border px-2 py-1"
            onChange={(e) => setNome(e.target.value)}
          />
        </div>

        <div className="flex flex-col">
          <Label htmlFor="option-aluno">Tipo</Label>
          <RadioGroup
            value={tipo}
            onValueChange={(value) => {
              setTipo(value)
            }}
            defaultValue="option-aluno"
            className="grid gap-1 rounded-lg p-2"
          >
            <div className="flex items-center space-x-2">
              <RadioGroupItem value="option-aluno" id="option-aluno" />
              <Label htmlFor="option-aluno">Aluno</Label>
            </div>
            <div className="flex items-center space-x-2">
              <RadioGroupItem value="option-professor" id="option-professor" />
              <Label htmlFor="option-professor">Professor</Label>
            </div>
          </RadioGroup>
        </div>
        <Button
          className="justify-self-end"
          onClick={async () => {
            if (!user || !nome.trim() || !tipo.trim()) return
            const usuario = await criar(
              user?.id,
              nome,
              tipo === 'option-aluno' ? 0 : 1,
            )
            setUser(usuario)
            navigate('/usuarios/' + usuario.id)
          }}
        >
          Criar
        </Button>
      </div>
    </TituloFrame>
  )
}

async function criar(autorId: string, name: string, tipo: 0 | 1) {
  console.log(name, tipo)
  const response = await fetch('http://localhost:7000/usuarios', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ name, tipo, autorId }),
  })
  return response.json()
}
