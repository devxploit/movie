"use client"

import { useState, useEffect } from "react"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Movie } from "./columns" // Import Movie type

interface MovieDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    movie: Movie | null
    onSave: (movie: Partial<Movie>) => void
}

export function MovieDialog({ open, onOpenChange, movie, onSave }: MovieDialogProps) {
    const [name, setName] = useState("")
    const [tmdbId, setTmdbId] = useState("")

    // Reset form when movie changes
    useEffect(() => {
        if (movie) {
            setName(movie.name)
            setTmdbId(movie.tmdbId.toString())
        } else {
            setName("")
            setTmdbId("")
        }
    }, [movie, open])

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault()
        onSave({
            id: movie?.id,
            name,
            tmdbId: parseInt(tmdbId) || 0,
            // Other fields would go here
        })
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="bg-gray-900 border-gray-800 text-white sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>{movie ? "Edit Movie" : "Add New Movie"}</DialogTitle>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="grid gap-4 py-4">
                    <div className="grid grid-cols-4 items-center gap-4">
                        <Label htmlFor="title" className="text-right">
                            Title
                        </Label>
                        <Input
                            id="title"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className="col-span-3 bg-gray-800 border-gray-700 text-white"
                            required
                        />
                    </div>
                    <div className="grid grid-cols-4 items-center gap-4">
                        <Label htmlFor="tmdbId" className="text-right">
                            TMDB ID
                        </Label>
                        <Input
                            id="tmdbId"
                            type="number"
                            value={tmdbId}
                            onChange={(e) => setTmdbId(e.target.value)}
                            className="col-span-3 bg-gray-800 border-gray-700 text-white"
                        />
                    </div>
                    <DialogFooter>
                        <Button type="submit" className="bg-indigo-600 hover:bg-indigo-700">Save changes</Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    )
}
