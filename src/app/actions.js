"use server";

import { writeFile, unlink } from "fs/promises";
import path from "path";
import { exec } from "child_process";
import { promisify } from "util";

const execAsync = promisify(exec);

export async function generateModel(formData) {
    const file = formData.get("file");

    if (!file) {
        return { error: "No file received." };
    }

    // Save temp file
    // Note: In Server Actions, we need to be careful with paths and cleanups
    const buffer = Buffer.from(await file.arrayBuffer());
    const tempFilename = `upload_${Date.now()}_${file.name.replace(/[^a-zA-Z0-9.]/g, "_")}`;
    const tempPath = path.join(process.cwd(), tempFilename);

    try {
        await writeFile(tempPath, buffer);
        console.log(`Saved temp file: ${tempPath}`);

        // Call Python Script
        // NOTE: Hardcoded to Python 3.14 to ensure compatibility
        const pythonExecutable = "C:\\Users\\Tima\\AppData\\Local\\Programs\\Python\\Python314\\python.exe";
        const pythonScript = path.join(process.cwd(), "ml_engine", "cli.py");
        // Escape paths for safety
        const command = `"${pythonExecutable}" "${pythonScript}" "${tempPath}"`;

        console.log(`Executing: ${command}`);

        const { stdout, stderr } = await execAsync(command);

        if (stderr) {
            console.error("Python Stderr:", stderr);
        }

        // Clean up temp file immediately after exec
        await unlink(tempPath);

        try {
            const result = JSON.parse(stdout);
            return result;
        } catch (parseError) {
            console.error("Parse Error:", stdout);
            return { error: "Failed to parse Python output", raw: stdout };
        }

    } catch (error) {
        // Ensure cleanup happens even on error
        try { await unlink(tempPath); } catch (e) { }

        console.error("Server Action Error:", error);
        return { error: "Failed to execute generation model", details: error.message };
    }
}
