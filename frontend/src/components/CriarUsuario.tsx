import { useState } from 'react'
import { useNavigate } from 'react-router'

export default function CriarUsuarios() {
  const [nome, setNome] = useState<string>('')
  const [tipo, setTipo] = useState<string>('')
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
      <label htmlFor="tipo">
        Tipo:
        <input
          type="text"
          className="border"
          onChange={(e) => setTipo(e.target.value)}
        />
      </label>
      <button
        onClick={async () => {
          if (!nome.trim() || !tipo.trim()) return
          const usuario = await criar(nome, tipo)
          navigate('/usuarios/' + usuario.id)
        }}
        className="border bg-amber-200 p-2"
      >
        Criar
      </button>
    </div>
  )
}

async function criar(name: string, tipo: string) {
  const response = await fetch('http://localhost:7000/usuarios', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ name, tipo }),
  })
  return response.json()
}
