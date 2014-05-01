@echo off 

REM -XX:+PrintGCDetails
REM -Xloggc:jworm-gc.log

set XMX=-Xms512M -Xloggc:jworm-gc.log
set CP=-classpath bin\deuceTests.jar;lib\jwormbench.jar;lib\jvstm-2.0.jar

REM arg 0: 512 iterations
REM arg 1: 4 nr of threads
REM arg 2: 22 workload
REM arg 3: deuce synchronization technique
REM arg 4: 120 timeout

REM set ARGS=512 4 22 deuce
set ARGS=8192 4 22 none 240

set SYS_PROPS=%XMX% %CP%

java %SYS_PROPS% org.deuce.benchmark.jwormbench.RunJWormBench %ARGS%