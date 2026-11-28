"""Actual DAO SQL parity and synthetic SQLite projection benchmark (not Android RAM)."""
from pathlib import Path
import gc, json, re, sqlite3, statistics, time, tracemalloc
ROOT=Path(__file__).resolve().parents[2]
def main():
    source=(ROOT/'app/src/main/java/com/alananasss/kittytune/data/local/AppDatabase.kt').read_text(encoding='utf-8')
    queries={name:sql for sql,name in re.findall(r'@Query\("([^"\n]+)"\)\s*fun (getAllTracks|getDownloadedTrackIds)\(',source)}
    old=queries['getAllTracks']; new=queries['getDownloadedTrackIds']
    db=sqlite3.connect(':memory:')
    db.execute('CREATE TABLE downloaded_tracks(id INTEGER PRIMARY KEY,title TEXT,artist TEXT,artworkUrl TEXT,duration INTEGER,localAudioPath TEXT,localArtworkPath TEXT,downloadedAt INTEGER,lufs REAL,truePeak REAL,source TEXT,permalinkUrl TEXT,ownerId INTEGER,secretToken TEXT)')
    def parity():
        full={row[0] for row in db.execute(old)}; projected={row[0] for row in db.execute(new)}
        assert full==projected,(full,projected)
        return projected
    payload='metadata-'*64
    rows=[(i-3,'title '+str(i),'artist',payload,1000,'' if i%4==0 else (' ' if i%9==0 else '/audio/'+str(i)),payload,i,None,None,'vk',payload,9,payload) for i in range(10000)]
    db.executemany('INSERT INTO downloaded_tracks VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)',rows)
    parity()
    db.execute("UPDATE downloaded_tracks SET localAudioPath='' WHERE id=1");parity()
    db.execute("UPDATE downloaded_tracks SET localAudioPath='/new' WHERE id=-3");parity()
    db.execute("UPDATE downloaded_tracks SET title='new title' WHERE id=2");parity()
    db.execute('DELETE FROM downloaded_tracks WHERE id=-2');parity()
    def timed(sql):
        started=time.perf_counter();result=list(db.execute(sql));elapsed=time.perf_counter()-started
        assert len(result)>0
        return elapsed*1000
    full=[]; projected=[]
    for _ in range(3):timed(old);timed(new)
    for i in range(9):
        if i%2:projected.append(timed(new));full.append(timed(old))
        else:full.append(timed(old));projected.append(timed(new))
    def retained(sql):
        gc.collect();tracemalloc.start();result=list(db.execute(sql));current,peak=tracemalloc.get_traced_memory();tracemalloc.stop()
        return {'rows':len(result),'retained_python_bytes':current,'peak_python_bytes':peak}
    result={'scope':'synthetic Python/SQLite component; not Android RAM or whole-app timings','row_count':10000,'full_median_ms':statistics.median(full),'ids_median_ms':statistics.median(projected),'full':retained(old),'ids':retained(new)}
    db.execute('DELETE FROM downloaded_tracks');assert parity()==set();db.close()
    print(json.dumps(result,indent=2))
if __name__=='__main__':main()
