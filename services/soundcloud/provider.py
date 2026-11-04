"""SoundCloud public catalog adapter. Credentials belong on the server, never Android."""
import base64, json, os, threading, time, urllib.parse, urllib.request

class SoundCloud:
    def __init__(self, client_id, client_secret, request=None):
        self.client_id,self.client_secret=client_id,client_secret
        self.request=request or self._request
        self._token=None; self._until=0; self._lock=threading.Lock()
    def _request(self,url,headers,data=None):
        req=urllib.request.Request(url,headers=headers,data=data)
        with urllib.request.urlopen(req,timeout=20) as response:
            payload=response.read(4*1024*1024+1)
            if len(payload)>4*1024*1024:raise ValueError('Provider response too large')
            return json.loads(payload)
    def token(self):
        with self._lock:
            if self._token and time.monotonic()<self._until:return self._token
            if not self.client_id or not self.client_secret:raise ValueError('SoundCloud application not configured')
            basic=base64.b64encode(f'{self.client_id}:{self.client_secret}'.encode()).decode()
            result=self.request('https://secure.soundcloud.com/oauth/token',{'Authorization':'Basic '+basic,'Content-Type':'application/x-www-form-urlencoded','Accept':'application/json'},b'grant_type=client_credentials')
            self._token=result['access_token'];self._until=time.monotonic()+max(0,int(result.get('expires_in',3600))-120)
            return self._token
    def get(self,path,query=None):
        if not path.startswith('/') or path.startswith('//'):raise ValueError('Invalid API path')
        url='https://api.soundcloud.com'+path
        if query:url+='?'+urllib.parse.urlencode(query)
        return self.request(url,{'Authorization':'OAuth '+self.token(),'Accept':'application/json'})
    @staticmethod
    def track(item):
        access=item.get('access','blocked')
        return dict(id=str(item.get('urn') or item['id']),title=item.get('title',''),artist=item.get('metadata_artist') or item.get('user',{}).get('username',''),artwork=item.get('artwork_url'),durationMs=item.get('duration',0),permalink=item.get('permalink_url'),access=access,source='SoundCloud',streamable=access=='playable' and bool(item.get('stream_url')))
    def search(self,query,limit=20):
        query=query.strip()
        if not query or len(query)>200:raise ValueError('Query must be 1–200 characters')
        result=self.get('/tracks',dict(q=query,access='playable',limit=max(1,min(50,int(limit))),linked_partitioning='true'))
        items=result.get('collection',[]) if isinstance(result,dict) else result
        return [self.track(item) for item in items]
    def streams(self,track_id):
        # IDs cannot inject a host, path or query into provider requests.
        import re
        if not re.fullmatch(r'(?:soundcloud:tracks:)?[0-9]+',str(track_id)):raise ValueError('Invalid track ID')
        result=self.get('/tracks/'+urllib.parse.quote(str(track_id),safe=''))
        if result.get('access')!='playable' or not result.get('stream_url'):raise ValueError('Track is unavailable for external playback')
        url=result['stream_url'];parsed=urllib.parse.urlparse(url)
        if parsed.scheme!='https' or parsed.hostname!='api.soundcloud.com' or parsed.username or parsed.password:raise ValueError('Unexpected stream endpoint')
        return self.get(parsed.path,dict(urllib.parse.parse_qsl(parsed.query)))

def from_environment():
    return SoundCloud(os.environ.get('SOUNDCLOUD_CLIENT_ID',''),os.environ.get('SOUNDCLOUD_CLIENT_SECRET',''))
