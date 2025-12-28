"use client"

import { useEffect, useState } from "react"
import { DataTable } from "@/components/ui/data-table"
import { getColumns, User } from "@/components/users/columns"
import { Button } from "@/components/ui/button"
import { UserPlus } from "lucide-react"
import { UserDialog } from "@/components/users/user-dialog"
import { API_URL } from "@/lib/config"

export default function UsersPage() {
    const [data, setData] = useState<User[]>([])
    const [loading, setLoading] = useState(true)
    const [modalOpen, setModalOpen] = useState(false)
    const [editingUser, setEditingUser] = useState<User | null>(null)

    const fetchUsers = async () => {
        setLoading(true)
        try {
            // Assuming direct access or proxy
            const token = localStorage.getItem('authToken')
            const res = await fetch(`${API_URL}/users?size=100`, {
                headers: { 'Authorization': `Bearer ${token}` }
            })
            if (res.ok) {
                const json = await res.json()
                setData(json.content)
            }
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchUsers()
    }, [])

    const handleEdit = (user: User) => {
        setEditingUser(user)
        setModalOpen(true)
    }

    const handleDelete = async (id: number) => {
        if (!confirm("Are you sure?")) return
        try {
            const token = localStorage.getItem('authToken')
            await fetch(`${API_URL}/users/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            })
            fetchUsers()
        } catch (err) { console.error(err) }
    }

    const handleSave = async (user: Partial<User> & { password?: string }) => {
        const isEdit = !!user.id
        const url = isEdit ? `${API_URL}/users/${user.id}` : `${API_URL}/users`
        const method = isEdit ? 'PUT' : 'POST'

        try {
            const token = localStorage.getItem('authToken')
            const res = await fetch(url, {
                method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(user)
            })
            if (res.ok) {
                setModalOpen(false)
                fetchUsers()
            } else {
                alert("Error saving user")
            }
        } catch (err) { console.error(err) }
    }

    const handleAddNew = () => {
        setEditingUser(null)
        setModalOpen(true)
    }

    const columns = getColumns({ onEdit: handleEdit, onDelete: handleDelete })

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-3xl font-bold">Users Management</h1>
                <Button onClick={handleAddNew} className="bg-indigo-600 hover:bg-indigo-700">
                    <UserPlus className="mr-2 h-4 w-4" /> Add User
                </Button>
            </div>

            {loading ? (
                <div className="text-center text-gray-500">Loading...</div>
            ) : (
                <DataTable columns={columns} data={data} searchKey="email" />
            )}

            <UserDialog
                open={modalOpen}
                onOpenChange={setModalOpen}
                user={editingUser}
                onSave={handleSave}
            />
        </div>
    )
}
