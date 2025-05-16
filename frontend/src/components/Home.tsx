import { Link } from 'react-router'

export default function Home() {
  return (
    <main className="flex flex-col">
      {links.map((link) => (
        <Link key={link[0]} to={link[1]}>
          {link[0]}
        </Link>
      ))}
    </main>
  )
}

const links = [
  ['Usuários', '/usuarios'],
  ['Turmas', '/turmas'],
]
