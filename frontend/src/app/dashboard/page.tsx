"use client"

import { useEffect, useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Film, Tv, Users, Activity } from "lucide-react"
import { API_URL } from "@/lib/config"
import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Tooltip as RechartsTooltip,
    Legend
} from "recharts"

interface DashboardStats {
    totalMovies: number
    totalTvShows: number
    totalUsers: number
    recentMovies: any[]
}

export default function DashboardPage() {
    const [stats, setStats] = useState<DashboardStats>({
        totalMovies: 0,
        totalTvShows: 0,
        totalUsers: 0,
        recentMovies: [],
    })
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const token = localStorage.getItem('authToken')
                const headers = { 'Authorization': `Bearer ${token}` }

                const [resMovies, resTv, resUsers, resRecent] = await Promise.all([
                    fetch(`${API_URL}/movies?size=0`, { headers }), // Just get metadata
                    fetch(`${API_URL}/tvshows?size=0`, { headers }),
                    fetch(`${API_URL}/users?size=0`, { headers }),
                    fetch(`${API_URL}/movies?page=0&size=5&sort=id,desc`, { headers })
                ])

                const jsonMovies = resMovies.ok ? await resMovies.json() : { totalElements: 0 }
                const jsonTv = resTv.ok ? await resTv.json() : { totalElements: 0 }
                const jsonUsers = resUsers.ok ? await resUsers.json() : { totalElements: 0 }
                const jsonRecent = resRecent.ok ? await resRecent.json() : { content: [] }

                setStats({
                    totalMovies: jsonMovies.totalElements || 0,
                    totalTvShows: jsonTv.totalElements || 0,
                    totalUsers: jsonUsers.totalElements || 0,
                    recentMovies: jsonRecent.content || [],
                })
            } catch (error) {
                console.error("Failed to fetch dashboard stats", error)
            } finally {
                setLoading(false)
            }
        }

        fetchStats()
    }, [])

    const dataDistribution = [
        { name: 'Movies', value: stats.totalMovies },
        { name: 'TV Shows', value: stats.totalTvShows },
    ]

    const COLORS = ['#6366f1', '#ec4899'] // Indigo, Pink

    return (
        <div className="space-y-6">
            <h1 className="text-3xl font-bold">Overview</h1>

            {/* Stats Cards */}
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                <Card className="bg-gray-900 border-gray-800 text-white">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Total Movies</CardTitle>
                        <Film className="h-4 w-4 text-indigo-400" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{loading ? "..." : stats.totalMovies}</div>
                        <p className="text-xs text-muted-foreground">Available in library</p>
                    </CardContent>
                </Card>

                <Card className="bg-gray-900 border-gray-800 text-white">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Total Series</CardTitle>
                        <Tv className="h-4 w-4 text-pink-400" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{loading ? "..." : stats.totalTvShows}</div>
                        <p className="text-xs text-muted-foreground">Available seasons</p>
                    </CardContent>
                </Card>

                <Card className="bg-gray-900 border-gray-800 text-white">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Active Users</CardTitle>
                        <Users className="h-4 w-4 text-blue-400" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{loading ? "..." : stats.totalUsers}</div>
                        <p className="text-xs text-muted-foreground">Registered users</p>
                    </CardContent>
                </Card>

                <Card className="bg-gray-900 border-gray-800 text-white">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">System Status</CardTitle>
                        <Activity className="h-4 w-4 text-green-500" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">Healthy</div>
                        <p className="text-xs text-muted-foreground">All systems operational</p>
                    </CardContent>
                </Card>
            </div>

            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
                {/* Chart Section */}
                <Card className="col-span-4 bg-gray-900 border-gray-800 text-white">
                    <CardHeader>
                        <CardTitle>Content Distribution</CardTitle>
                    </CardHeader>
                    <CardContent className="pl-2">
                        <div className="h-[300px] w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Pie
                                        data={dataDistribution}
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={60}
                                        outerRadius={80}
                                        fill="#8884d8"
                                        paddingAngle={5}
                                        dataKey="value"
                                    >
                                        {dataDistribution.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                        ))}
                                    </Pie>
                                    <RechartsTooltip
                                        contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                                        itemStyle={{ color: '#fff' }}
                                    />
                                    <Legend verticalAlign="bottom" height={36} />
                                </PieChart>
                            </ResponsiveContainer>
                        </div>
                    </CardContent>
                </Card>

                {/* Recent Activity Section */}
                <Card className="col-span-3 bg-gray-900 border-gray-800 text-white">
                    <CardHeader>
                        <CardTitle>Recent Movies</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-8">
                            {loading ? (
                                <div className="text-sm text-gray-400">Loading...</div>
                            ) : stats.recentMovies.length === 0 ? (
                                <div className="text-sm text-gray-400">No movies found.</div>
                            ) : (
                                stats.recentMovies.map((movie, i) => (
                                    <div key={movie.id || i} className="flex items-center">
                                        <div className="h-9 w-9 rounded-full bg-indigo-900 flex items-center justify-center border border-indigo-700">
                                            <Film className="h-4 w-4 text-indigo-400" />
                                        </div>
                                        <div className="ml-4 space-y-1">
                                            <p className="text-sm font-medium leading-none text-white">{movie.name}</p>
                                            <p className="text-xs text-gray-400">
                                                Tmdb ID: {movie.tmdbId}
                                            </p>
                                        </div>
                                        <div className="ml-auto font-medium text-sm text-gray-400">
                                            {movie.userScore ? `${movie.userScore * 10}%` : 'N/A'}
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    )
}
