import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useUser } from '../providers/UserProvider'

export default function CriarTurma() {
  const [nome, setNome] = useState<string>('')
  const { user } = useUser()
  const navigate = useNavigate()
  return (
    <div className="flex flex-col items-center border w-96 h-96 justify-center gap-4">
      <label htmlFor="nome">
        Nome:
        <input
          type="text"
          className="border"
          onChange={(e) => setNome(e.target.value)}
        />
      </label>
      <button
        onClick={() => {
          if (!nome.trim() || !user) return
          criar(nome, user.id)
          navigate('/')
        }}
        className="border bg-amber-200 p-2"
      >
        Criar
      </button>
    </div>
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
