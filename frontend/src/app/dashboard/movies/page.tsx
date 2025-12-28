"use client"

import { useState } from "react"
import { DataTable } from "@/components/ui/data-table"
import { getMovieColumns, Movie } from "@/components/movies/columns"
import { Button } from "@/components/ui/button"
import { Plus } from "lucide-react"
import { MovieDialog } from "@/components/movies/movie-dialog"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { API_URL } from "@/lib/config"

export default function MoviesPage() {
    const queryClient = useQueryClient()
    const [modalOpen, setModalOpen] = useState(false)
    const [editingMovie, setEditingMovie] = useState<Movie | null>(null)
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 20 })

    const fetchMovies = async (pageIndex: number, pageSize: number) => {
        const token = localStorage.getItem('authToken')
        const res = await fetch(`${API_URL}/movies?page=${pageIndex}&size=${pageSize}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        })
        if (!res.ok) throw new Error('Network response was not ok')
        const json = await res.json()
        return json
    }

    const { data: qData, isLoading } = useQuery({
        queryKey: ['movies', pagination.pageIndex, pagination.pageSize],
        queryFn: () => fetchMovies(pagination.pageIndex, pagination.pageSize),
    })

    const movies = qData?.content || []
    const pageCount = qData?.totalPages || 0
    const totalItems = qData?.totalElements || 0

    const deleteMutation = useMutation({
        mutationFn: async (id: number) => {
            const token = localStorage.getItem('authToken')
            await fetch(`${API_URL}/movies/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            })
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['movies'] })
        }
    })

    const saveMutation = useMutation({
        mutationFn: async (movie: Partial<Movie>) => {
            const isEdit = !!movie.id
            const url = isEdit ? `${API_URL}/movies/${movie.id}` : `${API_URL}/movies`
            const method = isEdit ? 'PUT' : 'POST'

            const token = localStorage.getItem('authToken')
            const res = await fetch(url, {
                method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(movie)
            })
            if (!res.ok) throw new Error('Error saving movie')
            return res.json()
        },
        onSuccess: () => {
            setModalOpen(false)
            queryClient.invalidateQueries({ queryKey: ['movies'] })
        },
        onError: (error) => {
            console.error(error)
            alert("Error saving movie")
        }
    })

    const handleEdit = (movie: Movie) => {
        setEditingMovie(movie)
        setModalOpen(true)
    }

    const handleDelete = (id: number) => {
        if (confirm("Are you sure?")) {
            deleteMutation.mutate(id)
        }
    }

    const handleSave = (movie: Partial<Movie>) => {
        saveMutation.mutate(movie)
    }

    const handleAddNew = () => {
        setEditingMovie(null)
        setModalOpen(true)
    }

    const columns = getMovieColumns({ onEdit: handleEdit, onDelete: handleDelete })

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold">Movies</h1>
                    <p className="text-muted-foreground">Total Movies: {totalItems}</p>
                </div>
                <Button onClick={handleAddNew} className="bg-indigo-600 hover:bg-indigo-700">
                    <Plus className="mr-2 h-4 w-4" /> Add Movie
                </Button>
            </div>

            {isLoading ? (
                <div className="text-center text-gray-500">Loading...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={movies}
                    searchKey="name"
                    pageCount={pageCount}
                    pagination={pagination}
                    onPaginationChange={setPagination}
                />
            )}

            <MovieDialog
                open={modalOpen}
                onOpenChange={setModalOpen}
                movie={editingMovie}
                onSave={handleSave}
            />
        </div>
    )
}
