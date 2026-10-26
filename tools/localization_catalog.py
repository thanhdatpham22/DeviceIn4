"""Extract legacy UI aliases into Android string resources; no hardware data is translated."""
from pathlib import Path
import hashlib, json, re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java/com/devicein4/app'
OUT = ROOT / 'tools/localization_catalog.json'

def quoted(source, start):
    if start >= len(source) or source[start] != '"': return None
    i, braces = start + 1, 0
    while i < len(source):
        if source[i] == '\\': i += 2; continue
        if source.startswith('${', i):
            depth, i = 1, i + 2
            while i < len(source) and depth:
                if source[i] == '"':
                    nested = quoted(source, i)
                    if nested: i = nested[1]; continue
                if source[i] == '{': depth += 1
                if source[i] == '}': depth -= 1
                i += 1
            continue
        if source[i] == '"': return source[start + 1:i], i + 1
        i += 1
    return None

def decode(value):
    return value.replace('\\n', '\n').replace('\\t', '\t').replace('\\"', '"').replace('\\\\', '\\')

def template(raw):
    result, i, n = '', 0, 0
    while i < len(raw):
        if raw.startswith('${', i):
            depth, i = 1, i + 2
            while i < len(raw) and depth:
                if raw[i] == '"':
                    nested = quoted(raw, i)
                    if nested: i = nested[1]; continue
                if raw[i] == '{': depth += 1
                if raw[i] == '}': depth -= 1
                i += 1
            n += 1; result += f'%{n}$s'
        elif raw[i] == '$' and i + 1 < len(raw) and (raw[i+1].isalpha() or raw[i+1] == '_'):
            i += 1
            while i < len(raw) and (raw[i].isalnum() or raw[i] == '_'): i += 1
            n += 1; result += f'%{n}$s'
        else:
            result += raw[i]; i += 1
    return decode(result), n

entries, by_alias = [], {}
def add(vi, en):
    vi, vn = template(vi); en, en_n = template(en)
    if vn != en_n: return
    if vi in by_alias:
        # Preserve an existing name when a label is reused.
        return
    slug = re.sub('[^a-z0-9]+', '_', en.lower()).strip('_')[:55] or 'message'
    key = 'ui_' + slug + '_' + hashlib.sha1(vi.encode()).hexdigest()[:6]
    entry = {'name': key, 'vi': vi, 'en': en, 'args': vn}
    entries.append(entry); by_alias[vi] = key

main = (SRC / 'MainActivity.kt').read_text(encoding='utf-8')
if 'private val englishDictionary = mapOf(' not in main:
    print('Legacy extraction is complete. Edit the catalog/translation files and run tools/emit_locales.py.')
    raise SystemExit(0)
dictionary = main.split('private val englishDictionary = mapOf(', 1)[1].split('private val dictionaries', 1)[0]
for match in re.finditer(r'"((?:\\.|[^"\\])*)"\s+to\s+"((?:\\.|[^"\\])*)"', dictionary):
    add(match[1], match[2])
for file in ['MainActivity.kt', 'SensorLiveChart.kt', 'DeviceWidgetProvider.kt']:
    source = (SRC / file).read_text(encoding='utf-8')
    for match in re.finditer(r'if\s*\(\s*(?:englishLanguage\.value|english)\s*\)\s*', source):
        en = quoted(source, match.end())
        if not en: continue
        next_match = re.match(r'\s*else\s*', source[en[1]:])
        if not next_match: continue
        vi = quoted(source, en[1] + next_match.end())
        if vi: add(vi[0], en[0])

# Common technical headings absent from the legacy dictionary.
extra = {
    'Device': 'Thiết bị', 'OS': 'Hệ điều hành', 'Storage': 'Lưu trữ', 'Display': 'Màn hình',
    'Battery': 'Pin', 'Network': 'Mạng', 'Camera': 'Camera', 'Sensor': 'Cảm biến', 'Features': 'Tính năng',
    'Device Information': 'Thông tin thiết bị', 'System': 'Hệ thống', 'Dark': 'Tối', 'Light': 'Sáng',
    'Default': 'Mặc định', 'Custom': 'Tùy chỉnh', 'Mint': 'Bạc hà', 'Violet': 'Tím',
    'Amber': 'Hổ phách', 'Rose': 'Hồng', 'Blue': 'Xanh dương', 'Version': 'Phiên bản',
    'Vendor / OEM': 'Nhà cung cấp / OEM', 'Resolution': 'Độ phân giải', 'Power': 'Điện năng',
    'Maximum Range': 'Dải đo tối đa', 'Sensor Name': 'Tên cảm biến', 'Sensor Type': 'Loại cảm biến',
    'Reporting Mode': 'Chế độ báo cáo', 'Continuous': 'Liên tục', 'On change': 'Khi thay đổi',
    'One shot': 'Kích hoạt một lần', 'Special trigger': 'Kích hoạt đặc biệt',
    'CPU': 'CPU', 'GPU': 'GPU', 'Privacy': 'Quyền riêng tư',
}
for en, vi in extra.items(): add(vi, en)
extra_labels = {
    'RAM hệ thống':'System RAM', 'RAM đang sử dụng':'RAM in use', 'RAM khả dụng':'Available RAM',
    'Ngưỡng thiếu RAM':'Low memory threshold', 'Bộ nhớ heap của app':'App heap memory', 'RAM quảng cáo':'Advertised RAM',
    'Áp lực bộ nhớ':'Memory pressure', 'Bình thường':'Normal', 'Hệ thống báo thiếu RAM':'System reports low memory',
    'Bộ nhớ đệm (Cache)':'CPU cache', 'Bộ điều phối (Governor)':'CPU governor',
    'Dải xung từng nhân (Min - Max)':'Per-core frequency range', 'ABI hỗ trợ':'Supported ABIs',
    'Tập lệnh (Features)':'Instruction set', 'Nhà sản xuất SoC':'SoC manufacturer',
    'Chip / nền tảng':'Chip / platform', 'Kiến trúc 64 bit':'64-bit architecture', 'Nhân logic':'Logical cores',
    'Tổng dung lượng phân vùng dữ liệu':'Total data partition capacity', 'Đã dùng / không khả dụng':'Used / unavailable',
    'Tệp đang chiếm':'Occupied by files', 'Trống nhưng dành riêng':'Reserved free space', 'Còn trống dùng được':'Usable free space',
    'Cách tính':'Calculation', 'Dữ liệu ứng dụng (không cache)':'App data excluding cache',
    'Cache ứng dụng (trong dữ liệu)':'App cache included in data', 'Mã ứng dụng (hệ thống báo)':'App code reported by system',
    'Giới hạn':'Limitations', 'Phân loại':'Classification', 'Swap toàn hệ thống':'System-wide swap',
    'ZRAM & swap · ngoài bộ nhớ trong':'ZRAM & swap · separate from internal storage',
    'Chế độ báo thức':'Wake-up mode', 'Nhà sản xuất cell pin':'Battery cell manufacturer',
    'Chức năng ống kính':'Lens function', 'Góc nhìn thực tế (FOV)':'Actual field of view (FOV)',
    'Độ phân giải thực cảm biến':'Sensor pixel resolution', 'Kích thước cảm biến vật lý':'Physical sensor size',
    'Định danh phần cứng':'Hardware identity', 'Cấp phần cứng Camera2':'Camera2 hardware level',
    'Chống rung quang học (OIS)':'Optical image stabilization (OIS)', 'Đèn flash trợ sáng':'Flash light',
    'Tốc độ khung hình (FPS)':'Frame rate (FPS)', 'Dải nhạy sáng ISO':'ISO sensitivity range',
    'Dải thời gian phơi sáng':'Exposure time range', 'Nhận diện khuôn mặt':'Face detection',
    'Ống kính vật lý liên kết':'Linked physical cameras', 'Tính năng chuyên sâu':'Advanced capabilities',
    'Tiện ích mở rộng (Extensions)':'Extensions', 'Zoom kỹ thuật số tối đa':'Maximum digital zoom',
    'Bản dựng':'Build', 'Tên sản phẩm':'Product name', 'Thương hiệu':'Brand', 'Mã thiết bị':'Device code',
    'Bo mạch':'Board', 'Hãng / mẫu':'Manufacturer / model', 'Thiết bị / bo mạch':'Device / board',
    'Thiết bị & hệ thống':'Device & system', 'CPU & kiến trúc':'CPU & architecture',
    'Model':'Model', 'Board':'Board', 'Brand':'Brand', 'Build Time':'Build time', 'Product':'Product',
    'User':'User', 'Host':'Host', 'Description':'Description', 'Processor':'Processor', 'Vendor':'Vendor',
    'Connection Status':'Connection status', 'Data Type / Network Type':'Transport / network type',
    'IP Address':'IP address', 'Link Speed':'Link speed', 'Wi-Fi frequency':'Wi-Fi frequency',
    'Wi-Fi standard':'Wi-Fi standard', 'Wi-Fi MAC Address':'Wi-Fi MAC address',
    'Screen Resolution':'Screen resolution', 'Security patch':'Security patch', 'Security level':'Security level',
    'Version Name':'Version name', 'GPU renderer':'GPU renderer', 'GPU vendor':'GPU vendor',
    'Driver OpenGL':'OpenGL driver', 'Max Texture Size':'Maximum texture size',
    'isDynamicSensor':'Dynamic sensor', 'isWakeUpSensor':'Wake-up sensor', 'Wide color gamut':'Wide color gamut',
}
for vi, en in extra_labels.items(): add(vi, en)
OUT.write_text(json.dumps(entries, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
for index, entry in enumerate(entries): print(f'{index:03d}\t{entry["en"].replace(chr(10), " / ")}')
print(f'COUNT={len(entries)}')
