
"use client";

import { useState, useCallback } from "react";
import { Upload, X, FileImage } from "lucide-react";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export default function ImageUploader({ onImageSelected }) {
    const [dragActive, setDragActive] = useState(false);
    const [preview, setPreview] = useState(null);

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

        // Create preview URL
        const url = URL.createObjectURL(file);
        setPreview(url);
        onImageSelected(file);
    };

    const clearImage = (e) => {
        e.stopPropagation(); // Prevent triggering the file input
        setPreview(null);
        onImageSelected(null);
    };

    return (
        <div className="w-full max-w-md mx-auto">
            <div
                className={twMerge(
                    clsx(
                        "relative flex flex-col items-center justify-center w-full h-64 border-2 border-dashed rounded-lg cursor-pointer transition-colors duration-200 ease-in-out",
                        dragActive
                            ? "border-emerald-500 bg-emerald-50 dark:bg-emerald-900/20"
                            : "border-gray-300 dark:border-zinc-700 hover:bg-gray-50 dark:hover:bg-zinc-800",
                        preview ? "border-transparent" : ""
                    )
                )}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
                onClick={() => document.getElementById("file-upload").click()}
            >
                <input
                    id="file-upload"
                    type="file"
                    className="hidden"
                    accept="image/*"
                    onChange={handleChange}
                />

                {preview ? (
                    <div className="relative w-full h-full flex items-center justify-center p-2">
                        {/* Image Preview */}
                        <img
                            src={preview}
                            alt="Preview"
                            className="max-h-full max-w-full rounded shadow-md object-contain"
                        />
                        {/* Clear Button */}
                        <button
                            onClick={clearImage}
                            className="absolute top-2 right-2 p-1 bg-red-500 text-white rounded-full hover:bg-red-600 transition shadow-lg"
                        >
                            <X size={16} />
                        </button>
                    </div>
                ) : (
                    <div className="flex flex-col items-center justify-center pt-5 pb-6 text-center px-4">
                        <Upload className={clsx("w-10 h-10 mb-3", dragActive ? "text-emerald-500" : "text-gray-400")} />
                        <p className="mb-2 text-sm text-gray-500 dark:text-gray-400">
                            <span className="font-semibold">Click to upload</span> or drag and drop
                        </p>
                        <p className="text-xs text-gray-400 dark:text-gray-500">
                            PNG, JPG (Sprite or Render)
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
}
