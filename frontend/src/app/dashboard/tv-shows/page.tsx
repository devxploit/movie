"use client"

import { useEffect, useState } from "react"
import { DataTable } from "@/components/ui/data-table"
import { getTvShowColumns, TvShow } from "@/components/tv-shows/columns"
import { Button } from "@/components/ui/button"
import { Plus } from "lucide-react"
import { TvShowDialog } from "@/components/tv-shows/tv-show-dialog"
import { API_URL } from "@/lib/config"

export default function TvShowsPage() {
    const [data, setData] = useState<TvShow[]>([])
    const [pageIndex, setPageIndex] = useState(0)
    const [pageSize, setPageSize] = useState(20)
    const [pageCount, setPageCount] = useState(0)
    const [totalItems, setTotalItems] = useState(0)
    const [loading, setLoading] = useState(true)
    const [modalOpen, setModalOpen] = useState(false)
    const [editingTvShow, setEditingTvShow] = useState<TvShow | null>(null)

    const fetchTvShows = async () => {
        setLoading(true)
        try {
            const token = localStorage.getItem('authToken')
            const res = await fetch(`${API_URL}/tvshows?page=${pageIndex}&size=${pageSize}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            })
            if (res.ok) {
                const json = await res.json()
                setData(json.content)
                setPageCount(json.totalPages)
                setTotalItems(json.totalElements)
            }
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchTvShows()
    }, [pageIndex, pageSize])

    const handleEdit = (tvShow: TvShow) => {
        setEditingTvShow(tvShow)
        setModalOpen(true)
    }

    const handleDelete = async (id: number) => {
        if (!confirm("Are you sure?")) return
        try {
            const token = localStorage.getItem('authToken')
            await fetch(`${API_URL}/tvshows/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            })
            fetchTvShows()
        } catch (err) { console.error(err) }
    }

    const handleSave = async (tvShow: Partial<TvShow>) => {
        const isEdit = !!tvShow.id
        const url = isEdit ? `${API_URL}/tvshows/${tvShow.id}` : `${API_URL}/tvshows`
        const method = isEdit ? 'PUT' : 'POST'

        try {
            const token = localStorage.getItem('authToken')
            const res = await fetch(url, {
                method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(tvShow)
            })
            if (res.ok) {
                setModalOpen(false)
                fetchTvShows()
            } else {
                alert("Error saving TV Show")
            }
        } catch (err) { console.error(err) }
    }

    const handleAddNew = () => {
        setEditingTvShow(null)
        setModalOpen(true)
    }

    const columns = getTvShowColumns({ onEdit: handleEdit, onDelete: handleDelete })

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold">TV Shows</h1>
                    <p className="text-muted-foreground">Total TvShows: {totalItems}</p>
                </div>
                <Button onClick={handleAddNew} className="bg-indigo-600 hover:bg-indigo-700">
                    <Plus className="mr-2 h-4 w-4" /> Add TV Show
                </Button>
            </div>

            {loading ? (
                <div className="text-center text-gray-500">Loading...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={data}
                    searchKey="name"
                    pageCount={pageCount}
                    pagination={{ pageIndex, pageSize }}
                    onPaginationChange={(updater) => {
                        if (typeof updater === 'function') {
                            const newPagination = updater({ pageIndex, pageSize })
                            setPageIndex(newPagination.pageIndex)
                            setPageSize(newPagination.pageSize)
                        } else {
                            setPageIndex(updater.pageIndex)
                            setPageSize(updater.pageSize)
                        }
                    }}
                />
            )}

            <TvShowDialog
                open={modalOpen}
                onOpenChange={setModalOpen}
                tvShow={editingTvShow}
                onSave={handleSave}
            />
        </div>
    )
}
