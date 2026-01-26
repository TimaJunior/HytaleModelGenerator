
import { NextResponse } from "next/server";
import { writeFile, unlink } from "fs/promises";
import path from "path";
import { exec } from "child_process";
import { promisify } from "util";

const execAsync = promisify(exec);

export async function POST(request) {
    try {
        const formData = await request.formData();
        const file = formData.get("file");

        if (!file) {
            return NextResponse.json({ error: "No file received." }, { status: 400 });
        }

        const buffer = Buffer.from(await file.arrayBuffer());

        // Save temp file (simplified for windows/dev)
        const tempFilename = `upload_${Date.now()}_${file.name}`;
        const tempPath = path.join(process.cwd(), tempFilename);

        await writeFile(tempPath, buffer);
        console.log(`Saved temp file: ${tempPath}`);

        // Call Python Script
        // NOTE: Hardcoded to Python 3.14 to ensure compatibility
        const pythonExecutable = "C:\\Users\\Tima\\AppData\\Local\\Programs\\Python\\Python314\\python.exe";
        const pythonScript = path.join(process.cwd(), "ml_engine", "cli.py");
        // Escape paths for safety
        const command = `"${pythonExecutable}" "${pythonScript}" "${tempPath}"`;

        console.log(`Executing: ${command}`);

        try {
            const { stdout, stderr } = await execAsync(command);

            // Clean up temp file
            await unlink(tempPath);

            if (stderr) {
                console.error("Python Stderr:", stderr);
                // Non-fatal warnings might appear in stderr (like pytorch startup info)
            }

            console.log("Python Stdout:", stdout);

            try {
                const result = JSON.parse(stdout);
                if (result.error) {
                    return NextResponse.json({ error: result.error }, { status: 500 });
                }
                return NextResponse.json(result);
            } catch (parseError) {
                return NextResponse.json({ error: "Failed to parse Python output", raw: stdout }, { status: 500 });
            }

        } catch (execError) {
            // Clean up temp file in case of error
            try { await unlink(tempPath); } catch (e) { }

            console.error("Exec Error:", execError);
            return NextResponse.json({ error: "Failed to execute generation model", details: execError.message }, { status: 500 });
        }

    } catch (error) {
        console.error("API Error:", error);
        return NextResponse.json({ error: "Internal Server Error" }, { status: 500 });
    }
}
