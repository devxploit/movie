"use client"

import { ColumnDef } from "@tanstack/react-table"
import { Button } from "@/components/ui/button"
import { ArrowUpDown, Edit, Trash } from "lucide-react"

export type TvShow = {
    id: number
    name: string
    tmdbId: number
    posterPath: string
    userScore: number
    // Could add seasons count if API returns it
}

interface TvShowColumnsProps {
    onEdit: (tvShow: TvShow) => void
    onDelete: (id: number) => void
}

export const getTvShowColumns = ({ onEdit, onDelete }: TvShowColumnsProps): ColumnDef<TvShow>[] => [
    {
        accessorKey: "id",
        header: "ID",
        size: 60,
    },
    {
        accessorKey: "name",
        header: ({ column }) => {
            return (
                <Button
                    variant="ghost"
                    onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
                >
                    Title
                    <ArrowUpDown className="ml-2 h-4 w-4" />
                </Button>
            )
        },
        cell: ({ row }) => {
            const poster = row.original.posterPath
            return (
                <div className="flex items-center gap-2">
                    {poster && <img src={`https://image.tmdb.org/t/p/w92${poster}`} className="w-8 h-12 object-cover rounded" alt="" />}
                    <span className="font-medium">{row.getValue("name")}</span>
                </div>
            )
        }
    },
    {
        accessorKey: "tmdbId",
        header: "TMDB ID",
    },
    {
        accessorKey: "userScore",
        header: "Score",
        cell: ({ row }) => (
            <span className="inline-flex items-center rounded-md bg-blue-400/10 px-2 py-1 text-xs font-medium text-blue-400 ring-1 ring-inset ring-blue-400/20">
                {row.getValue("userScore")}
            </span>
        )
    },
    {
        id: "actions",
        cell: ({ row }) => {
            const tvShow = row.original
            return (
                <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => onEdit(tvShow)} className="bg-blue-900/50 border-blue-800 text-blue-200 hover:bg-blue-900">
                        <Edit className="h-4 w-4" />
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => onDelete(tvShow.id)} className="bg-red-900/50 border-red-800 text-red-200 hover:bg-red-900">
                        <Trash className="h-4 w-4" />
                    </Button>
                </div>
            )
        },
    },
]
