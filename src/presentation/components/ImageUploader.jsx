"use client";

import { useState, useCallback } from "react";
import { Upload, X, Image as ImageIcon, Sparkles } from "lucide-react";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export default function ImageUploader({ onImageSelected }) {
    const [dragActive, setDragActive] = useState(false);
    const [preview, setPreview] = useState(null);
    const [isScanning, setIsScanning] = useState(false);

    const handleDrag = useCallback((e) => {
        e.preventDefault();
        e.stopPropagation();
        if (e.type === "dragenter" || e.type === "dragover") {
            setDragActive(true);
        } else if (e.type === "dragleave") {
            setDragActive(false);
        }
    }, []);

    const handleDrop = useCallback((e) => {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);
        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
            handleFile(e.dataTransfer.files[0]);
        }
    }, []);

    const handleChange = (e) => {
        e.preventDefault();
        if (e.target.files && e.target.files[0]) {
            handleFile(e.target.files[0]);
        }
    };

    const handleFile = (file) => {
        if (!file.type.startsWith("image/")) {
            alert("Please upload an image file");
            return;
        }

        const url = URL.createObjectURL(file);
        setPreview(url);
        onImageSelected(file);

        // Simulate a scanning effect for premium feel
        setIsScanning(true);
        setTimeout(() => setIsScanning(false), 1500);
    };

    const clearImage = (e) => {
        e.stopPropagation();
        setPreview(null);
        onImageSelected(null);
        setIsScanning(false);
    };

    return (
        <div className="w-full max-w-xl mx-auto group">
            <div
                className={twMerge(
                    clsx(
                        "relative flex flex-col items-center justify-center w-full aspect-[4/3] rounded-2xl cursor-pointer overflow-hidden transition-all duration-500 ease-out",
                        "backdrop-blur-xl bg-white/10 border border-white/20 shadow-2xl",
                        dragActive ? "scale-105 border-emerald-500/50 bg-emerald-500/10" : "hover:border-emerald-500/30 hover:bg-white/15"
                    )
                )}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
                onClick={() => document.getElementById("file-upload").click()}
            >
                {/* Ambient glow effect background */}
                <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/10 via-purple-500/10 to-emerald-500/10 opacity-50 group-hover:opacity-100 transition-opacity duration-700" />

                <input
                    id="file-upload"
                    type="file"
                    className="hidden"
                    accept="image/*"
                    onChange={handleChange}
                />

                {preview ? (
                    <div className="relative w-full h-full flex items-center justify-center p-6 z-10">
                        {/* Scanning Effect Overlay */}
                        {isScanning && (
                            <div className="absolute inset-0 z-20 pointer-events-none bg-emerald-500/5 animate-pulse">
                                <div className="w-full h-1 bg-gradient-to-r from-transparent via-emerald-400 to-transparent absolute top-0 animate-scan" style={{ animationDuration: '1.5s' }} />
                            </div>
                        )}

                        <div className="relative group/preview transition-transform duration-500 hover:scale-[1.02]">
                            <img
                                src={preview}
                                alt="Preview"
                                className="max-h-[300px] w-auto rounded-lg shadow-2xl object-contain ring-1 ring-white/10"
                            />
                            {/* Reflection effect */}
                            <div className="absolute -bottom-4 left-0 right-0 h-4 bg-gradient-to-b from-white/20 to-transparent rounded-b-lg opacity-50" />
                        </div>

                        <button
                            onClick={clearImage}
                            className="absolute top-4 right-4 p-2 bg-black/40 backdrop-blur-md text-white rounded-full hover:bg-red-500/80 transition-all duration-300 border border-white/10 shadow-lg hover:rotate-90"
                        >
                            <X size={18} />
                        </button>
                    </div>
                ) : (
                    <div className="flex flex-col items-center justify-center text-center p-8 z-10">
                        <div className={clsx(
                            "w-20 h-20 mb-6 rounded-2xl flex items-center justify-center transition-all duration-500",
                            "bg-gradient-to-tr from-emerald-500/20 to-cyan-500/20 shadow-inner border border-white/10",
                            dragActive ? "rotate-12 scale-110" : "group-hover:scale-110 group-hover:rotate-6"
                        )}>
                            <ImageIcon className={clsx("w-10 h-10 transition-colors duration-300", dragActive ? "text-emerald-400" : "text-white/70")} />
                        </div>

                        <h3 className="text-xl font-bold text-white mb-2 tracking-tight">
                            Drop image here
                        </h3>
                        <p className="text-sm text-white/50 mb-6 max-w-[200px] leading-relaxed">
                            Upload your sprite to generate a Voxel Model
                        </p>

                        <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 text-xs font-medium text-emerald-400">
                            <Sparkles size={12} />
                            <span>AI Powered Generation</span>
                        </div>
                    </div>
                )}
            </div>

            {/* Helper Text below */}
            {!preview && (
                <div className="flex justify-between mt-4 px-2 opacity-60 text-xs text-white/40 font-mono">
                    <span>Supported: PNG, JPG, WEBP</span>
                    <span>Max Size: 10MB</span>
                </div>
            )}
        </div>
    );
}

// Add this to your global.css for the scan animation if not present
// @keyframes scan { 0% { top: 0% } 100% { top: 100% } }
