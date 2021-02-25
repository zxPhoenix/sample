import m3u8
from locust import HttpUser, task, between

class Player(HttpUser):
    plDuration = 3
    wait_time = between(plDuration, plDuration)
    sessionId = 'null'
    masterPl = None
    mediaPl = None
    mediaUri = None
    chunkUri = None
    prevChunk = None

    @task
    def pl(self):
        if self.sessionId != 'null' and self.mediaUri != None: 
            resp = self.client.get("/media/4aa3191a-8955-4a86-82f5-01920616ac8b/{}/{}".format(self.sessionId, self.mediaUri), name="/pl.m3u8")

            if resp != None :
                self.chunkUri = self.getTSURI(resp)

            if self.sessionId != 'null' and self.chunkUri != None: 
                if(self.prevChunk != self.chunkUri) :
                    self.prevChunk = self.chunkUri
                    self.client.get(self.chunkUri, name="/chunk.ts")
            else :
                ''    

        else :
            '' 

    def on_start(self):
        response = self.client.get("/play/4aa3191a-8955-4a86-82f5-01920616ac8b.m3u8")
        self.sessionId = response.cookies.get('x-session-id', path='/')
        print(self.sessionId)
        self.client.cookies.set('x-session-id', self.sessionId)

        if self.sessionId != 'null' : 
            resp = self.client.get("/media/4aa3191a-8955-4a86-82f5-01920616ac8b/{}/master.m3u8".format(self.sessionId), name="/master.m3u8")

            if resp != None : 

                st = str(resp.content.decode("utf-8")).replace("3,", "3")
                print(st)
                self.masterPl = m3u8.loads(st)

                self.mediaUri = self.getPlURI(self.masterPl)
    
    def getPlURI(self, resp): 
        if resp != None and resp.playlists != None : 
            return max(self.masterPl.playlists, key=lambda item: item.stream_info.bandwidth).uri
    
        else :
            return None 

    def getTSURI(self, resp): 
        if resp != None : 
                s = resp.content.decode("utf-8").replace(",\n#", "\n#").replace(",\n\n#", "\n\n#")

                print(s)

                mediaPl = m3u8.loads(s)

                if mediaPl != None and mediaPl.segments != None :

                    duration = mediaPl.target_duration
                    if self.plDuration == None :
                        self.plDuration = duration
                    
                    self.mediaPl = mediaPl

                    return self.mediaPl.segments[-1].uri
                else :
                    return None
        else :
            return None 
        
                

