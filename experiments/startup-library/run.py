"""Run the actual artist cover index without an Android SDK."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--java-home', type=Path)
    parser.add_argument('--dependency-cache', type=Path)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    java_home = args.java_home or Path(os.environ['JAVA_HOME'])
    java = str(java_home / 'bin' / ('java.exe' if os.name == 'nt' else 'java'))
    jars = output / 'dependencies'
    jars.mkdir(exist_ok=True)

    def dep(group, artifact, version):
        if args.dependency_cache:
            path = args.dependency_cache / 'caches/modules-2/files-2.1' / group / artifact / version
            matches = sorted(path.glob(f'*/{artifact}-{version}.jar'))
            if matches:
                return matches[0]
        target = jars / f'{artifact}-{version}.jar'
        if not target.exists():
            url = f'https://repo.maven.apache.org/maven2/{group.replace(".", "/")}/{artifact}/{version}/{target.name}'
            with urllib.request.urlopen(url, timeout=90) as response:
                target.write_bytes(response.read())
        return target

    kotlin = '2.4.21'
    stdlib = dep('org.jetbrains.kotlin', 'kotlin-stdlib', kotlin)
    coroutines = dep('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.11.0')
    annotations = dep('org.jetbrains', 'annotations', '13.0')
    compiler = [dep('org.jetbrains.kotlin', x, kotlin) for x in (
        'kotlin-compiler-embeddable', 'kotlin-build-tools-api', 'kotlin-script-runtime', 'kotlin-daemon-embeddable')]
    compiler += [dep('org.jetbrains.kotlin', 'kotlin-reflect', '1.6.10'), stdlib, coroutines, annotations]
    runtime = [stdlib, coroutines, annotations, dep('junit', 'junit', '4.13.2'), dep('org.hamcrest', 'hamcrest-core', '1.3')]
    runtime += [dep('com.google.code.gson', 'gson', '2.11.0')]
    shim = output / 'SpotifyRepositoryShim.kt'
    shim.write_text('package com.alananasss.kittytune.data.spotify; object SpotifyRepository { fun extractId(value:String):String = value.substringAfterLast(":").substringAfterLast("/") }', encoding='utf-8')
    main=ROOT/'app/src/main/java/com/alananasss/kittytune'
    test=ROOT/'app/src/test/java/com/alananasss/kittytune'
    sources=[main/p for p in ['core/ProcessInitializer.kt','core/CoalescingTask.kt','data/StoredLikes.kt','ui/library/LibraryCollections.kt','domain/Models.kt','data/spotify/SpotifyModels.kt','data/network/LongIdAdapter.kt','data/network/UserBadgesAdapter.kt']]
    sources += [test/p for p in ['core/ProcessInitializerTest.kt','core/CoalescingTaskTest.kt','data/StoredLikesTest.kt','ui/library/LibraryCollectionsTest.kt']]
    sources += [shim,ROOT/'experiments/startup-library/Benchmark.kt']
    classes=output/'classes';classes.mkdir(exist_ok=True)
    classpath=os.pathsep.join(map(str,runtime))
    subprocess.run([java,'-Xmx2g','-cp',os.pathsep.join(map(str,compiler)),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','11','-classpath',classpath,'-d',str(classes),*map(str,sources)],check=True)
    result=subprocess.run([java,'-cp',str(classes)+os.pathsep+classpath,'org.junit.runner.JUnitCore','com.alananasss.kittytune.core.ProcessInitializerTest','com.alananasss.kittytune.core.CoalescingTaskTest','com.alananasss.kittytune.data.StoredLikesTest','com.alananasss.kittytune.ui.library.LibraryCollectionsTest'],text=True,capture_output=True,timeout=180)
    (output/'junit.log').write_text(result.stdout+result.stderr,encoding='utf-8');print(result.stdout+result.stderr,flush=True);result.check_returncode()
    for process in range(3):
        measured=subprocess.run([java,'-Xms128m','-Xmx512m','-cp',str(classes)+os.pathsep+classpath,'com.alananasss.kittytune.ui.library.BenchmarkKt'],text=True,capture_output=True,check=True,timeout=180)
        (output/f'benchmark-{process}.log').write_text(measured.stdout,encoding='utf-8');print(measured.stdout,flush=True)
    import sys
    measured=subprocess.run([sys.executable,str(ROOT/'experiments/startup-library/query_projection.py')],text=True,capture_output=True,check=True,timeout=180)
    (output/'query-projection.json').write_text(measured.stdout,encoding='utf-8');print(measured.stdout,flush=True)
    (output/'result.json').write_text(json.dumps({'revision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'scope':'local component verification; no device profiling','sources_sha256':{str(p.relative_to(ROOT)) if p.is_relative_to(ROOT) else p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}},indent=2),encoding='utf-8')

if __name__=='__main__':main()
