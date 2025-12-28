"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
    Folder,
    File,
    ChevronRight,
    Home,
    ArrowUp,
    Download,
    Loader2
} from "lucide-react"

type ExplorerItem = {
    name: string
    path: string
    type: "dir" | "file"
    size: number
}

export default function ExplorerPage() {
    const [items, setItems] = useState<ExplorerItem[]>([])
    const [currentPath, setCurrentPath] = useState("/")
    const [loading, setLoading] = useState(false)
    const [generator, setGenerator] = useState("legacy") // 'legacy' or 'updates'

    const fetchItems = async (path: string) => {
        setLoading(true)
        try {
            const token = localStorage.getItem('authToken')
            const encodedPath = encodeURIComponent(path)
            const res = await fetch(`http://localhost:8080/api/dashboard/explorer?path=${encodedPath}&generator=${generator}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            })
            if (res.ok) {
                const data = await res.json()
                setItems(data)
                setCurrentPath(path)
            }
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchItems("/")
    }, [generator])

    const handleNavigate = (item: ExplorerItem) => {
        if (item.type === "dir") {
            fetchItems(item.path)
        }
    }

    const handleUp = () => {
        if (currentPath === "/") return
        // Simple parent path calculation
        const parentPath = currentPath.substring(0, currentPath.lastIndexOf("/")) || "/"
        fetchItems(parentPath)
    }

    const [importDialogOpen, setImportDialogOpen] = useState(false)
    const [selectedImportItem, setSelectedImportItem] = useState<ExplorerItem | null>(null)
    const [importType, setImportType] = useState<"movie" | "tvshow">("movie")

    const handleImportClick = (item: ExplorerItem) => {
        setSelectedImportItem(item)
        // Try to guess type based on path or name
        if (item.path.toLowerCase().includes("serie") || currentPath.toLowerCase().includes("serie")) {
            setImportType("tvshow")
        } else {
            setImportType("movie")
        }
        setImportDialogOpen(true)
    }

    const confirmImport = async () => {
        if (!selectedImportItem) return

        try {
            const token = localStorage.getItem('authToken')
            const res = await fetch(`http://localhost:8080/api/dashboard/sync/import?path=${encodeURIComponent(selectedImportItem.path)}&type=${importType}&generator=${generator}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            })
            if (res.ok) {
                alert("Import triggered successfully!")
            } else {
                alert("Failed to trigger import")
            }
        } catch (e) {
            console.error(e)
            alert("Error triggering import")
        } finally {
            setImportDialogOpen(false)
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-3xl font-bold">File Explorer</h1>
                <div className="flex gap-2">
                    <Button
                        variant={generator === 'legacy' ? 'default' : 'outline'}
                        onClick={() => setGenerator('legacy')}
                    >
                        Legacy
                    </Button>
                    <Button
                        variant={generator === 'updates' ? 'default' : 'outline'}
                        onClick={() => setGenerator('updates')}
                    >
                        Updates
                    </Button>
                </div>
            </div>

            <Card className="bg-gray-900 border-gray-800 text-white">
                <CardHeader className="flex flex-row items-center space-y-0 gap-2 border-b border-gray-800 pb-4">
                    <Button variant="ghost" size="icon" onClick={() => fetchItems("/")} disabled={loading}>
                        <Home className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={handleUp} disabled={currentPath === "/" || loading}>
                        <ChevronRight className="h-4 w-4 rotate-180" />
                    </Button>
                    <div className="flex-1 px-2 font-mono text-sm bg-gray-950/50 py-1.5 rounded text-gray-400">
                        {currentPath}
                    </div>
                </CardHeader>
                <CardContent className="p-0">
                    {loading ? (
                        <div className="flex justify-center p-8">
                            <Loader2 className="h-8 w-8 animate-spin text-indigo-500" />
                        </div>
                    ) : (
                        <div className="divide-y divide-gray-800">
                            {items.length === 0 && (
                                <div className="p-8 text-center text-gray-500">Empty directory</div>
                            )}
                            {items.map((item) => (
                                <div
                                    key={item.path}
                                    className="flex items-center justify-between p-3 hover:bg-gray-800/50 transition-colors group"
                                >
                                    <div
                                        className="flex items-center gap-3 cursor-pointer flex-1"
                                        onClick={() => handleNavigate(item)}
                                    >
                                        {item.type === "dir" ? (
                                            <Folder className="h-5 w-5 text-yellow-500" />
                                        ) : (
                                            <File className="h-5 w-5 text-blue-400" />
                                        )}
                                        <span className="text-sm font-medium">{item.name}</span>
                                    </div>
                                    <div className="flex items-center gap-4">
                                        <div className="text-xs text-gray-500 font-mono">
                                            {item.type === "file" && (item.size / 1024 / 1024).toFixed(2) + " MB"}
                                        </div>
                                        {item.type === 'dir' && (
                                            <Button
                                                variant="secondary"
                                                size="sm"
                                                className="opacity-0 group-hover:opacity-100 transition-opacity"
                                                onClick={(e) => {
                                                    e.stopPropagation()
                                                    handleImportClick(item)
                                                }}
                                            >
                                                Import
                                                <Download className="ml-2 h-3 w-3" />
                                            </Button>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* Import Dialog */}
            {importDialogOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80">
                    <div className="bg-gray-900 border border-gray-800 p-6 rounded-lg w-[400px] space-y-4">
                        <h2 className="text-xl font-bold text-white">Import Content</h2>
                        <p className="text-gray-400">
                            Import <strong>{selectedImportItem?.name}</strong>?
                        </p>
                        <div className="space-y-2">
                            <label className="text-sm font-medium text-gray-300">Content Type</label>
                            <select
                                className="w-full bg-gray-950 border border-gray-800 rounded p-2 text-white"
                                value={importType}
                                onChange={(e) => setImportType(e.target.value as "movie" | "tvshow")}
                            >
                                <option value="movie">Movie</option>
                                <option value="tvshow">TV Show</option>
                            </select>
                        </div>
                        <div className="flex justify-end gap-2 pt-2">
                            <Button variant="ghost" onClick={() => setImportDialogOpen(false)}>Cancel</Button>
                            <Button onClick={confirmImport}>Confirm Import</Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}

