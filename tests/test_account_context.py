import sys, unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"TMessagesProj/src/main/python"))
from account_context import account_scope,get_hook_account
class AccountTests(unittest.TestCase):
 def test_nested_callbacks_restore_account(self):
  self.assertIsNone(get_hook_account())
  with account_scope(1):
   with account_scope(2): self.assertEqual(get_hook_account(),2)
   self.assertEqual(get_hook_account(),1)
  self.assertIsNone(get_hook_account())
 def test_exception_restores_scope(self):
  with self.assertRaises(RuntimeError):
   with account_scope(3): raise RuntimeError()
  self.assertIsNone(get_hook_account())
 def test_workers_do_not_share_scope(self):
  from concurrent.futures import ThreadPoolExecutor
  def worker(account):
   with account_scope(account): return get_hook_account()
  with account_scope(1):
   with ThreadPoolExecutor(max_workers=2) as pool: self.assertEqual(list(pool.map(worker,[2,3])),[2,3])
   self.assertEqual(get_hook_account(),1)
