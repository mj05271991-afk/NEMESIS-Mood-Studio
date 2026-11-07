#!/usr/bin/env python3
"""Wrap a freshly signed HMG-API mod in a proven NEMESIS installer template.

Requires two explicit inputs: the original v0.2.0 *inner* NEMESIS ZIP and a
NEWLY SIGNED HMG-API mod ZIP. Never reuse the original signature on edited files.
This preserves the original NEMESIS installation scripts and descriptor fields.
"""
import argparse
import io
import pathlib
import zipfile

def build(template_path, signed_path, output_path):
    with zipfile.ZipFile(template_path) as template, zipfile.ZipFile(signed_path) as signed:
        template_files = set(template.namelist())
        signed_files = set(signed.namelist())
        required = {'NEMESIS.data','mod/id','scripts/preinstall.sh',
                    'scripts/postinstall.sh','scripts/preuninstall.sh','bin/open.sh'}
        missing = required - template_files
        if missing:
            raise ValueError('Missing installer template entries: '+', '.join(sorted(missing)))
        if not {'META-HMGAPI/manifest.json','META-HMGAPI/signature','mod.json',
                'app/MoonStudio.apk','moon.sh','icons/tile.png',
                'icons/tile_disabled.png'}.issubset(signed_files):
            raise ValueError('Signed mod lacks required files')
        import json
        config=json.loads(signed.read('mod.json'))
        if config.get('id') != 'moon-studio':
            raise ValueError('Signed mod has incorrect mod ID')
        if template.read('mod/id').strip()!=b'moon-studio':
            raise ValueError('Template targets another HMG-API mod')
        desc=template.read('NEMESIS.data').decode()
        if 'depends="hmgapi@>=1.0"' not in desc:
            raise ValueError('Template lacks HMG-API dependency')
        version=str(config['version'])
        import re
        desc=re.sub(r'(?m)^package-version=.*$', 'package-version='+version, desc)
        inner=io.BytesIO()
        with zipfile.ZipFile(inner,'w',zipfile.ZIP_DEFLATED) as dst:
            for item in template.infolist():
                if item.is_dir():continue
                name=item.filename
                if name=='mod/package.zip':dst.writestr(name,pathlib.Path(signed_path).read_bytes())
                elif name=='NEMESIS.data':dst.writestr(name,desc)
                else:dst.writestr(item,template.read(name))
            if 'mod/package.zip' not in template_files:
                dst.writestr('mod/package.zip',pathlib.Path(signed_path).read_bytes())
        output_path=pathlib.Path(output_path)
        output_path.parent.mkdir(parents=True,exist_ok=True)
        with zipfile.ZipFile(output_path,'w',zipfile.ZIP_DEFLATED) as outer:
            outer.writestr('NEMESIS.data','[nemesis]\nversion=1.0\ntype=install\npath=packages/hmgapi.moon-studio.zip\n')
            outer.writestr('NEMESIS.sh','#!/system/bin/sh\nexit 0\n')
            outer.writestr('packages/hmgapi.moon-studio.zip',inner.getvalue())
    print('Created structurally wrapped installer:',output_path)
    print('IMPORTANT: Signature validity and vehicle operation are NOT verified here.')

if __name__=='__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--template-inner',required=True)
    p.add_argument('--signed-mod',required=True)
    p.add_argument('--output',required=True)
    a=p.parse_args()
    build(a.template_inner,a.signed_mod,a.output)
