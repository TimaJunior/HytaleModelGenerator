
"use client";

import React, { useLayoutEffect, useRef, useMemo, useState, useCallback, useEffect } from "react";
import { Canvas } from "@react-three/fiber";
import { OrbitControls, Center, PerspectiveCamera, Environment, ContactShadows } from "@react-three/drei";
import * as THREE from "three";
import { exportToHytaleJson } from "@/lib/hytale_exporter";

/**
 * VoxelMesh — renders instanced colored voxels.
 *
 * Expects voxels in NEW format:
 *   [{pos: [x, y, z], color: "#RRGGBB"}, ...]
 *
 * Also supports legacy format:
 *   [[z, y, x], ...]  (renders with default color)
 */
function VoxelMesh({ voxels, onVoxelClick }) {
    const meshRef = useRef();

    const geometry = useMemo(() => new THREE.BoxGeometry(1, 1, 1), []);

    // InstancedMesh з підтримкою вертексних кольорів
    const material = useMemo(
        () =>
            new THREE.MeshStandardMaterial({
                vertexColors: true,
                roughness: 0.3,
                metalness: 0.15,
            }),
        []
    );

    useLayoutEffect(() => {
        if (!meshRef.current) return;

        const tempObject = new THREE.Object3D();
        const colorsArray = new Float32Array(voxels.length * 3);
        const color = new THREE.Color();

        voxels.forEach((voxel, index) => {
            let x, y, z, hexColor;

            // Новий формат: {pos: [x, y, z], color: "#RRGGBB"}
            if (voxel && typeof voxel === "object" && voxel.pos) {
                [x, y, z] = voxel.pos;
                hexColor = voxel.color || "#10B981";
            } else {
                // Legacy формат: [z, y, x]
                [z, y, x] = voxel;
                hexColor = "#10B981";
            }

            tempObject.position.set(x, y, z);
            tempObject.updateMatrix();
            meshRef.current.setMatrixAt(index, tempObject.matrix);

            // Колір
            color.set(hexColor);
            colorsArray[index * 3 + 0] = color.r;
            colorsArray[index * 3 + 1] = color.g;
            colorsArray[index * 3 + 2] = color.b;
        });

        // Оновлюємо матриці та кольори
        meshRef.current.instanceMatrix.needsUpdate = true;

        // Встановлюємо instanceColor
        meshRef.current.instanceColor = new THREE.InstancedBufferAttribute(
            colorsArray, 3
        );
    }, [voxels]);

    const handleClick = useCallback((e) => {
        e.stopPropagation();
        const instanceId = e.instanceId;
        if (instanceId === undefined) return;
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
    const [voxels, setVoxels] = useState([]);

    useEffect(() => {
        if (data?.voxels) {
            setVoxels([...data.voxels]);
        }
    }, [data]);

    const handleVoxelClick = (event, instanceId) => {
        // Shift + Click = Remove
        if (event.shiftKey) {
            setVoxels(voxels.filter((_, idx) => idx !== instanceId));
            return;
        }

        // Click = Add Voxel adjacent to clicked face
        const mesh = event.object;
        const matrix = new THREE.Matrix4();
        mesh.getMatrixAt(instanceId, matrix);
        const position = new THREE.Vector3();
        position.setFromMatrixPosition(matrix);

        const normal = event.face.normal;
        const newPos = position.clone().add(normal);
        const np = [Math.round(newPos.x), Math.round(newPos.y), Math.round(newPos.z)];

        // Колір для нового вокселя — беремо від сусіда
        const clickedVoxel = voxels[instanceId];
        const clickedColor = clickedVoxel?.color || "#10B981";

        const exists = voxels.some((v) => {
            const p = v.pos || [v[2], v[1], v[0]];
            return p[0] === np[0] && p[1] === np[1] && p[2] === np[2];
        });

        if (!exists) {
            setVoxels([...voxels, { pos: np, color: clickedColor }]);
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

    const voxelCount = voxels.length;

    return (
        <div className="w-full h-full min-h-[400px] bg-slate-100 dark:bg-zinc-950 rounded-lg overflow-hidden cursor-move relative group">
            <div className="absolute top-2 left-2 z-10 px-2 py-1 bg-black/50 text-white text-xs rounded backdrop-blur-sm pointer-events-none opacity-50 group-hover:opacity-100 transition-opacity">
                <strong>Click</strong> to Add • <strong>Shift+Click</strong> to Remove
                {voxelCount > 0 && <span className="ml-2 text-emerald-400">({voxelCount} voxels)</span>}
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
                <ambientLight intensity={0.5} />
                <directionalLight
                    position={[10, 20, 10]}
                    intensity={1.5}
                    castShadow
                    shadow-mapSize={[1024, 1024]}
                />
                <pointLight position={[-10, 10, -10]} intensity={0.5} color="#6366f1" />
                <Environment preset="city" />

                <Center>
                    {voxels.length > 0 && <VoxelMesh voxels={voxels} onVoxelClick={handleVoxelClick} />}
                </Center>

                <ContactShadows position={[0, -0.1, 0]} opacity={0.4} scale={60} blur={2} far={6} />
                <OrbitControls makeDefault minDistance={5} maxDistance={150} />
            </Canvas>
        </div>
    );
}
