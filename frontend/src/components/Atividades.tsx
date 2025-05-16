import useSWR from 'swr'
import AtividadeSkeletonCard from './AtividadeSkeletonCard'
import _ from 'lodash'
import { Button } from './ui/button'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogTrigger,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Popover,
  PopoverTrigger,
  PopoverContent,
} from '@/components/ui/popover'
import { Calendar } from '@/components/ui/calendar'
import { Label } from '@radix-ui/react-label'
import { Input } from './ui/input'
import { Textarea } from './ui/textarea'
import { CalendarIcon } from 'lucide-react'
import { format } from 'date-fns'
import { useState } from 'react'
import { cn } from '@/lib/utils'
import { useParams } from 'react-router'

export default function Atividades() {
  const { data, error, isLoading } = useSWR('listar-atividades', listar)
  if (error) return <div>Erro ao listar atividades</div>
  if (isLoading)
    return (
      <div className="flex justify-center">
        <div className="w-full max-w-2xl space-y-2">
          {_.range(0, 5).map((i) => (
            <AtividadeSkeletonCard key={i} />
          ))}
        </div>
      </div>
    )
  return (
    <div className="flex justify-center">
      <div className="w-full max-w-2xl space-y-2">
        {data?.length === 0 && (
          <p className="italic">Nenhuma atividade encontrada</p>
        )}
        {data?.map((atividade) => (
          <div
            key={atividade.id}
            className="flex items-center space-y-3 rounded-xl border-b px-8 py-1"
          >
            <img
              className="h-16 w-16 rounded-full"
              src={atividade.imagem}
              alt={atividade.titulo}
            />
            <div className="m-4 space-y-2">
              <h2 className="text-xl font-semibold">{atividade.titulo}</h2>
              <p>{atividade.descricao}</p>
            </div>
          </div>
        ))}
        <div className="flex justify-end">
          <DialogDemo />
        </div>
      </div>
    </div>
  )
}

async function listar() {
  const response = await fetch('http://localhost:7000/atividades')
  const posts = await response.json()
  return posts as Record<string, any>[]
}

async function criar(
  titulo: string,
  corpo: string,
  turmaId: string,
  dataEntrega: Date,
) {
  const response = await fetch('http://localhost:7000/atividades', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      titulo,
      corpo,
      turmaId,
      dataEntrega: format(dataEntrega, 'yyyy-MM-dd'),
    }),
  })
  return response.json()
}

function DialogDemo() {
  const [date, setDate] = useState<Date>()
  const [titulo, setTitulo] = useState<string>('')
  const [corpo, setCorpo] = useState<string>('')
  const params = useParams()
  const turmaId = params['id']

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline">Nova atividade</Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Nova atividade</DialogTitle>
          <DialogDescription>
            Criar uma nova atividade para esta disciplina
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="titulo" className="text-right">
              Título
            </Label>
            <Input
              id="titulo"
              className="col-span-3"
              onChange={(e) => setTitulo(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-4 items-start gap-4">
            <Label htmlFor="corpo" className="text-right">
              Descrição
            </Label>
            <Textarea
              id="corpo"
              className="col-span-3"
              onChange={(e) => setCorpo(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="date" className="text-right">
              Data de Entrega
            </Label>
            <DatePickerDemo date={date} setDate={setDate} />
          </div>
        </div>
        <DialogFooter>
          <Button
            type="submit"
            onClick={() => {
              console.log(titulo, corpo, turmaId, date)
              if (!titulo.trim() || !corpo.trim() || !turmaId || !date) return
              criar(titulo, corpo, turmaId, date)
            }}
          >
            Criar atividade
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function DatePickerDemo({
  date,
  setDate,
}: {
  date: Date | undefined
  setDate: (date: Date | undefined) => void
}) {
  return (
    <Popover modal>
      <PopoverTrigger asChild>
        <Button
          variant={'outline'}
          className={cn(
            'w-[240px] justify-start text-left font-normal',
            !date && 'text-muted-foreground',
          )}
        >
          <CalendarIcon />
          {date ? format(date, 'PPP') : <span>Pick a date</span>}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          mode="single"
          selected={date}
          onSelect={setDate}
          initialFocus
        />
      </PopoverContent>
    </Popover>
  )
}
