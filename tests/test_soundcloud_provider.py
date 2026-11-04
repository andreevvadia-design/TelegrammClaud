import importlib.util,pathlib,unittest
path=pathlib.Path(__file__).resolve().parents[1]/'services/soundcloud/provider.py'
spec=importlib.util.spec_from_file_location('soundcloud_provider',path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
class SoundCloudTests(unittest.TestCase):
 def test_search_token_reused_and_query_encoded(self):
  calls=[]
  def request(url,headers,data=None):
   calls.append((url,headers,data))
   if 'oauth/token' in url:return {'access_token':'test_token','expires_in':3600}
   return {'collection':[{'id':42,'title':'Track','user':{'username':'Artist'},'access':'playable','stream_url':'https://api.soundcloud.com/tracks/42/stream'}]}
  client=module.SoundCloud('id','secret',request)
  result=client.search('a & b');client.search('next')
  self.assertEqual(3,len(calls));self.assertIn('q=a+%26+b',calls[1][0]);self.assertEqual('OAuth test_token',calls[1][1]['Authorization']);self.assertTrue(result[0]['streamable']);self.assertNotIn('secret',str(result))
 def test_blocked_not_playable(self):
  self.assertFalse(module.SoundCloud.track({'id':1,'access':'blocked','stream_url':None})['streamable'])
 def test_unconfigured_no_network(self):
  with self.assertRaises(ValueError):module.SoundCloud('','').token()
 def test_invalid_stream_id(self):
  with self.assertRaises(ValueError):module.SoundCloud('id','secret').streams('https://example.com')
 def test_reject_arbitrary_stream_host(self):
  def request(url,headers,data=None):
   if 'oauth/token' in url:return {'access_token':'test','expires_in':3600}
   return {'access':'playable','stream_url':'https://evil.example/stream'}
  with self.assertRaises(ValueError):module.SoundCloud('id','secret',request).streams('1')
if __name__=='__main__':unittest.main()
