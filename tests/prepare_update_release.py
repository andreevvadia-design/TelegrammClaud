"""Prepare an APK and update manifest for upload to an HTTPS release host."""
import argparse, hashlib, json, pathlib, shutil, urllib.parse
p=argparse.ArgumentParser()
p.add_argument('apk',type=pathlib.Path)
p.add_argument('--url',required=True,help='Public HTTPS URL of the uploaded APK')
p.add_argument('--version-code',required=True,type=int,help='Actual Android APK versionCode')
p.add_argument('--version',required=True)
p.add_argument('--out',type=pathlib.Path,default=pathlib.Path('release-update'))
a=p.parse_args()
u=urllib.parse.urlparse(a.url)
if u.scheme!='https' or not u.hostname or u.username or u.password:p.error('A public HTTPS URL without credentials is required')
if not a.apk.is_file():p.error('APK does not exist')
if a.version_code<=0:p.error('version code must be positive')
a.out.mkdir(parents=True,exist_ok=True)
target=a.out/'claud-mode-arm64.apk'
if a.apk.resolve()!=target.resolve():shutil.copy2(a.apk,target)
manifest={'schema':1,'package':'app.claudcode.messenger.beta','abi':'arm64-v8a','versionCode':a.version_code,'versionName':a.version,'url':a.url,'size':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest()}
(a.out/'update.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Prepared',target,'and',a.out/'update.json')
