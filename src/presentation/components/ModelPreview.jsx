
"use client";

import { Box } from "lucide-react";

export default function ModelPreview({ isLoading, data }) {
    if (isLoading) {
        return (
            <div className="w-full h-64 border rounded-lg flex flex-col items-center justify-center bg-gray-50 dark:bg-zinc-900 animate-pulse">
                <Box className="w-12 h-12 text-gray-400 animate-bounce" />
                <p className="mt-4 text-sm font-medium text-gray-500">Generating Voxel Model...</p>
                <p className="text-xs text-gray-400">This involves strict 2D-to-3D matrix calculation</p>
            </div>
        );
    }

    if (!data) {
        return (
            <div className="w-full h-64 border rounded-lg flex flex-col items-center justify-center bg-gray-50 dark:bg-zinc-900 border-dashed border-gray-300 dark:border-zinc-700">
                <Box className="w-12 h-12 text-gray-300 dark:text-zinc-600" />
                <p className="mt-2 text-sm text-gray-400 dark:text-zinc-500">
                    Generated model will appear here
                </p>
            </div>
        );
    }

    return (
        <div className="w-full border rounded-lg bg-white dark:bg-zinc-950 shadow-sm overflow-hidden">
            <div className="p-4 border-b dark:border-zinc-800 flex justify-between items-center bg-gray-50 dark:bg-zinc-900/50">
                <h3 className="font-semibold text-sm flex items-center gap-2">
                    <Box className="w-4 h-4 text-emerald-500" />
                    Model Result
                </h3>
                <span className="text-xs px-2 py-1 rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400">
                    Success
                </span>
            </div>

            <div className="p-4">
                <div className="bg-gray-900 text-gray-100 p-4 rounded text-xs font-mono overflow-auto max-h-64">
                    {/* Placeholder for 3D View - showing raw data statistics for now */}
                    <pre>{JSON.stringify(data, null, 2)}</pre>
                </div>
                <div className="mt-4 text-right">
                    <button className="text-xs text-blue-500 hover:underline">Download .json</button>
                </div>
            </div>
        </div>
    );
}
