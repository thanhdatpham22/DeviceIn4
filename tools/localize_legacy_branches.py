"""One-time migration of bilingual branches to resource-backed lookups."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

def quoted(source, start):
    if start >= len(source) or source[start] != '"': return None
    i = start + 1
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
        if source[i] == '"': return i + 1
        i += 1
    return None

for name in ('MainActivity.kt', 'SensorLiveChart.kt'):
    path = ROOT/'app/src/main/java/com/devicein4/app'/name
    source = path.read_text(encoding='utf-8')
    edits = []
    for match in re.finditer(r'if\s*\(\s*(?:englishLanguage\.value|english)\s*\)\s*', source):
        if source[:match.start()].endswith(('localized(', 'LocalizationManager.localize(')): continue
        if any(start <= match.start() < end for start,end in edits): continue
        en = quoted(source, match.end())
        if not en: continue
        separator = re.match(r'\s*else\s*', source[en:])
        if not separator: continue
        end = quoted(source, en + separator.end())
        if end: edits.append((match.start(), end))
    function = 'localized' if name == 'MainActivity.kt' else 'LocalizationManager.localize'
    for start,end in reversed(edits): source = source[:start] + function + '(' + source[start:end] + ')' + source[end:]
    path.write_text(source, encoding='utf-8')
    print(name, len(edits), 'branches migrated')
