import { useEffect, useState } from 'react'
import { format, parseISO } from 'date-fns'
import { CalendarIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Calendar } from '@/components/ui/calendar'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import ErroApi from './ErroApi'

export type DadosAtividade = {
  titulo: string
  corpo: string
  dataEntrega: string // yyyy-MM-dd, o formato que o backend guarda
}

/**
 * Diálogo controlado: ele precisa fechar sozinho depois de salvar, e abrir já
 * preenchido quando está editando.
 */
export function AtividadeDialog({
  rotulo,
  titulo: tituloDialog,
  descricao,
  confirmar,
  valorInicial,
  onConfirmar,
  variante = 'outline',
}: {
  rotulo: string
  titulo: string
  descricao: string
  confirmar: string
  valorInicial?: DadosAtividade
  onConfirmar: (dados: DadosAtividade) => Promise<void>
  variante?: 'outline' | 'default'
}) {
  const [aberto, setAberto] = useState(false)
  const [titulo, setTitulo] = useState(valorInicial?.titulo ?? '')
  const [corpo, setCorpo] = useState(valorInicial?.corpo ?? '')
  const [data, setData] = useState<Date | undefined>(
    valorInicial?.dataEntrega ? parseISO(valorInicial.dataEntrega) : undefined,
  )
  const [erro, setErro] = useState<unknown>(null)
  const [salvando, setSalvando] = useState(false)

  // Reabrir precisa recomeçar do valor atual, não do que ficou da vez passada.
  useEffect(() => {
    if (!aberto) return
    setTitulo(valorInicial?.titulo ?? '')
    setCorpo(valorInicial?.corpo ?? '')
    setData(
      valorInicial?.dataEntrega
        ? parseISO(valorInicial.dataEntrega)
        : undefined,
    )
    setErro(null)
  }, [
    aberto,
    valorInicial?.titulo,
    valorInicial?.corpo,
    valorInicial?.dataEntrega,
  ])

  return (
    <Dialog open={aberto} onOpenChange={setAberto}>
      <DialogTrigger asChild>
        <Button variant={variante} size="sm">
          {rotulo}
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{tituloDialog}</DialogTitle>
          <DialogDescription>{descricao}</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="titulo" className="text-right">
              Título
            </Label>
            <Input
              id="titulo"
              className="col-span-3"
              value={titulo}
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
              value={corpo}
              onChange={(e) => setCorpo(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Entrega</Label>
            <SeletorData data={data} setData={setData} />
          </div>
        </div>
        {erro != null && <ErroApi erro={erro} />}
        <DialogFooter>
          <Button
            type="submit"
            disabled={salvando}
            onClick={async () => {
              if (!titulo.trim() || !corpo.trim() || !data) return
              setSalvando(true)
              setErro(null)
              try {
                await onConfirmar({
                  titulo: titulo.trim(),
                  corpo: corpo.trim(),
                  dataEntrega: format(data, 'yyyy-MM-dd'),
                })
                setAberto(false)
              } catch (e) {
                setErro(e)
              } finally {
                setSalvando(false)
              }
            }}
          >
            {salvando ? 'Salvando...' : confirmar}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function SeletorData({
  data,
  setData,
}: {
  data: Date | undefined
  setData: (data: Date | undefined) => void
}) {
  return (
    <Popover modal>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          className={cn(
            'col-span-3 justify-start text-left font-normal',
            !data && 'text-muted-foreground',
          )}
        >
          <CalendarIcon />
          {data ? format(data, 'dd/MM/yyyy') : <span>Escolher data</span>}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          mode="single"
          selected={data}
          onSelect={setData}
          initialFocus
        />
      </PopoverContent>
    </Popover>
  )
}

export function AlertaExclusao({
  onExcluir,
}: {
  onExcluir: () => void | Promise<void>
}) {
  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant="destructive" size="sm">
          Excluir
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Excluir a atividade?</AlertDialogTitle>
          <AlertDialogDescription>
            Isso não pode ser desfeito. A atividade e os comentários dela serão
            excluídos.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction onClick={() => onExcluir()}>
            Excluir
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
