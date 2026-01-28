"use client";

import { useState } from "react";
import ImageUploader from "@/presentation/components/ImageUploader";
import ModelPreview from "@/presentation/components/ModelPreview";
import { generateModel } from "./actions";

export default function Home() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const handleGenerate = async (file) => {
    if (!file) {
      setResult(null);
      return;
    }

    setLoading(true);
    setResult(null);

    try {
      const formData = new FormData();
      formData.append("file", file);

      // Call Server Action
      const data = await generateModel(formData);

      if (data.error) {
        throw new Error(data.details ? `${data.error}: ${data.details}` : data.error);
      }

      setResult(data);
    } catch (error) {
      console.error("Generation failed:", error);
      alert(`Failed to generate model: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="flex min-h-screen flex-col items-center justify-start p-8 md:p-24 bg-white dark:bg-black font-sans">
      <div className="z-10 max-w-5xl w-full items-center justify-between text-sm flex-col">
        <div className="text-center mb-12">
          <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-emerald-500 to-cyan-600">
            Hytale Model Generator
          </h1>
          <p className="mt-4 text-zinc-500 dark:text-zinc-400 text-lg">
            Transform 2D sprites into 3D voxel masterpieces using GANs
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-12 w-full">
          {/* Left Column: Input */}
          <div className="flex flex-col gap-6">
            <div className="p-6 border rounded-xl bg-white dark:bg-zinc-900/50 shadow-sm">
              <h2 className="text-xl font-bold mb-4 dark:text-white">1. Upload Sprite</h2>
              <ImageUploader onImageSelected={handleGenerate} />
            </div>

            <div className="p-4 bg-blue-50 dark:bg-blue-900/20 text-blue-800 dark:text-blue-300 rounded-lg text-sm">
              <strong>Tip:</strong> Use a clear PNG with a transparent background for best results.
            </div>
          </div>

          {/* Right Column: Output */}
          <div className="flex flex-col gap-6">
            <div className="p-6 border rounded-xl bg-white dark:bg-zinc-900/50 shadow-sm min-h-[400px]">
              <h2 className="text-xl font-bold mb-4 dark:text-white">2. 3D Result</h2>
              <ModelPreview isLoading={loading} data={result} />
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
