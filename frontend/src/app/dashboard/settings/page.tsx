"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardHeader, CardContent, CardTitle, CardDescription } from "@/components/ui/card"
import { Loader2, RefreshCw, UploadCloud, Database } from "lucide-react"
import { API_URL } from "@/lib/config"

export default function SettingsPage() {
    const [loading, setLoading] = useState<string | null>(null)

    const triggerSync = async (endpoint: string, generator: string, label: string) => {
        setLoading(label)
        try {
            const token = localStorage.getItem('authToken')
            const res = await fetch(`${API_URL}/sync/${endpoint}?generator=${generator}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            })

            if (res.ok) {
                alert(`${label} triggered successfully in the background!`)
            } else {
                alert(`Failed to trigger ${label}`)
            }
        } catch (e) {
            console.error(e)
            alert(`Error triggering ${label}`)
        } finally {
            setLoading(null)
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-3xl font-bold">Settings</h1>
            </div>

            <Card className="bg-gray-900 border-gray-800 text-white">
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <RefreshCw className="h-5 w-5 text-indigo-500" />
                        Content Synchronization
                    </CardTitle>
                    <CardDescription className="text-gray-400">
                        Manage Yandex Disk synchronization and library updates.
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">

                    <div className="flex items-center justify-between p-4 bg-gray-950/50 rounded-lg border border-gray-800">
                        <div className="space-y-1">
                            <p className="font-medium flex items-center gap-2">
                                <UploadCloud className="h-4 w-4 text-green-400" />
                                Get New URLs (Updates)
                            </p>
                            <p className="text-sm text-gray-400">
                                Scans the "Updates" generator for new video files/qualities and merges them into existing library items.
                            </p>
                        </div>
                        <Button
                            onClick={() => triggerSync('update-urls', 'updates', 'Update URLs')}
                            disabled={!!loading}
                        >
                            {loading === 'Update URLs' && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            Run Update Scan
                        </Button>
                    </div>

                    <div className="flex items-center justify-between p-4 bg-gray-950/50 rounded-lg border border-gray-800">
                        <div className="space-y-1">
                            <p className="font-medium flex items-center gap-2">
                                <Database className="h-4 w-4 text-blue-400" />
                                Full Sync (Legacy)
                            </p>
                            <p className="text-sm text-gray-400">
                                Performs a full re-scan of the Legacy library. Use heavily to initialize or repair data.
                            </p>
                        </div>
                        <Button
                            variant="secondary"
                            onClick={() => triggerSync('first-data', 'legacy', 'Full Sync (Legacy)')}
                            disabled={!!loading}
                        >
                            {loading === 'Full Sync (Legacy)' && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            Run Full Sync
                        </Button>
                    </div>

                    <div className="flex items-center justify-between p-4 bg-gray-950/50 rounded-lg border border-gray-800">
                        <div className="space-y-1">
                            <p className="font-medium flex items-center gap-2">
                                <Database className="h-4 w-4 text-yellow-400" />
                                Full Sync (Updates)
                            </p>
                            <p className="text-sm text-gray-400">
                                Performs a full re-scan of the Updates library.
                            </p>
                        </div>
                        <Button
                            variant="secondary"
                            onClick={() => triggerSync('first-data', 'updates', 'Full Sync (Updates)')}
                            disabled={!!loading}
                        >
                            {loading === 'Full Sync (Updates)' && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            Run Full Sync
                        </Button>
                    </div>

                </CardContent>
            </Card>
        </div>
    )
}
