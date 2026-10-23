import os
import re
import cairosvg

def convert_svg_fix_entities(svg_path, png_path, scale=2.0):
    try:
        with open(svg_path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()

        # 1. Bỏ phần <!DOCTYPE ... [...]> ở đầu file
        content_no_doctype = re.sub(r'<!DOCTYPE[^>]*(\[.*?\])?\s*>', '', content, flags=re.DOTALL)

        # 2. Fix lỗi "undefined entity": Thay thế tất cả các &entity_name; không xác định thành chuỗi rỗng
        clean_svg = re.sub(r'&[a-zA-Z0-9_]+;', '', content_no_doctype)

        # 3. Convert sang PNG
        cairosvg.svg2png(
            bytestring=clean_svg.encode('utf-8'),
            write_to=png_path,
            scale=scale
        )
        print(f"[SUCCESS] Đã chuyển đổi: {os.path.basename(svg_path)} -> {os.path.basename(png_path)}")

    except Exception as e:
        print(f"[ERROR] Lỗi khi chuyển đổi {os.path.basename(svg_path)}: {e}")

def convert_all(input_folder, output_folder, scale=2.0):
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    svg_files = [f for f in os.listdir(input_folder) if f.lower().endswith('.svg')]
    for file_name in svg_files:
        svg_path = os.path.join(input_folder, file_name)
        png_path = os.path.join(output_folder, os.path.splitext(file_name)[0] + ".png")
        convert_svg_fix_entities(svg_path, png_path, scale=scale)

if __name__ == "__main__":
    convert_all("./svg_input", "./png_output", scale=2.0)