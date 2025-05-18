import { Link } from 'react-router'
import { Button } from '@/components/ui/button'

export default function Home() {
  return (
    <main className="flex flex-col gap-0.5">
      {links.map((link) => (
        <Link key={link[0]} to={link[1]}>
          <Button variant="default" className="w-32">
            {link[0]}
          </Button>
        </Link>
      ))}
    </main>
  )
}

const links = [
  ['Usuários', '/usuarios'],
  ['Turmas', '/turmas'],
]
