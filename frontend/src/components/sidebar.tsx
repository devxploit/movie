"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import {
    LayoutDashboard,
    Film,
    Tv,
    Users,
    FolderOpen,
    Settings,
    LogOut,
    Clapperboard
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { useRouter } from "next/navigation"

const sidebarItems = [
    { icon: LayoutDashboard, label: "Overview", href: "/dashboard" },
    { icon: Film, label: "Movies", href: "/dashboard/movies" },
    { icon: Tv, label: "TV Shows", href: "/dashboard/tv-shows" },
    { icon: Users, label: "Users", href: "/dashboard/users" },
    { icon: FolderOpen, label: "Explorer", href: "/dashboard/explorer" },
    { icon: Settings, label: "Settings", href: "/dashboard/settings" },
]

export function Sidebar() {
    const pathname = usePathname()
    const router = useRouter()

    const handleLogout = () => {
        localStorage.removeItem("authToken")
        localStorage.removeItem("userRole")
        router.push("/login")
    }

    return (
        <div className="flex h-full w-64 flex-col bg-gray-900 text-white border-r border-gray-800">
            <div className="flex h-16 items-center border-b border-gray-800 px-6">
                <Clapperboard className="mr-2 h-6 w-6 text-indigo-500" />
                <span className="text-lg font-bold">MovieSP Admin</span>
            </div>
            <nav className="flex-1 space-y-1 px-3 py-4">
                {sidebarItems.map((item) => (
                    <Link
                        key={item.href}
                        href={item.href}
                        className={cn(
                            "flex items-center rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-gray-800 hover:text-white",
                            pathname === item.href
                                ? "bg-gray-800 text-white"
                                : "text-gray-400"
                        )}
                    >
                        <item.icon className="mr-3 h-5 w-5 flex-shrink-0" />
                        {item.label}
                    </Link>
                ))}
            </nav>
            <div className="border-t border-gray-800 p-4">
                <Button
                    variant="ghost"
                    className="w-full justify-start text-gray-400 hover:bg-gray-800 hover:text-white"
                    onClick={handleLogout}
                >
                    <LogOut className="mr-3 h-5 w-5" />
                    Logout
                </Button>
            </div>
        </div>
    )
}
