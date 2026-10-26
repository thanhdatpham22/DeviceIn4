"""Verify locale coverage, resource uniqueness and format-argument parity."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1] / 'app/src/main/res'
folders = ['values', 'values-vi', 'values-fr', 'values-ja', 'values-b+zh+Hans']
baseline = None
for folder in folders:
    tree = ET.parse(root/folder/'strings.xml').getroot()
    values = {item.attrib['name']: ''.join(item.itertext()) for item in tree if item.tag == 'string'}
    assert len(values) == len(tree.findall('string')), f'Duplicate resource in {folder}'
    if baseline is None: baseline = values
    assert values.keys() == baseline.keys(), f'Incomplete locale {folder}'
    for key, text in values.items():
        expected = sorted(re.findall(r'%\d+\$[sd]', baseline[key]))
        assert sorted(re.findall(r'%\d+\$[sd]', text)) == expected, (folder, key)
    quantities = tree.find("plurals[@name='visible_app_count']")
    assert quantities is not None and {i.attrib['quantity'] for i in quantities} == {'one', 'other'}
    assert all('%d' in ''.join(item.itertext()) for item in quantities)
    print(f'{folder}: {len(values)} strings, plural forms and format arguments OK')
print('All five locale resource sets are complete.')
