
import numpy as np

def sanitize_colors(voxels):
    """
    Ensures no voxel colors are pure black (#000000) or pure white (#FFFFFF).
    For the current binary voxel grid, this is a placeholder.
    If/when we return (C, D, H, W) with RGB channels, this logic applies.
    """
    # Placeholder for future RGB support
    # If voxels was (4, D, H, W) where 0-2 are RGB:
    # mask_black = (r==0) & (g==0) & (b==0)
    # voxels[0][mask_black] = 26/255.0 # #1A1A1A
    
    return voxels

def sanitize_hex_color(hex_color):
    """
    Sanitizes a single hex string.
    """
    hex_color = hex_color.upper()
    if hex_color == "#000000" or hex_color == "#000":
        return "#1A1A1A"
    if hex_color == "#FFFFFF" or hex_color == "#FFF":
        return "#F0F0F0"
    return hex_color
