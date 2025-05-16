import { Link } from 'react-router'

export default function ListLayout({
  children,
  href,
}: {
  children: React.ReactNode
  href: string
}) {
  return (
    <>
      <h1 className="text-3xl font-bold underline">Hello world!</h1>
      <div className="flex justify-center border gap-4">{children}</div>
      <div className="flex gap-0.5">
        <Link to={href}>
          <button className="border bg-amber-200 p-2">Criar</button>
        </Link>
      </div>
    </>
  )
}
