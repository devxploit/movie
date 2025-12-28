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
import { Select } from "@/components/ui/select-native"
import { User } from "./columns"

interface UserDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    user: User | null
    onSave: (user: Partial<User> & { password?: string }) => void
}

export function UserDialog({ open, onOpenChange, user, onSave }: UserDialogProps) {
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [role, setRole] = useState("USER")
    const [status, setStatus] = useState("INACTIVE")

    useEffect(() => {
        if (user) {
            setEmail(user.email)
            setRole(user.role)
            setStatus(user.subscriptionStatus)
            setPassword("") // Reset password on edit
        } else {
            setEmail("")
            setPassword("")
            setRole("USER")
            setStatus("INACTIVE")
        }
    }, [user, open])

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault()
        onSave({
            id: user?.id,
            email,
            role: role as any,
            subscriptionStatus: status as any,
            password: password || undefined
        })
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="bg-gray-900 border-gray-800 text-white sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>{user ? "Edit User" : "Add New User"}</DialogTitle>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="grid gap-4 py-4">
                    <div className="grid grid-cols-4 items-center gap-4">
                        <Label htmlFor="email" className="text-right">
                            Email
                        </Label>
                        <Input
                            id="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="col-span-3 bg-gray-800 border-gray-700 text-white"
                            required
                        />
                    </div>
                    <div className="grid grid-cols-4 items-center gap-4">
                        <Label htmlFor="password" className="text-right">
                            Password
                        </Label>
                        <Input
                            id="password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="col-span-3 bg-gray-800 border-gray-700 text-white"
                            placeholder={user ? "Leave blank to keep current" : "Required"}
                            required={!user}
                        />
                    </div>
                    <div className="grid grid-cols-4 items-center gap-4">
                        <Label htmlFor="role" className="text-right">
                            Role
                        </Label>
                        <Select
                            id="role"
                            value={role}
                            onChange={(e) => setRole(e.target.value)}
                            className="col-span-3 bg-gray-800 border-gray-700 text-white"
                        >
                            <option value="USER">User</option>
                            <option value="ADMIN">Admin</option>
                        </Select>
                    </div>
                    <div className="grid grid-cols-4 items-center gap-4">
                        <Label htmlFor="status" className="text-right">
                            Status
                        </Label>
                        <Select
                            id="status"
                            value={status}
                            onChange={(e) => setStatus(e.target.value)}
                            className="col-span-3 bg-gray-800 border-gray-700 text-white"
                        >
                            <option value="INACTIVE">Inactive</option>
                            <option value="ACTIVE">Active</option>
                        </Select>
                    </div>
                    <DialogFooter>
                        <Button type="submit" className="bg-indigo-600 hover:bg-indigo-700">Save changes</Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    )
}
