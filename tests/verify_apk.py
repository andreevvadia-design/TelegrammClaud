import io, json, pathlib, zipfile, hashlib
root=pathlib.Path(__file__).resolve().parents[1]
apk=root/'TMessagesProj_App/build/outputs/apk/afat/debug/app.apk'
with zipfile.ZipFile(apk) as z:
    assert z.testzip() is None
    py=zipfile.ZipFile(io.BytesIO(z.read('assets/chaquopy/app.imy')))
    source=root/'TMessagesProj/src/main/python'
    names=['claud_engine.py','base_plugin.py','account_context.py','client_utils.py','android_utils.py','native_events.py','ui/settings.py']
    matches={n:n in py.namelist() and py.read(n)==(source/n).read_bytes() for n in names}
    dex=b''.join(z.read(n) for n in z.namelist() if n.endswith('.dex'))
    classes={n:n.encode() in dex for n in ['ClaudNotificationsActivity','ClaudPetView','ClaudPetOverlay','ClaudModeSettingsActivity','ClaudProfileActivity','ClaudPluginInstaller','ClaudPillStackView','PillRegistry','PillStackConfig','JavaHookBridge','de/robv/android/xposed/XposedBridge']}
    for asset in (root/'TMessagesProj/src/main/assets/claud_plugins').glob('*.plugin'):
        assert z.read('assets/claud_plugins/'+asset.name)==asset.read_bytes(), asset.name
    bundled=z.read('assets/claud_plugins/custom_profile.plugin')
    assert bundled==(root/'TMessagesProj/src/main/assets/claud_plugins/custom_profile.plugin').read_bytes()
    import ast,base64,zlib
    values={node.targets[0].id:ast.literal_eval(node.value) for node in ast.parse(bundled.decode('utf-8')).body if isinstance(node,ast.Assign) and isinstance(node.targets[0],ast.Name) and node.targets[0].id in ('_DEX_B64','_DEX_SHA')}
    assert hashlib.sha256(zlib.decompress(base64.b64decode(values['_DEX_B64']))).hexdigest()==values['_DEX_SHA']=='47d4f4d2ef2006b97d6762babc59aed239465aeeabbd1ee4c68090f2c1466206'
    libs=[n for n in z.namelist() if n.startswith('lib/') and ('aliu' in n.lower() or 'lsplant' in n.lower())]
    result=dict(python=matches,classes=classes,hook_libraries=libs,bytes=apk.stat().st_size,sha256=hashlib.sha256(apk.read_bytes()).hexdigest())
    print(json.dumps(result,indent=2))
    assert all(matches.values()), 'Packaged Python differs from current sources'
    assert all(classes.values()), 'Missing classes'
    assert libs, 'Missing hook native libraries'
