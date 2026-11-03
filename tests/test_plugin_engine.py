import sys
import tempfile
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "TMessagesProj/src/main/python"))
from claud_engine import Engine

SOURCE = """from base_plugin import BasePlugin
__id__ = 'sample'
__name__ = 'Sample'
__version__ = '1.0'
class Sample(BasePlugin):
    def on_plugin_load(self):
        self.set_setting('loads', self.get_setting('loads', 0) + 1)
    def on_event(self, name, payload):
        return payload + '!'
"""
class EngineTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.engine = Engine(self.temp.name)
    def tearDown(self):
        for pid in list(self.engine.loaded): self.engine.disable(pid)
        self.temp.cleanup()
    def test_bundled_enables_once_and_respects_disable(self):
        self.engine.install_bundled(SOURCE)
        self.assertIn('sample', self.engine.loaded)
        self.engine.disable('sample')
        self.engine.install_bundled(SOURCE)
        self.assertNotIn('sample', self.engine.loaded)
        self.assertEqual(self.engine.setting('sample','loads'),1)

    def test_bundled_respects_safe_mode(self):
        (Path(self.temp.name)/'disable_all.flag').write_text('1')
        recovered=Engine(self.temp.name)
        recovered.install_bundled(SOURCE)
        self.assertNotIn('sample',recovered.loaded)
        self.assertTrue((Path(self.temp.name)/'sample.py').exists())

    def test_bundled_failed_first_load_is_not_retried(self):
        source=SOURCE+'\nraise RuntimeError("load failed")'
        self.engine.install_bundled(source)
        self.assertNotIn('sample',self.engine.loaded)
        self.assertIn('sample',self.engine.errors)
        self.engine.install_bundled(SOURCE)
        self.assertNotIn('sample',self.engine.loaded)

    def test_file_preview_does_not_execute_and_install_checks_identity(self):
        import json
        file=Path(self.temp.name)/'download.plugin'
        file.write_text(SOURCE+'\nraise RuntimeError("must not execute")',encoding='utf-8')
        preview=json.loads(self.engine.preview_file(str(file)))
        self.assertEqual(preview['__id__'],'sample')
        self.assertFalse(self.engine.loaded)
        self.assertFalse((Path(self.temp.name)/'sample.py').exists())
        self.engine.install_file(str(file),preview['sha256'],False)
        self.assertFalse(self.engine.loaded)
        file.write_text(SOURCE,encoding='utf-8')
        with self.assertRaisesRegex(ValueError,'changed after preview'):
            self.engine.install_file(str(file),preview['sha256'])

    def test_file_preview_enforces_limit_and_explicit_enable(self):
        import json
        file=Path(self.temp.name)/'download.plugin'
        file.write_bytes(b'#'*4194305)
        with self.assertRaisesRegex(ValueError,'exceeds'):
            self.engine.preview_file(str(file))
        file.write_text(SOURCE,encoding='utf-8')
        preview=json.loads(self.engine.preview_file(str(file)))
        self.engine.install_file(str(file),preview['sha256'],True)
        self.assertIn('sample',self.engine.loaded)

    def test_profile_editor_entries_survive_original_launcher(self):
        import json
        source=SOURCE.replace("'sample'", "'custom_profile'")+"""
    _core = True
    def create_settings(self):
        from ui.settings import Text
        return [Text('Оформить мой профиль'), Text('Native feature')]
"""
        self.engine.install(source)
        self.engine.enable('custom_profile')
        schema=json.loads(self.engine.settings_schema('custom_profile'))
        self.assertEqual([row['text'] for row in schema],['Оформить мой профиль','Мастерская оформления','Native feature'])
        calls=[]
        self.engine._open_custom_profile=lambda instance,workshop=False: calls.append(workshop)
        self.engine.setting_click('custom_profile',schema[0]['index'])
        self.engine.setting_click('custom_profile',schema[1]['index'])
        self.assertEqual(calls,[False,True])

    def test_optional_bundled_features_stay_off_and_keep_existing_choice(self):
        self.engine.install_bundled(SOURCE,False)
        self.assertNotIn('sample',self.engine.loaded)
        self.engine.enable('sample')
        self.assertIn('sample',self.engine.loaded)
        (Path(self.temp.name)/'sample.bundled').unlink()
        self.engine.install_bundled(SOURCE,False)
        self.assertIn('sample',self.engine.loaded)

    def test_import_does_not_execute(self):
        self.engine.install(SOURCE + "\nraise RuntimeError('executed')")
        self.assertFalse(self.engine.loaded)
        with self.assertRaises(RuntimeError): self.engine.enable('sample')
        self.assertFalse(self.engine.state['sample']['enabled'])
    def test_lifecycle_settings_restart(self):
        self.engine.install(SOURCE)
        self.engine.enable('sample')
        self.assertEqual(self.engine.dispatch('event', 'hi'), 'hi!')
        restored = Engine(self.temp.name)
        restored.restore()
        self.assertEqual(restored.setting('sample', 'loads'), 2)
        restored.disable('sample')
        self.engine.disable('sample')
        self.assertEqual(self.engine.dispatch('event', 'hi'), 'hi')
    def test_update_disables_and_delete_removes(self):
        self.engine.install(SOURCE)
        self.engine.enable('sample')
        self.engine.install(SOURCE.replace("'1.0'", "'2.0'"))
        self.assertNotIn('sample', self.engine.loaded)
        self.engine.remove('sample')
        self.assertFalse((Path(self.temp.name) / 'sample.py').exists())
    def test_interrupted_event_is_not_restored(self):
        self.engine.install(SOURCE)
        self.engine.enable('sample')
        self.engine._enter('sample')
        recovered = Engine(self.temp.name)
        recovered.restore()
        self.assertFalse(recovered.loaded)
        self.assertIn('sample', recovered.errors)

    def test_schema_and_default_version(self):
        source = SOURCE.replace("__version__ = '1.0'", "")
        source += "\n    def create_settings(self):\n        from ui.settings import Switch\n        return [Switch('option', 'Option', True)]\n"
        self.engine.install(source)
        self.engine.enable('sample')
        import json
        schema = json.loads(self.engine.settings_schema('sample'))
        self.assertTrue(schema[0]['value'])
        self.assertEqual(self.engine.metadata(source)['__version__'], '1.0')

    def test_unload_failure_does_not_prevent_removal(self):
        source = SOURCE + "\n    def on_plugin_unload(self):\n        raise RuntimeError('unload failure')\n"
        self.engine.install(source)
        self.engine.enable('sample')
        self.engine.remove('sample')
        self.assertNotIn('sample', self.engine.loaded)
        self.assertFalse((Path(self.temp.name) / 'sample.py').exists())

    def test_settings_from_concurrent_callbacks(self):
        from concurrent.futures import ThreadPoolExecutor
        with ThreadPoolExecutor(max_workers=4) as pool:
            list(pool.map(lambda i: self.engine.set_setting('sample', str(i), i), range(40)))
        restored = Engine(self.temp.name)
        self.assertEqual(len(restored.state['sample']['settings']), 40)

    def test_system_exit_during_load_is_cleaned_up(self):
        self.engine.install(SOURCE + "\nraise SystemExit('plugin exit')")
        with self.assertRaises(SystemExit): self.engine.enable('sample')
        self.assertNotIn('claud_plugin_sample', sys.modules)
        self.assertFalse(self.engine.state['sample']['enabled'])

    def test_display_name_does_not_break_dataclass_module_lookup(self):
        source = SOURCE.replace("class Sample(BasePlugin):", "from dataclasses import dataclass\n@dataclass\nclass Sample(BasePlugin):")
        source = source.replace("__name__ = 'Sample'", "__name__ = 'Human Readable Name'")
        self.engine.install(source)
        self.engine.enable('sample')
        self.assertEqual(self.engine.loaded['sample'].__class__.__module__, 'claud_plugin_sample')

    def test_constructor_can_read_settings(self):
        source = SOURCE.replace("class Sample(BasePlugin):", "class Sample(BasePlugin):\n    def __init__(self):\n        self.initial = self.get_setting('initial', 'ready')")
        self.engine.install(source)
        self.engine.enable('sample')
        self.assertEqual(self.engine.loaded['sample'].initial, 'ready')

    def test_native_hook_cleanup_on_failed_load(self):
        from unittest.mock import patch
        import types
        from java_hooks import Handle
        calls = []
        class Native:
            def unhook(self): calls.append('unhook')
        fake = types.ModuleType('java_hooks')
        fake.install = lambda *args: Handle(Native(), object())
        source = SOURCE.replace("self.set_setting('loads', self.get_setting('loads', 0) + 1)", "self.hook_method('target')\n        raise RuntimeError('load failure')")
        self.engine.install(source)
        with patch.dict(sys.modules, {'java_hooks': fake}):
            with self.assertRaises(RuntimeError): self.engine.enable('sample')
        self.assertEqual(calls, ['unhook'])
        self.assertFalse(self.engine.loaded)

    def test_selector_changes_callback_and_type(self):
        source = SOURCE + "\n    def create_settings(self):\n        from ui.settings import Selector\n        return [Selector('mode','Mode',0,['A','B'],on_change=lambda v:self.set_setting('observed',v))]\n"
        self.engine.install(source); self.engine.enable('sample')
        import json
        rows = json.loads(self.engine.settings_schema('sample'))
        self.engine.change_setting('sample', rows[0]['index'], 1)
        self.assertEqual(self.engine.setting('sample', 'observed'), 1)
        self.assertEqual(self.engine.setting('sample', 'mode'), 1)

    def test_nested_settings_rows_keep_stable_handles(self):
        source = SOURCE + "\n    def create_settings(self):\n        from ui.settings import Text,Input\n        return [Text('More',create_sub_fragment=lambda:[Input('name','Name')])]\n"
        self.engine.install(source); self.engine.enable('sample')
        import json
        root=json.loads(self.engine.settings_schema('sample'))
        child=json.loads(self.engine.setting_click('sample',root[0]['index']))
        self.engine.change_setting('sample',child[0]['index'],'Claud')
        self.assertEqual(self.engine.setting('sample','name'),'Claud')
        self.assertNotEqual(root[0]['index'], child[0]['index'])

    def test_exteragram_id_case_and_hyphen(self):
        source = SOURCE.replace("'sample'", "'Example-Mod'")
        self.engine.install(source)
        self.engine.enable('Example-Mod')
        self.assertIn('Example-Mod',self.engine.loaded)
        self.engine.remove('Example-Mod')

    def test_manual_safe_mode_disables_all(self):
        self.engine.install(SOURCE)
        self.engine.enable('sample')
        (Path(self.temp.name) / 'disable_all.flag').touch()
        recovered = Engine(self.temp.name)
        recovered.restore()
        self.assertFalse(recovered.loaded)
        self.assertFalse(recovered.state['sample']['enabled'])

    def test_corrupt_state_is_recoverable(self):
        (Path(self.temp.name) / 'state.json').write_text('[]')
        self.assertEqual(Engine(self.temp.name).state, {})

    def test_settings_editor_validates_object(self):
        self.engine.save_settings_json('sample', '{"color":"warm"}')
        self.assertEqual(self.engine.setting('sample', 'color'), 'warm')
        with self.assertRaises(ValueError): self.engine.save_settings_json('sample', '[]')

    def test_traversal_and_oversize(self):
        with self.assertRaises(ValueError): self.engine.install(SOURCE.replace("'sample'", "'../bad'"))
        with self.assertRaises(ValueError): self.engine.install(SOURCE + '#' * 4194304)
    def test_event_failure_disables_plugin(self):
        self.engine.install(SOURCE.replace("return payload + '!'", "raise ValueError('event failure')"))
        self.engine.enable('sample')
        self.assertEqual(self.engine.dispatch('event', 'hi'), 'hi')
        self.assertFalse(self.engine.state['sample']['enabled'])
        self.assertIn('sample', self.engine.errors)

if __name__ == '__main__': unittest.main()
