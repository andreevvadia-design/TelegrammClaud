"""Create a source-only publication directory without Git history or local artifacts."""
import pathlib,subprocess,shutil
root=pathlib.Path(__file__).resolve().parents[1]
out=root.parent/'claud-mode-publish'
files=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=root).decode().split('\0')
count=0
for name in files:
    if not name:continue
    p=pathlib.PurePosixPath(name)
    if p.parts[0] in ('design','.git') or any(part in ('build','.gradle','.cxx','__pycache__') for part in p.parts):continue
    if p.suffix.lower() in ('.apk','.aab','.log','.keystore','.jks','.pem','.p12','.pfx','.pyc'):continue
    if p.name in ('google-services.json','local.properties') or p.name.startswith(('BUILD-STATUS','PERSONAL-SETUP','.env')):continue
    src=root/name
    if not src.is_file() or src.is_symlink():continue
    dest=out/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dest);count+=1
print(f'Prepared {count} files in {out}; no Git history, signing keys, APKs or Firebase configuration copied.')
