import { Skeleton } from './ui/skeleton'

export default function SkeletonCard() {
  return (
    <div className="flex items-center space-y-3 rounded-xl border-b px-8 py-1">
      <Skeleton className="h-16 w-16 rounded-full" />
      <div className="m-4 space-y-2">
        <Skeleton className="h-4 w-[200px]" />
        <Skeleton className="h-4 w-[150px]" />
      </div>
    </div>
  )
}
