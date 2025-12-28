"use client"

import { ColumnDef } from "@tanstack/react-table"
import { Button } from "@/components/ui/button" // Ensure Button import
import { ArrowUpDown, UserCog, Trash } from "lucide-react"

export type User = {
    id: number
    email: string
    role: "ADMIN" | "USER"
    subscriptionStatus: "ACTIVE" | "INACTIVE"
    lastLogin: string | null
}

interface UserColumnsProps {
    onEdit: (user: User) => void
    onDelete: (id: number) => void
}

export const getColumns = ({ onEdit, onDelete }: UserColumnsProps): ColumnDef<User>[] => [
    {
        accessorKey: "id",
        header: "ID",
    },
    {
        accessorKey: "email",
        header: ({ column }) => {
            return (
                <Button
                    variant="ghost"
                    onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
                >
                    Email
                    <ArrowUpDown className="ml-2 h-4 w-4" />
                </Button>
            )
        },
    },
    {
        accessorKey: "role",
        header: "Role",
        cell: ({ row }) => {
            const role = row.getValue("role") as string
            return (
                <span className={role === 'ADMIN' ? 'text-blue-400 font-bold' : 'text-green-400'}>
                    {role}
                </span>
            )
        }
    },
    {
        accessorKey: "subscriptionStatus",
        header: "Status",
        cell: ({ row }) => {
            const status = row.getValue("subscriptionStatus") as string
            return (
                <span className={status === 'ACTIVE' ? 'text-green-400' : 'text-red-400'}>
                    {status}
                </span>
            )
        }
    },
    {
        accessorKey: "lastLogin",
        header: "Last Login",
        cell: ({ row }) => {
            const date = row.getValue("lastLogin")
            if (!date) return <span className="text-gray-500">-</span>
            return new Date(date as string).toLocaleDateString()
        }
    },
    {
        id: "actions",
        cell: ({ row }) => {
            const user = row.original
            return (
                <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => onEdit(user)} className="bg-blue-900/50 border-blue-800 text-blue-200 hover:bg-blue-900">
                        <UserCog className="h-4 w-4" />
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => onDelete(user.id)} className="bg-red-900/50 border-red-800 text-red-200 hover:bg-red-900">
                        <Trash className="h-4 w-4" />
                    </Button>
                </div>
            )
        },
    },
]
