export function Header() {
    return (
        <header className="flex h-16 items-center justify-between border-b border-gray-800 bg-gray-900 px-6">
            <div className="flex items-center">
                {/* Breadcrumb or Title placeholder */}
                <h2 className="text-lg font-medium text-white">Dashboard</h2>
            </div>
            <div className="flex items-center space-x-4">
                {/* User profile or notifications could go here */}
                <div className="h-8 w-8 rounded-full bg-indigo-500 flex items-center justify-center text-white font-bold">
                    A
                </div>
            </div>
        </header>
    )
}
