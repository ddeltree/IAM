import { Skeleton } from './ui/skeleton'

export default function SkeletonCard() {
  return (
    <div className="flex h-[250px] flex-col space-y-3 rounded-xl border">
      <Skeleton className="h-[100px] w-[250px] rounded-xl" />
      <div className="m-4 space-y-2">
        <Skeleton className="h-4 w-[200px]" />
        <Skeleton className="h-4 w-[150px]" />
        <Skeleton className="h-4 w-[75px]" />
      </div>
    </div>
  )
}
