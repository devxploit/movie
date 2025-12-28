"use client"

import * as React from "react"
import {
    ColumnDef,
    flexRender,
    getCoreRowModel,
    useReactTable,
    getPaginationRowModel,
    SortingState,
    getSortedRowModel,
    ColumnFiltersState,
    getFilteredRowModel,
} from "@tanstack/react-table"

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

interface DataTableProps<TData, TValue> {
    columns: ColumnDef<TData, TValue>[]
    data: TData[]
    searchKey?: string
    pageCount?: number
    pagination?: {
        pageIndex: number
        pageSize: number
    }
    onPaginationChange?: (pagination: any) => void
    searchValue?: string
    onSearchChange?: (value: string) => void
}

export function DataTable<TData, TValue>({
    columns,
    data,
    searchKey,
    pageCount,
    pagination,
    onPaginationChange,
    searchValue,
    onSearchChange,
}: DataTableProps<TData, TValue>) {
    const [sorting, setSorting] = React.useState<SortingState>([])
    const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>(
        []
    )

    const table = useReactTable({
        data,
        columns,
        getCoreRowModel: getCoreRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
        onSortingChange: setSorting,
        getSortedRowModel: getSortedRowModel(),
        onColumnFiltersChange: setColumnFilters,
        getFilteredRowModel: getFilteredRowModel(),
        manualPagination: !!pageCount,
        pageCount: pageCount,
        onPaginationChange: onPaginationChange,
        state: {
            sorting,
            columnFilters,
            pagination: pagination,
        },
    })

    return (
        <div>
            {searchKey && (
                <div className="flex items-center py-4">
                    <Input
                        placeholder="Search..."
                        value={searchValue !== undefined ? searchValue : ((table.getColumn(searchKey)?.getFilterValue() as string) ?? "")}
                        onChange={(event) => {
                            if (onSearchChange) {
                                onSearchChange(event.target.value)
                            } else {
                                table.getColumn(searchKey)?.setFilterValue(event.target.value)
                            }
                        }}
                        className="max-w-sm bg-gray-900 border-gray-800 text-white"
                    />
                </div>
            )}
            <div className="rounded-md border border-gray-800">
                <Table>
                    <TableHeader>
                        {table.getHeaderGroups().map((headerGroup) => (
                            <TableRow key={headerGroup.id} className="border-gray-800 hover:bg-gray-900/50">
                                {headerGroup.headers.map((header) => {
                                    return (
                                        <TableHead key={header.id} className="text-gray-400">
                                            {header.isPlaceholder
                                                ? null
                                                : flexRender(
                                                    header.column.columnDef.header,
                                                    header.getContext()
                                                )}
                                        </TableHead>
                                    )
                                })}
                            </TableRow>
                        ))}
                    </TableHeader>
                    <TableBody>
                        {table.getRowModel().rows?.length ? (
                            table.getRowModel().rows.map((row) => (
                                <TableRow
                                    key={row.id}
                                    data-state={row.getIsSelected() && "selected"}
                                    className="border-gray-800 hover:bg-gray-900/50 text-white"
                                >
                                    {row.getVisibleCells().map((cell) => (
                                        <TableCell key={cell.id}>
                                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                        </TableCell>
                                    ))}
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell
                                    colSpan={columns.length}
                                    className="h-24 text-center text-gray-400"
                                >
                                    No results.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </div>
            <div className="flex items-center justify-between py-4">
                <div className="flex-1 text-sm text-gray-400">
                    Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}
                </div>
                <div className="flex items-center space-x-2">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => table.setPageIndex(0)}
                        disabled={!table.getCanPreviousPage()}
                        className="bg-gray-900 border-gray-800 text-white hover:bg-gray-800 hidden md:flex"
                    >
                        First
                    </Button>
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => table.previousPage()}
                        disabled={!table.getCanPreviousPage()}
                        className="bg-gray-900 border-gray-800 text-white hover:bg-gray-800"
                    >
                        Previous
                    </Button>

                    <div className="flex items-center space-x-1">
                        {(() => {
                            const pageIndex = table.getState().pagination.pageIndex
                            const pageCount = table.getPageCount()
                            const buttons = []

                            // Simple algorithm for pagination
                            // Always show first, last, current, and neighbors

                            const showEllipsisStart = pageIndex > 3
                            const showEllipsisEnd = pageIndex < pageCount - 4

                            if (pageCount <= 7) {
                                for (let i = 0; i < pageCount; i++) {
                                    buttons.push(i)
                                }
                            } else {
                                buttons.push(0)
                                if (showEllipsisStart) buttons.push(-1)

                                const start = Math.max(1, pageIndex - 1)
                                const end = Math.min(pageCount - 2, pageIndex + 1)

                                // Adjust if near start or end to show meaningful block
                                if (pageIndex < 4) {
                                    for (let i = 1; i < 5; i++) buttons.push(i)
                                } else if (pageIndex > pageCount - 5) {
                                    for (let i = pageCount - 5; i < pageCount - 1; i++) buttons.push(i)
                                } else {
                                    for (let i = start; i <= end; i++) buttons.push(i)
                                }

                                if (showEllipsisEnd) buttons.push(-2)
                                buttons.push(pageCount - 1)
                            }

                            // Deduplicate just in case logic overlaps
                            const uniqueButtons = Array.from(new Set(buttons)).sort((a, b) => {
                                if (a < 0 || b < 0) return 0 // keep relative order for ellipsis logic slightly broken? No, logic above is sequential.
                                return a - b
                            })

                            // Actually duplicate logic above handles order correctly so Set might reorder negative params. 
                            // Let's stick to the generated 'buttons' array which is ordered.

                            return buttons.map((page, i) => {
                                if (page < 0) {
                                    return <span key={`ellipsis-${i}`} className="px-2 text-gray-400">...</span>
                                }
                                return (
                                    <Button
                                        key={page}
                                        variant={pageIndex === page ? "default" : "outline"}
                                        size="sm"
                                        onClick={() => table.setPageIndex(page)}
                                        className={`${pageIndex === page
                                            ? "bg-indigo-600 hover:bg-indigo-700 text-white border-indigo-600"
                                            : "bg-gray-900 border-gray-800 text-white hover:bg-gray-800"} w-9`}
                                    >
                                        {page + 1}
                                    </Button>
                                )
                            })
                        })()}
                    </div>

                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => table.nextPage()}
                        disabled={!table.getCanNextPage()}
                        className="bg-gray-900 border-gray-800 text-white hover:bg-gray-800"
                    >
                        Next
                    </Button>
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => table.setPageIndex(table.getPageCount() - 1)}
                        disabled={!table.getCanNextPage()}
                        className="bg-gray-900 border-gray-800 text-white hover:bg-gray-800 hidden md:flex"
                    >
                        Last
                    </Button>
                </div>
            </div>
        </div>
    )
}
