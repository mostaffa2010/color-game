#!/usr/bin/env python3
"""
Convert AI Image to Color-by-Number Level
Generates:
1. lines.png        - Transparent PNG of black boundary outlines
2. regions_map.png  - 24-bit RGB PNG encoding (R: id & 0xFF, G: (id >> 8) & 0xFF, B: colorId)
3. level.json       - Level metadata, palette colors, region centers & counts
"""

import sys
import os
import json
import argparse
import cv2
import numpy as np
from PIL import Image
from scipy.cluster.vq import kmeans, vq
from scipy.ndimage import distance_transform_edt

def convert_image_to_level(
    image_path: str,
    output_dir: str,
    level_id: str = "bakery_01",
    level_title: str = "The Village Bakery",
    chapter_num: int = 1,
    chapter_title: str = "Chapter 1: The Whispering Village",
    story_intro: str = "In the quiet heart of the old kingdom, the warmth of the hearth brings life to the cobblestones. Bring back its morning colors!",
    story_outro: str = "The bakery glows with golden crusts and fragrant pastries! As the villagers arrive, the baker hands you a mysterious parchment...",
    num_colors: int = 32,
    min_region_area: int = 25,
    line_threshold: int = 75
):
    os.makedirs(output_dir, exist_ok=True)
    
    print(f"Reading image from {image_path}...")
    img = cv2.imread(image_path)
    if img is None:
        raise ValueError(f"Could not load image: {image_path}")
    
    h, w, _ = img.shape
    img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    # 1. Extract Line Art (Outlines)
    print("Extracting smooth antialiased outlines...")
    # Pixels darker than line_threshold + margin are lines
    # Smooth alpha falloff for nice antialiasing
    threshold_high = line_threshold + 20
    alpha = np.clip((threshold_high - gray.astype(float)) / (threshold_high - 30 + 1e-5) * 255.0, 0, 255).astype(np.uint8)
    
    lines_rgba = np.zeros((h, w, 4), dtype=np.uint8)
    # Color of lines is dark charcoal #1A1A1A
    lines_rgba[:, :, 0] = 26
    lines_rgba[:, :, 1] = 26
    lines_rgba[:, :, 2] = 26
    lines_rgba[:, :, 3] = alpha
    
    lines_img = Image.fromarray(lines_rgba, mode="RGBA")
    lines_path = os.path.join(output_dir, "lines.png")
    lines_img.save(lines_path, optimize=True)
    print(f"Saved outlines to {lines_path}")
    
    # 2. Segment connected non-line components
    print("Segmenting regions...")
    is_line = (gray < line_threshold).astype(np.uint8)
    non_line = (1 - is_line).astype(np.uint8)
    
    num_labels, labels, stats, centroids = cv2.connectedComponentsWithStats(non_line, connectivity=4)
    print(f"Detected {num_labels} raw components.")
    
    # Filter by minimum area
    valid_labels = [l for l in range(1, num_labels) if stats[l, cv2.CC_STAT_AREA] >= min_region_area]
    print(f"Retained {len(valid_labels)} valid regions (area >= {min_region_area}px).")
    
    # Map valid regions to 1..N
    mapped_labels = np.zeros((h, w), dtype=np.int32)
    for new_id, old_id in enumerate(valid_labels, start=1):
        mapped_labels[labels == old_id] = new_id
        
    # Fill boundary lines and tiny specks using Voronoi nearest-neighbor
    print("Filling boundaries via Voronoi nearest-neighbor...")
    unassigned_mask = (mapped_labels == 0)
    _, indices = distance_transform_edt(unassigned_mask, return_indices=True)
    filled_labels = mapped_labels[tuple(indices)]
    
    # 3. Compute region features: median color and label position (pole of inaccessibility)
    print("Computing region features and label coordinates...")
    region_colors = []
    region_records = []
    
    for new_id, old_id in enumerate(valid_labels, start=1):
        mask = (labels == old_id)
        pixels = img_rgb[mask]
        median_col = np.median(pixels, axis=0)
        region_colors.append(median_col)
        
        # Label placement: use distance transform to find point furthest from boundaries
        region_binary = mask.astype(np.uint8)
        dist_map = cv2.distanceTransform(region_binary, cv2.DIST_L2, 5)
        _, max_val, _, max_loc = cv2.minMaxLoc(dist_map)
        
        # Fallback to centroid if max_val is 0
        if max_val <= 0:
            cx, cy = int(centroids[old_id][0]), int(centroids[old_id][1])
        else:
            cx, cy = int(max_loc[0]), int(max_loc[1])
            
        region_records.append({
            "id": new_id,
            "labelX": cx,
            "labelY": cy,
            "maxRadius": round(float(max_val), 1),
            "area": int(stats[old_id, cv2.CC_STAT_AREA])
        })
        
    # 4. Color Quantization / Clustering into palette
    print(f"Clustering colors into {num_colors} palette swatches...")
    region_colors_arr = np.array(region_colors, dtype=np.float32)
    
    centroids_colors, _ = kmeans(region_colors_arr, num_colors)
    # Sort palette colors by luminance for aesthetically pleasing palette display
    centroids_colors = sorted(centroids_colors, key=lambda c: (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]))
    centroids_colors = np.array(centroids_colors, dtype=np.float32)
    
    palette_indices, _ = vq(region_colors_arr, centroids_colors)
    
    # Assign color IDs to regions (1-indexed)
    region_id_to_color_id = {}
    color_region_counts = {c: 0 for c in range(1, num_colors + 1)}
    
    for i, rec in enumerate(region_records):
        cid = int(palette_indices[i] + 1)
        rec["colorId"] = cid
        region_id_to_color_id[rec["id"]] = cid
        color_region_counts[cid] += 1
        
    palette_list = []
    for c_idx in range(num_colors):
        cid = c_idx + 1
        rgb = centroids_colors[c_idx].astype(int)
        hex_code = f"#{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}"
        palette_list.append({
            "id": cid,
            "hex": hex_code,
            "r": int(rgb[0]),
            "g": int(rgb[1]),
            "b": int(rgb[2]),
            "totalRegions": color_region_counts[cid]
        })
        
    # 5. Create regions_map.png
    print("Generating regions_map.png...")
    # RGB channels:
    # R: region_id & 0xFF
    # G: (region_id >> 8) & 0xFF
    # B: color_id & 0xFF
    regions_map_rgb = np.zeros((h, w, 3), dtype=np.uint8)
    
    # Vectorized mapping
    reg_id_grid = filled_labels.astype(np.int32)
    r_channel = (reg_id_grid & 0xFF).astype(np.uint8)
    g_channel = ((reg_id_grid >> 8) & 0xFF).astype(np.uint8)
    
    # Create lookup array for color_id from region_id
    color_lookup = np.zeros(len(valid_labels) + 1, dtype=np.uint8)
    for rid, cid in region_id_to_color_id.items():
        color_lookup[rid] = cid
    b_channel = color_lookup[reg_id_grid]
    
    regions_map_rgb[:, :, 0] = r_channel
    regions_map_rgb[:, :, 1] = g_channel
    regions_map_rgb[:, :, 2] = b_channel
    
    regions_map_img = Image.fromarray(regions_map_rgb, mode="RGB")
    regions_map_path = os.path.join(output_dir, "regions_map.png")
    regions_map_img.save(regions_map_path, optimize=True)
    print(f"Saved regions map to {regions_map_path}")
    
    # 6. Save level.json
    level_data = {
        "id": level_id,
        "title": level_title,
        "chapter": chapter_num,
        "chapterTitle": chapter_title,
        "storyIntro": story_intro,
        "storyOutro": story_outro,
        "width": w,
        "height": h,
        "totalRegions": len(region_records),
        "totalColors": num_colors,
        "palette": palette_list,
        "regions": region_records
    }
    
    json_path = os.path.join(output_dir, "level.json")
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(level_data, f, indent=2, ensure_ascii=False)
    print(f"Saved level JSON metadata to {json_path}")
    
    print("\nLevel conversion finished successfully!")
    print(f"  Level Title: {level_title}")
    print(f"  Total Regions: {len(region_records)}")
    print(f"  Palette Colors: {num_colors}")
    print(f"  Output Directory: {output_dir}")
    return level_data

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert an image to a Color-by-Number level")
    parser.add_argument("--image", required=True, help="Input image path")
    parser.add_argument("--outdir", required=True, help="Output folder")
    parser.add_argument("--level-id", default="bakery_01")
    parser.add_argument("--title", default="The Village Bakery")
    parser.add_argument("--chapter", type=int, default=1)
    parser.add_argument("--colors", type=int, default=32)
    args = parser.parse_args()
    
    convert_image_to_level(
        image_path=args.image,
        output_dir=args.outdir,
        level_id=args.level_id,
        level_title=args.title,
        chapter_num=args.chapter,
        num_colors=args.colors
    )
