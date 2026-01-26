"use client";

import { Box } from "lucide-react";

const MOCK_GALLERY = [
    { id: 1, name: "Voxel Knight", date: "2024-03-10", image: "https://placehold.co/400x400/101827/10b981?text=Knight" },
    { id: 2, name: "Fantasy Castle", date: "2024-03-09", image: "https://placehold.co/400x400/101827/10b981?text=Castle" },
    { id: 3, name: "Potion Flask", date: "2024-03-08", image: "https://placehold.co/400x400/101827/10b981?text=Potion" },
    { id: 4, name: "Magic Sword", date: "2024-03-08", image: "https://placehold.co/400x400/101827/10b981?text=Sword" },
    { id: 5, name: "Ancient Tree", date: "2024-03-07", image: "https://placehold.co/400x400/101827/10b981?text=Tree" },
    { id: 6, name: "Crystal Ores", date: "2024-03-06", image: "https://placehold.co/400x400/101827/10b981?text=Crystal" },
];

export default function GalleryPage() {
    return (
        <div className="min-h-screen bg-zinc-950 text-white p-8">
            <div className="max-w-6xl mx-auto space-y-8">
                <header className="flex items-center justify-between pb-6 border-b border-white/10">
                    <h1 className="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-emerald-400 to-cyan-400">
                        Generated Models
                    </h1>
                    <span className="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-xs text-zinc-400">
                        {MOCK_GALLERY.length} Items
                    </span>
                </header>

                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                    {MOCK_GALLERY.map((item) => (
                        <div
                            key={item.id}
                            className="group relative aspect-square rounded-xl overflow-hidden bg-white/5 border border-white/10 hover:border-emerald-500/50 transition-all duration-300 hover:shadow-2xl hover:shadow-emerald-500/20"
                        >
                            <img
                                src={item.image}
                                alt={item.name}
                                className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                            />

                            <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col justify-end p-4">
                                <h3 className="font-semibold text-lg">{item.name}</h3>
                                <div className="flex justify-between items-center mt-2">
                                    <span className="text-xs text-zinc-400">{item.date}</span>
                                    <button className="p-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-400 transition-colors">
                                        <Box size={16} />
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}

                    {/* New Generation Placeholder */}
                    <a href="/" className="group flex flex-col items-center justify-center aspect-square rounded-xl border-2 border-dashed border-white/10 hover:border-emerald-500/50 hover:bg-white/5 transition-all text-zinc-500 hover:text-emerald-400 cursor-pointer">
                        <div className="w-12 h-12 rounded-full bg-white/5 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                            <span className="text-2xl">+</span>
                        </div>
                        <span className="font-medium text-sm">Generate New</span>
                    </a>
                </div>
            </div>
        </div>
    );
}
