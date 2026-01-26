
"use client";

import React, { useLayoutEffect, useRef, useMemo, useState, useCallback, useEffect } from "react";
import { Canvas } from "@react-three/fiber";
import { OrbitControls, Center, PerspectiveCamera, Environment, ContactShadows } from "@react-three/drei";
import * as THREE from "three";
import { exportToHytaleJson } from "@/lib/hytale_exporter";

function VoxelMesh({ voxels, onVoxelClick }) {
    const meshRef = useRef();

    // Create a reusable geometry and material
    const geometry = useMemo(() => new THREE.BoxGeometry(1, 1, 1), []);
    // Material - sleek emerald green with some shine
    const material = useMemo(
        () =>
            new THREE.MeshStandardMaterial({
                color: "#10b981", // Emerald 500
                roughness: 0.2,
                metalness: 0.1,
            }),
        []
    );

    useLayoutEffect(() => {
        if (!meshRef.current) return;

        const tempObject = new THREE.Object3D();

        voxels.forEach((voxel, index) => {
            const [z, y, x] = voxel;
            tempObject.position.set(x, y, z);
            tempObject.updateMatrix();
            meshRef.current.setMatrixAt(index, tempObject.matrix);
        });

        // Hide unused instances if voxels array shrunk (though we remount on length change usually)
        meshRef.current.instanceMatrix.needsUpdate = true;
    }, [voxels]);

    const handleClick = useCallback((e) => {
        e.stopPropagation();
        // Get instance ID
        const instanceId = e.instanceId;
        if (instanceId === undefined) return;

        // Pass event to parent handler for add/remove logic
        onVoxelClick(e, instanceId);
    }, [onVoxelClick]);

    return (
        <instancedMesh
            ref={meshRef}
            args={[geometry, material, voxels.length]}
            castShadow
            receiveShadow
            onClick={handleClick}
            onPointerOver={() => document.body.style.cursor = 'pointer'}
            onPointerOut={() => document.body.style.cursor = 'move'}
        />
    );
}

export default function VoxelCanvas({ data }) {
    // Local state for voxels to allow editing
    const [voxels, setVoxels] = useState([]);

    // Initialize voxels from props
    useEffect(() => {
        if (data?.voxels) {
            setVoxels([...data.voxels]); // Clone to allow mutation
        }
    }, [data]);

    const handleVoxelClick = (event, instanceId) => {
        // Shift + Click = Remove
        if (event.shiftKey) {
            const newVoxels = voxels.filter((_, idx) => idx !== instanceId);
            setVoxels(newVoxels);
            return;
        }

        // Click = Add Voxel at normal face
        // We need to calculate the position of the new voxel based on the clicked face normal
        // The event gives us the point and face, but for instanced mesh it's tricky to get exact "neighbor" logic easily without raycasting logic
        // For V1, let's implement just Removal to be safe, or Add on top

        // Getting matrix of clicked instance
        const mesh = event.object;
        const matrix = new THREE.Matrix4();
        mesh.getMatrixAt(instanceId, matrix);
        const position = new THREE.Vector3();
        position.setFromMatrixPosition(matrix);

        const normal = event.face.normal;
        // Transform normal to world space if object was rotated (here it is not)

        const newPos = position.clone().add(normal);
        const newVoxel = [Math.round(newPos.z), Math.round(newPos.y), Math.round(newPos.x)];

        // Check if voxel already exists
        const exists = voxels.some(v => v[0] === newVoxel[0] && v[1] === newVoxel[1] && v[2] === newVoxel[2]);
        if (!exists) {
            setVoxels([...voxels, newVoxel]);
        }
    };

    const handleDownload = () => {
        const jsonString = exportToHytaleJson(voxels);
        const blob = new Blob([jsonString], { type: "application/json" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `hytale_model_${Date.now()}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    return (
        <div className="w-full h-full min-h-[400px] bg-slate-100 dark:bg-zinc-950 rounded-lg overflow-hidden cursor-move relative group">
            <div className="absolute top-2 left-2 z-10 px-2 py-1 bg-black/50 text-white text-xs rounded backdrop-blur-sm pointer-events-none opacity-50 group-hover:opacity-100 transition-opacity">
                <strong>Click</strong> to Add • <strong>Shift+Click</strong> to Remove
            </div>

            <div className="absolute top-2 right-2 z-10">
                <button
                    onClick={handleDownload}
                    className="px-3 py-1 bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold rounded shadow-lg transition-transform active:scale-95 flex items-center gap-1"
                >
                    Download JSON
                </button>
            </div>

            <Canvas shadows dpr={[1, 2]} camera={{ position: [20, 20, 20], fov: 45 }}>
                <ambientLight intensity={0.4} />
                <directionalLight
                    position={[10, 20, 10]}
                    intensity={1.2}
                    castShadow
                    shadow-mapSize={[1024, 1024]}
                />
                <Environment preset="city" />

                <Center>
                    {voxels.length > 0 && <VoxelMesh voxels={voxels} onVoxelClick={handleVoxelClick} />}
                </Center>

                <ContactShadows position={[0, -0.1, 0]} opacity={0.4} scale={40} blur={2} far={4.5} />
                <OrbitControls makeDefault minDistance={5} maxDistance={100} />
            </Canvas>
        </div>
    );
}
